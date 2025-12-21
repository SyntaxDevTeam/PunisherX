package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import java.util.function.Predicate
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

object BrigadierCommandUtils {
    fun suggestions(
        command: BrigadierCommand,
        argsProvider: (CommandContext<CommandSourceStack>) -> List<String>
    ): SuggestionProvider<CommandSourceStack> = SuggestionProvider { context, builder ->
        command.suggest(context.source, argsProvider(context)).forEach { builder.suggest(it) }
        builder.buildFuture()
    }

    fun greedyArgs(base: List<String>, value: String): List<String> {
        if (value.isBlank()) {
            return base
        }
        val parts = value.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
        return base + parts
    }

    fun requiresPermission(key: PermissionChecker.PermissionKey): Predicate<CommandSourceStack> {
        return Commands.restricted { source ->
            PermissionChecker.hasWithLegacy(source.sender, key)
        }
    }

    fun requiresPlayerPermission(key: PermissionChecker.PermissionKey): Predicate<CommandSourceStack> {
        return Commands.restricted { source ->
            source.sender is Player && PermissionChecker.hasWithLegacy(source.sender, key)
        }
    }

    fun requiresPlayer(): Predicate<CommandSourceStack> {
        return Commands.restricted { source -> source.sender is Player }
    }

    fun requiresConsole(): Predicate<CommandSourceStack> {
        return Commands.restricted { source -> source.sender is ConsoleCommandSender }
    }
}
