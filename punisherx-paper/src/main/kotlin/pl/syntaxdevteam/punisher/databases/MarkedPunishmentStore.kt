package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.syntaxdb.Database
import pl.syntaxdevteam.syntaxdb.DatabaseClient
import java.util.Locale

/** Mark identity is independent of the displayed reason, player name and active/history IDs. */
internal class MarkedPunishmentStore(private val database: Database, private val scope: String) {
    companion object {
        fun normalize(mark: String): String {
            require(Regex("[A-Za-z0-9][A-Za-z0-9_.:-]{0,63}").matches(mark)) { "Invalid mark" }
            return mark.lowercase(Locale.ROOT)
        }
    }

    private val scopeFilter = if (scope == "network") "1 = 1" else "(server = ? OR server = 'network')"
    private val scopeParams = if (scope == "network") emptyList() else listOf(scope)

    // All server instances sharing the database serialize issuance and reversal on this row.
    private fun DatabaseClient.lock() {
        check(execute("UPDATE punishment_mark_lock SET id = id WHERE id = 1") == 1) { "Mark lock is missing" }
    }

    fun isPaused(mark: String): Boolean = database.query(
        "SELECT mark FROM punishment_mark_pauses WHERE mark = ? AND $scopeFilter",
        listOf(normalize(mark)) + scopeParams
    ).isNotEmpty()

    fun record(name: String, uuid: String, reason: String, operator: String, type: String,
               start: Long, end: Long, mark: String, playerUuid: String,
               template: String? = null): Long = database.transaction {
        lock()
        val normalized = normalize(mark)
        check(query("SELECT mark FROM punishment_mark_pauses WHERE mark = ? AND $scopeFilter",
            listOf(normalized) + scopeParams).isEmpty()) { "Mark $normalized is paused" }
        val values = linkedMapOf<String, Any?>("name" to name, "uuid" to uuid, "reason" to reason,
            "operator" to operator, "punishmentType" to type, "start" to start, "endTime" to end, "server" to scope)
            .mapKeys { PunisherDatabaseSchema.column(database, it.key) }
        val activeId = if (type == "KICK") null else
            requireNotNull(insert("punishments", values).generatedKey as? Number).toLong()
        val historyId = requireNotNull(insert("punishmenthistory", values).generatedKey as? Number).toLong()
        insert("punishment_marks", mapOf("historyId" to historyId, "activeId" to activeId,
            "mark" to normalized, "playerUuid" to playerUuid)
            .mapKeys { PunisherDatabaseSchema.column(database, it.key) })
        if (template != null) execute(
            "INSERT INTO punishment_templates (historyId, templateName, playerUuid) VALUES (?, ?, ?)",
            listOf(historyId, template.lowercase(Locale.ROOT), playerUuid))
        activeId ?: historyId
    }

    /** A pause and its reversals commit together; history and the undo audit are retained. */
    fun undo(playerUuid: String?, mark: String?, operator: String, pause: Boolean = false): List<PunishmentData> = database.transaction {
        lock()
        val normalized = mark?.let(::normalize)
        if (pause) {
            require(normalized != null && playerUuid == null)
            execute("DELETE FROM punishment_mark_pauses WHERE mark = ? AND server = ?", listOf(normalized, scope))
            execute("INSERT INTO punishment_mark_pauses (mark, server, pausedBy, pausedAt) VALUES (?, ?, ?, ?)",
                listOf(normalized, scope, operator, System.currentTimeMillis()))
        }
        val filters = mutableListOf(scopeFilter,
            "NOT EXISTS (SELECT 1 FROM punishment_undos u WHERE u.historyId = h.id)")
        val params = scopeParams.toMutableList<Any>()
        if (playerUuid != null) { filters += "m.playerUuid = ?"; params += playerUuid }
        if (normalized != null) { filters += "m.mark = ?"; params += normalized }
        val selected = query("SELECT h.*, m.activeId FROM punishmenthistory h JOIN punishment_marks m ON m.historyId = h.id " +
            "WHERE ${filters.joinToString(" AND ")}", params)
        selected.map { row ->
            val punishment = PunishmentData(row.int("id") ?: 0, row.string("uuid").orEmpty(),
                row.string("punishmentType").orEmpty(), row.string("reason").orEmpty(),
                row.long("start") ?: 0, row.long("endTime") ?: -1, row.string("name").orEmpty(),
                row.string("operator").orEmpty(), row.string("server").orEmpty())
            execute("INSERT INTO punishment_undos (historyId, undoneBy, undoneAt) VALUES (?, ?, ?)",
                listOf(punishment.id, operator, System.currentTimeMillis()))
            row.long("activeId")?.let { activeId ->
                // Also verify event fields so an imported/reused ID cannot remove an unrelated penalty.
                execute("DELETE FROM punishments WHERE id = ? AND uuid = ? AND punishmentType = ? AND start = ? AND server = ?",
                    listOf(activeId, punishment.uuid, punishment.type, punishment.start, punishment.server))
            }
            if (punishment.type == "BAN" || punishment.type == "BANIP") execute(
                "DELETE FROM bridge_events WHERE action = ? AND target = ? AND processed = 0 AND NOT EXISTS " +
                    "(SELECT 1 FROM punishments WHERE uuid = ? AND punishmentType = ?)",
                listOf(punishment.type, punishment.uuid, punishment.uuid, punishment.type))
            punishment
        }
    }

    fun resume(mark: String) = database.transaction {
        lock()
        execute("DELETE FROM punishment_mark_pauses WHERE mark = ? AND server = ?", listOf(normalize(mark), scope))
    }
}
