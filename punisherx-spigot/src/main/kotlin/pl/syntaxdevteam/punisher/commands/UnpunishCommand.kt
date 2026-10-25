package pl.syntaxdevteam.punisher.commands

import pl.syntaxdevteam.punisher.compatibility.*

import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

/** Reverses a history entry, including its contribution to template escalation. */
class UnpunishCommand(private val plugin: PunisherX) : BasicCommand {
    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNPUNISH)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unpunish", "usage"))
            return
        }
        val templateName = args.drop(1).joinToString(" ").removeSurrounding("\"")
        val template = plugin.punishTemplateManager.getTemplate(templateName)
        if (template == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent(
                "punish", "template_not_found", mapOf("template" to templateName)))
            return
        }
        val uuid = plugin.resolvePlayerUuid(args[0])
        val placeholders = mapOf("player" to args[0], "template" to template.name, "operator" to stack.sender.name)
        val undone = plugin.databaseHandler.undoPunishment(uuid.toString(), args[0], template.reason, template.name, stack.sender.name)
        if (undone == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unpunish", "db_error"))
            return
        }
        if (undone.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unpunish", "not_found", placeholders))
            return
        }
        // Do not release a jail imposed by a different, still active punishment.
        if (undone.any { it.type == "JAIL" } &&
            plugin.databaseHandler.getPunishments(uuid.toString()).none { it.type == "JAIL" }) {
            plugin.cache.removePunishment(uuid, notify = false, removeDatabase = false)
        }
        plugin.commandLoggerPlugin.logCommand(stack.sender.name, "UNPUNISH", args[0], "${template.name}: history #${undone.first().id}")
        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unpunish", "success", placeholders + ("id" to undone.first().id.toString())))
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNPUNISH)) return emptyList()
        val input = args.lastOrNull().orEmpty()
        return if (args.size <= 1) plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(input, true) }
        else plugin.punishTemplateManager.getTemplateNames()
            .filter { it.startsWith(args.drop(1).joinToString(" ").trimStart('"'), true) }
            .map { if (' ' in it) "\"$it\"" else it }
    }
}
