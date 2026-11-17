package pl.syntaxdevteam.punisher.commands

import pl.syntaxdevteam.punisher.compatibility.*

import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.MarkedPunishmentStore

/** Invocation-local flags, removed before interpreting targets, times, reasons or levels. */
internal class PunishmentCommandOptions(
    val arguments: Array<String>,
    val force: Boolean,
    val silent: Boolean,
    val mark: String? = null,
    private val invalidMark: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): PunishmentCommandOptions {
            val marks = args.filter { it == "--mark" || it.startsWith("--mark=") }
            val raw = marks.firstOrNull()?.let { if (it == "--mark") "default" else it.substringAfter('=') }
            val normalized = raw?.let { runCatching { MarkedPunishmentStore.normalize(it) }.getOrNull() }
            return PunishmentCommandOptions(
                args.filterNot { it == "--force" || it == "--silent" || it in marks }.toTypedArray(),
                "--force" in args, "--silent" in args, normalized,
                marks.size > 1 || (raw != null && normalized == null))
        }
    }

    fun allowMark(plugin: PunisherX, stack: CommandSourceStack): Boolean {
        val key = if (invalidMark) "invalid_mark" else if (mark == null) return true else try {
            if (plugin.databaseHandler.isMarkPaused(mark)) "paused" else return true
        } catch (error: Exception) {
            plugin.logger.err("Failed to check mark $mark. ${error.message}")
            "db_error"
        }
        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unmark", key,
            mapOf("mark" to mark.orEmpty())))
        return false
    }

    fun persist(plugin: PunisherX, name: String, uuid: String, reason: String, operator: String,
                type: String, start: Long, end: Long, playerUuid: String = uuid, template: String? = null): Long? =
        if (mark == null) plugin.databaseHandler.addPunishment(name, uuid, reason, operator, type, start, end)
        else plugin.databaseHandler.addMarkedPunishment(name, uuid, reason, operator, type, start, end, mark, playerUuid, template)
}
