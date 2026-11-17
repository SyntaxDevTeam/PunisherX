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
    fun addTrailingFlags(node: com.mojang.brigadier.tree.CommandNode<CommandSourceStack>, command: io.papermc.paper.command.brigadier.BasicCommand) {
        val children = node.children.toList()
        children.forEach { addTrailingFlags(it, command) }
        val selfGreedy = node is com.mojang.brigadier.tree.ArgumentCommandNode<*, *> &&
            (node.type is pl.syntaxdevteam.punisher.commands.arguments.ReasonArgumentType ||
            (node.type is com.mojang.brigadier.arguments.StringArgumentType &&
            (node.type as com.mojang.brigadier.arguments.StringArgumentType).type == com.mojang.brigadier.arguments.StringArgumentType.StringType.GREEDY_PHRASE))
        if (node.command != null && children.isEmpty() && !selfGreedy && node.name.isNotEmpty()) {
            node.addChild(Commands.argument("flags", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                .executes(executor(command)).build())
        }
    }

    fun executor(command: io.papermc.paper.command.brigadier.BasicCommand): com.mojang.brigadier.Command<CommandSourceStack> =
        com.mojang.brigadier.Command { context ->
            var invocations = listOf(emptyList<String>())
            for (node in context.nodes.drop(1)) {
                val value = runCatching { context.getArgument(node.node.name, Any::class.java) }.getOrNull()
                val choices: List<List<String>> = when (value) {
                    is PlayerProfileListResolver -> value.resolve(context.source).mapNotNull { profile ->
                        (profile.name ?: profile.id?.toString())?.let { listOf(it) }
                    }
                    is PlayerSelectorArgumentResolver -> value.resolve(context.source).map { listOf(it.name) }
                    is String -> listOf(value.trim().split(Regex("\\s+")))
                    else -> listOf(node.range.get(context.input).trim().split(Regex("\\s+")))
                }
                invocations = invocations.flatMap { prefix -> choices.map { prefix + it } }
            }
            invocations.forEach { command.execute(context.source, it.toTypedArray()) }
            1
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
        return Predicate { source ->
            PermissionChecker.hasWithLegacy(source.sender, key)
        }
    }

    fun requiresPlayerPermission(key: PermissionChecker.PermissionKey): Predicate<CommandSourceStack> {
        return Predicate { source ->
            source.sender is Player && PermissionChecker.hasWithLegacy(source.sender, key)
        }
    }

    fun requiresPlayer(): Predicate<CommandSourceStack> {
        return Predicate { source -> source.sender is Player }
    }

    fun requiresConsole(): Predicate<CommandSourceStack> {
        return Predicate { source -> source.sender is ConsoleCommandSender }
    }
}
