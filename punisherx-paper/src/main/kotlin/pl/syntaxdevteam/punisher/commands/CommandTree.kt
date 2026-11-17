package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.command.CommandSender
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.concurrent.CompletableFuture

/**
 * Builds a Brigadier tree from the forms a command accepts, e.g. `add <player> <text>`.
 * The typed words are passed on to the command's own execute, as with the old string commands.
 */
internal class CommandTree(private val plugin: PunisherX) {
    sealed interface Part
    class Literal(val name: String, val requires: (CommandSender) -> Boolean = { true }) : Part
    class Argument(
        val key: String,
        val kind: Kind = Kind.WORD,
        val flags: List<String> = emptyList(),
        val values: () -> List<String> = { emptyList() }
    ) : Part
    enum class Kind { WORD, GREEDY, DURATION }

    private class Node(val part: Part?) {
        val children = linkedMapOf<String, Node>()
    }

    private val names = mutableMapOf<String, String>()
    private val punishmentFlags = listOf("--force", "--silent", "--mark", "--mark=")

    fun create(
        name: String,
        requires: (CommandSender) -> Boolean,
        command: BasicCommand,
        vararg forms: List<Part>
    ): LiteralCommandNode<CommandSourceStack> {
        val root = Node(null)
        forms.forEach { form ->
            var node = root
            form.forEach { part -> node = node.children.getOrPut(id(part)) { Node(part) } }
        }
        val literal = Commands.literal(name)
            .requires { src -> requires(src.sender) }
            .executes { ctx -> execute(command, ctx, emptyList()) }
        root.children.values.forEach { literal.then(build(it, emptyList(), command)) }
        return literal.build()
    }

    /** /ban, /mute and the like: <player> [time] [reason and flags]. */
    fun punishment(
        name: String,
        messageKey: String,
        permission: PermissionChecker.PermissionKey,
        command: BasicCommand,
        timed: Boolean = true,
        targetArgument: String = "player"
    ): LiteralCommandNode<CommandSourceStack> {
        val target = Argument(targetArgument, values = ::players)
        val time = Argument("time", Kind.DURATION, values = ::times)
        val reason = Argument("reason", Kind.GREEDY, punishmentFlags) {
            plugin.messageHandler.getMessageStringList(messageKey, "reasons")
        }
        val forms = if (timed) {
            arrayOf(listOf(target), listOf(target, time), listOf(target, time, reason), listOf(target, reason))
        } else {
            arrayOf(listOf(target), listOf(target, reason))
        }
        return create(name, { PermissionChecker.hasWithLegacy(it, permission) }, command, *forms)
    }

    fun players(): List<String> = plugin.server.onlinePlayers.map { it.name }.sorted()

    private fun times(): List<String> =
        plugin.config.getStringList("command-completion.times").ifEmpty { listOf("1h", "1d", "7d", "30d") }

    private fun id(part: Part) = when (part) {
        is Literal -> "literal:${part.name}"
        is Argument -> "argument:${part.key}"
    }

    private fun build(node: Node, parent: List<Part>, command: BasicCommand): ArgumentBuilder<CommandSourceStack, *> {
        val path = parent + node.part!!
        val builder = when (val part = node.part) {
            is Literal -> Commands.literal(part.name).requires { src -> part.requires(src.sender) }
            is Argument -> Commands.argument(argumentName(part.key), type(part.kind))
                .suggests { _, suggestions -> suggest(part, suggestions) }
        }
        builder.executes { ctx -> execute(command, ctx, path) }
        node.children.values.forEach { builder.then(build(it, path, command)) }
        return builder
    }

    private fun type(kind: Kind): ArgumentType<String> = when (kind) {
        Kind.WORD -> StringArgumentType.word()
        Kind.GREEDY -> StringArgumentType.greedyString()
        Kind.DURATION -> DurationArgumentType(plugin)
    }

    private fun suggest(part: Argument, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val remaining = builder.remaining
        if (part.kind != Kind.GREEDY) {
            part.values().filter { it.startsWith(remaining, ignoreCase = true) }.forEach { builder.suggest(it) }
            return builder.buildFuture()
        }
        val last = remaining.substringAfterLast(' ')
        val values = part.values()
        if (part.flags.isNotEmpty() && (last.startsWith("-") || values.isEmpty())) {
            val used = remaining.split(Regex("\\s+")).dropLast(1)
            val offset = builder.createOffset(builder.start + remaining.length - last.length)
            part.flags.filter { it.startsWith(last) && it !in used }.forEach { offset.suggest(it) }
            return offset.buildFuture()
        }
        // A value here may have spaces, so it replaces everything typed for this argument.
        values.filter { it.startsWith(remaining, ignoreCase = true) }.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    private fun execute(command: BasicCommand, ctx: CommandContext<CommandSourceStack>, path: List<Part>): Int {
        val args = path.flatMap { part ->
            when (part) {
                is Literal -> listOf(part.name)
                is Argument -> {
                    val value = ctx.getArgument(argumentName(part.key), String::class.java)
                    if (part.kind == Kind.GREEDY) value.trim().split(Regex("\\s+")) else listOf(value)
                }
            }
        }
        command.execute(ctx.source, args.toTypedArray())
        return Command.SINGLE_SUCCESS
    }

    // Read once, so a config reload cannot rename an argument the registered tree still uses.
    private fun argumentName(key: String): String = names.getOrPut(key) {
        plugin.config.getString("command-completion.arguments.$key")?.takeIf { it.isNotBlank() } ?: key
    }

    /** Accepts only durations like 30m or 7d, so anything else is read as the start of the reason. */
    private class DurationArgumentType(private val plugin: PunisherX) : CustomArgumentType.Converted<String, String> {
        private val invalid = SimpleCommandExceptionType(LiteralMessage("Not a duration"))

        override fun convert(nativeType: String): String {
            try {
                plugin.timeHandler.parseTime(nativeType)
            } catch (_: NumberFormatException) {
                throw invalid.create()
            }
            return nativeType
        }

        override fun getNativeType(): StringArgumentType = StringArgumentType.word()
    }
}
