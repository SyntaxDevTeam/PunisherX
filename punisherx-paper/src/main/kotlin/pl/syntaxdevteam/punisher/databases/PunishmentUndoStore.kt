package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.syntaxdb.Database
import pl.syntaxdevteam.syntaxdb.DatabaseClient
import pl.syntaxdevteam.syntaxdb.row.Row
import java.util.Locale

/** Keeps the original history and records each reversal once, in the same transaction as removal. */
internal class PunishmentUndoStore(private val database: Database, private val scope: String) {
    private val scopeFilter = if (scope == "network") "1 = 1" else "(server = ? OR server = 'network')"
    private val scopeParams: List<Any> = if (scope == "network") emptyList() else listOf(scope)

    private val eligible = "(" +
        "EXISTS (SELECT 1 FROM punishment_templates t WHERE t.historyId = punishmenthistory.id " +
        "AND t.playerUuid = ? AND t.templateName = ?) OR (" +
        "NOT EXISTS (SELECT 1 FROM punishment_templates t WHERE t.historyId = punishmenthistory.id) " +
        "AND (uuid = ? OR (punishmentType = 'BANIP' AND LOWER(name) = ?)) AND reason = ?)) " +
        "AND $scopeFilter AND NOT EXISTS (SELECT 1 FROM punishment_undos u WHERE u.historyId = punishmenthistory.id)"

    private fun eligibleParams(uuid: String, name: String, reason: String, template: String): List<Any> =
        listOf(uuid, template.lowercase(Locale.ROOT), uuid, name.lowercase(Locale.ROOT), reason) + scopeParams

    /** Writes both the history event and its template identity, or neither. */
    fun record(name: String, uuid: String, reason: String, operator: String, type: String,
               start: Long, end: Long, server: String, template: String, playerUuid: String) = database.transaction {
        val values = linkedMapOf<String, Any?>("name" to name, "uuid" to uuid, "reason" to reason,
            "operator" to operator, "punishmentType" to type, "start" to start, "endTime" to end, "server" to server)
            .mapKeys { PunisherDatabaseSchema.column(database, it.key) }
        val id = requireNotNull(insert("punishmenthistory", values).generatedKey as? Number) { "History insert returned no ID" }.toInt()
        execute("INSERT INTO punishment_templates (historyId, templateName, playerUuid) VALUES (?, ?, ?)",
            listOf(id, template.lowercase(Locale.ROOT), playerUuid))
    }

    fun count(uuid: String, name: String, reason: String, template: String): Int = database.transaction {
        rows("SELECT * FROM punishmenthistory WHERE $eligible", eligibleParams(uuid, name, reason, template))
            .distinctBy { row ->
                // One /punish BANIP can create multiple rows, one per known IP.
                if (row.type == "BANIP") listOf(row.name.lowercase(Locale.ROOT), row.type, row.start, row.operator, row.server)
                else listOf(row.id)
            }.size
    }

    /** Empty means missing, already undone, wrong player or inaccessible server. Errors propagate. */
    fun undo(uuid: String, name: String, reason: String, template: String, operator: String): List<PunishmentData> = database.transaction {
        val selected = rows(
            "SELECT * FROM punishmenthistory WHERE $eligible ORDER BY start DESC, id DESC",
            eligibleParams(uuid, name, reason, template)
        ).firstOrNull()
        if (selected == null) {
            return@transaction emptyList()
        }
        val rows = if (selected.type == "BANIP") rows(
            "SELECT * FROM punishmenthistory WHERE $eligible AND punishmentType = 'BANIP' AND name = ? AND reason = ? " +
                "AND operator = ? AND start = ? AND server = ?",
            eligibleParams(uuid, name, reason, template) + listOf(selected.name, selected.reason, selected.operator, selected.start, selected.server)
        ) else listOf(selected)
        val now = System.currentTimeMillis()
        for (row in rows) {
            // The primary key also prevents concurrent or repeated reversals of the same entry.
            execute("INSERT INTO punishment_undos (historyId, undoneBy, undoneAt) VALUES (?, ?, ?)",
                listOf(row.id, operator, now))
            // Active and history IDs are independent; match the original event, never the IDs.
            execute("DELETE FROM punishments WHERE uuid = ? AND punishmentType = ? AND start = ? AND server = ? AND operator = ?",
                listOf(row.uuid, row.type, row.start, row.server, row.operator))
            if (row.type == "BAN" || row.type == "BANIP") {
                // A queued disconnect must not fire later for a ban that no longer exists.
                execute("DELETE FROM bridge_events WHERE action = ? AND target = ? AND reason = ? " +
                    "AND endTime = ? AND processed = 0 AND NOT EXISTS " +
                    "(SELECT 1 FROM punishments WHERE uuid = ? AND punishmentType = ?)",
                    listOf(row.type, row.uuid, row.reason, row.end, row.uuid, row.type))
            }
        }
        rows
    }


    private fun DatabaseClient.rows(sql: String, params: List<Any>): List<PunishmentData> =
        query(sql, params).map { it.punishment() }

    private fun Row.punishment() = PunishmentData(
        int("id") ?: 0, string("uuid").orEmpty(), string("punishmentType").orEmpty(), string("reason").orEmpty(),
        long("start") ?: 0L, long("endTime") ?: 0L, string("name").orEmpty(), string("operator").orEmpty(), string("server").orEmpty()
    )
}
