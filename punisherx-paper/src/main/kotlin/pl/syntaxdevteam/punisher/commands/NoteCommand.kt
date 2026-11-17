package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.CommandSender
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Staff-only observations. Never a punishment or an anti-cheat verdict. */
class NoteCommand(private val plugin: PunisherX) : BasicCommand {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    private val pageSize = 10

    private fun canManage(sender: CommandSender) =
        PermissionChecker.hasWithManage(sender, PermissionChecker.PermissionKey.MANAGE_NOTES)

    private fun canRead(sender: CommandSender) =
        canManage(sender) || PermissionChecker.hasWithSee(sender, PermissionChecker.PermissionKey.SEE_NOTES)

    private fun message(sender: CommandSender, key: String, values: Map<String, String> = emptyMap()) =
        sender.sendMessage(plugin.messageHandler.stringMessageToComponent("staff-notes", key, values))

    private fun denied(sender: CommandSender) =
        sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))

    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        val sender = stack.sender
        val action = args.firstOrNull()?.lowercase(Locale.ROOT)
        if (action == "add" || action == "remove") {
            if (!canManage(sender)) { denied(sender); return }
        } else if (!canRead(sender)) { denied(sender); return }
        if (action !in setOf("add", "list", "remove")) { message(sender, "usage"); return }
        val name = args.getOrNull(1)
        if (name == null) { message(sender, "usage"); return }
        val uuid = resolveTarget(name)
        if (uuid == null) { message(sender, "usage"); return }
        if (!plugin.databaseHandler.isReady()) { message(sender, "db-error"); return }
        try {
            when (action) {
                "add" -> {
                    if (args.size < 3) { message(sender, "usage"); return }
                    val content = args.drop(2).joinToString(" ").trim()
                    if (content.length !in 3..500 || content.any { Character.isISOControl(it) }) {
                        message(sender, "invalid-content"); return
                    }
                    val id = plugin.databaseHandler.addStaffNote(uuid, sender.name, content)
                    if (id == null) { message(sender, "db-error"); return }
                    plugin.commandLoggerPlugin.logCommand(sender.name, "NOTE_ADD", name, "#" + id)
                    message(sender, "added", mapOf("id" to id.toString(), "player" to name))
                }
                "list" -> {
                    if (args.size > 3) { message(sender, "usage"); return }
                    val page = if (args.size == 2) 1 else args.getOrNull(2)?.toIntOrNull()
                    if (page == null || page !in 1..(Int.MAX_VALUE / pageSize)) {
                        message(sender, "invalid-page"); return
                    }
                    val notes = plugin.databaseHandler.getStaffNotes(uuid, pageSize + 1, (page - 1) * pageSize)
                    message(sender, "list-title", mapOf("player" to name, "page" to page.toString()))
                    if (notes.isEmpty()) { message(sender, "empty"); return }
                    notes.take(pageSize).forEach { note ->
                        // User text must remain literal: no MiniMessage parsing or run_command injection.
                        val text = "#" + note.id + " | " + formatter.format(Instant.ofEpochMilli(note.createdAt)) +
                            " | " + note.author + " [" + note.server + "]: " + note.content
                        sender.sendMessage(Component.text(text))
                    }
                    if (page > 1) {
                        val previous = "/note list " + name + " " + (page - 1)
                        sender.sendMessage(Component.text("← " + previous).clickEvent(ClickEvent.runCommand(previous)))
                    }
                    if (notes.size > pageSize) {
                        val next = "/note list " + name + " " + (page + 1)
                        sender.sendMessage(Component.text(next + " →").clickEvent(ClickEvent.runCommand(next)))
                    }
                }
                "remove" -> {
                    if (args.size != 3) { message(sender, "usage"); return }
                    val id = args[2].toLongOrNull()
                    if (id == null || id <= 0) { message(sender, "usage"); return }
                    if (!plugin.databaseHandler.removeStaffNote(uuid, id)) { message(sender, "not-found"); return }
                    plugin.commandLoggerPlugin.logCommand(sender.name, "NOTE_REMOVE", name, "#" + id)
                    message(sender, "removed", mapOf("id" to id.toString(), "player" to name))
                }
            }
        } catch (error: Exception) {
            plugin.logger.err("Staff notes command failed: " + error.message)
            message(sender, "db-error")
        }
    }

    private fun resolveTarget(identifier: String): UUID? {
        val parsed = runCatching { UUID.fromString(identifier) }.getOrNull()
        if (parsed != null && parsed.toString().equals(identifier, ignoreCase = true)) return parsed
        if (!Regex("[A-Za-z0-9_]{3,16}").matches(identifier)) return null
        return plugin.resolvePlayerUuid(identifier)
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> {
        if (!canRead(stack.sender)) return emptyList()
        val input = args.lastOrNull().orEmpty()
        val choices = when (args.size) {
            0, 1 -> buildList {
                add("list")
                if (canManage(stack.sender)) { add("add"); add("remove") }
            }
            2 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
        return choices.filter { it.startsWith(input, ignoreCase = true) }
    }
}
