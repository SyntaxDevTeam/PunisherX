package pl.syntaxdevteam.punisher.commands

import com.destroystokyo.paper.profile.PlayerProfile
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
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

    fun suggestions(valuesProvider: (CommandContext<CommandSourceStack>) -> Iterable<String>):
        SuggestionProvider<CommandSourceStack> = SuggestionProvider { context, builder ->
            valuesProvider(context).forEach { builder.suggest(it) }
            builder.buildFuture()
        }

    fun suggestions(valuesProvider: (CommandContext<CommandSourceStack>, String) -> Iterable<String>):
        SuggestionProvider<CommandSourceStack> = SuggestionProvider { context, builder ->
            val remaining = builder.remainingLowerCase
            valuesProvider(context, remaining).forEach { builder.suggest(it) }
            builder.buildFuture()
        }

    fun greedyArgs(base: List<String>, value: String): List<String> {
        if (value.isBlank()) {
            return base
        }
        val parts = value.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
        return base + parts
    }

    fun resolvePlayers(context: CommandContext<CommandSourceStack>, argName: String): List<Player> {
        val resolver = context.getArgument(argName, PlayerSelectorArgumentResolver::class.java)
        return resolver.resolve(context.source)
    }

    fun resolvePlayerNames(context: CommandContext<CommandSourceStack>, argName: String): List<String> {
        return resolvePlayers(context, argName).map { it.name }
    }

    fun resolvePlayerProfiles(context: CommandContext<CommandSourceStack>, argName: String): Collection<PlayerProfile> {
        val resolver = context.getArgument(argName, PlayerProfileListResolver::class.java)
        return resolver.resolve(context.source)
    }

    fun resolvePlayerProfileNames(context: CommandContext<CommandSourceStack>, argName: String): List<String> {
        return resolvePlayerProfiles(context, argName)
            .mapNotNull { profile -> profile.name ?: profile.id?.toString() }
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
