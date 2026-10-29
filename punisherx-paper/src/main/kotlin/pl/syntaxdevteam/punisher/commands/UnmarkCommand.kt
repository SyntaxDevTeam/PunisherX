package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.MarkedPunishmentStore
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

class UnmarkCommand(private val plugin: PunisherX) : BasicCommand {
    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMARK)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        fun reply(key: String, values: Map<String, String> = emptyMap()) =
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unmark", key, values))
        val options = PunishmentCommandOptions.parse(args)
        val arguments = options.arguments
        if (arguments.size !in 1..2 || args.any { it == "--mark" || it.startsWith("--mark=") } || arguments.any { it.startsWith("--") }) {
            reply("usage")
            return
        }
        val operation = arguments[0].lowercase(java.util.Locale.ROOT)
        val control = operation == "pause" || operation == "resume"
        if (control && arguments.size != 2) { reply("usage"); return }
        val mark = arguments.getOrNull(1)?.let { value ->
            runCatching { MarkedPunishmentStore.normalize(value) }.getOrElse { reply("invalid_mark"); return }
        }
        if (operation == "resume") {
            val success = plugin.databaseHandler.resumeMark(requireNotNull(mark))
            if (success) plugin.commandLoggerPlugin.logCommand(stack.sender.name, "UNMARK_RESUME", "all", mark)
            reply(if (success) "resumed" else "db_error", mapOf("mark" to mark))
            return
        }
        val playerUuid = if (control || operation == "all") null else plugin.resolvePlayerUuid(arguments[0]).toString()
        val undone = plugin.databaseHandler.undoMarkedPunishments(playerUuid, mark, stack.sender.name, pause = control)
        if (undone == null) { reply("db_error"); return }
        for (target in undone.filter { it.type == "JAIL" }.map { it.uuid }.distinct()) {
            if (plugin.databaseHandler.getPunishments(target).none { it.type == "JAIL" }) {
                plugin.cache.removePunishment(UUID.fromString(target), notify = false, removeDatabase = false)
            }
        }
        plugin.commandLoggerPlugin.logCommand(stack.sender.name, if (control) "UNMARK_PAUSE" else "UNMARK",
            arguments[0], "${mark ?: "*"}: ${undone.size}")
        reply(if (control) "pause_success" else if (options.silent) "silent_success" else "success",
            mapOf("player" to arguments[0], "mark" to (mark ?: "*"), "count" to undone.size.toString()))
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMARK)) return emptyList()
        val input = args.lastOrNull().orEmpty()
        return if (args.size <= 1) (listOf("all", "pause", "resume") + plugin.server.onlinePlayers.map { it.name })
            .filter { it.startsWith(input, true) }
        else emptyList()
    }
}
