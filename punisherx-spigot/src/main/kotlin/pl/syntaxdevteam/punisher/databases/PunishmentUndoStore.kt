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
        reverse(rows, operator)
        rows
    }

    /** Undo the exact active event selected in a GUI, never the latest event in its template. */
    fun undoActive(punishment: PunishmentData, operator: String): Boolean = database.transaction {
        val active = rows("SELECT * FROM punishments WHERE id = ? AND $scopeFilter",
            listOf(punishment.id) + scopeParams).singleOrNull() ?: return@transaction false
        // Reject stale GUI entries, including an ID reused for a different event.
        if (active != punishment.copy(undone = false)) return@transaction false
        val history = rows("SELECT * FROM punishmenthistory WHERE uuid = ? AND punishmentType = ? " +
            "AND start = ? AND server = ? AND operator = ? AND reason = ? AND endTime = ?",
            eventParams(active))
        check(history.size <= 1) { "Ambiguous history for active punishment #${active.id}" }
        val selected = history.singleOrNull()
        if (selected == null) {
            // Older/imported active-only entries have no contribution to the ladder to reverse.
            execute("DELETE FROM punishments WHERE id = ?", listOf(active.id))
            cancelBridge(active)
        } else {
            val related = if (selected.type == "BANIP") {
                val template = query("SELECT templateName, playerUuid FROM punishment_templates WHERE historyId = ?",
                    listOf(selected.id)).singleOrNull()
                val templateFilter = if (template == null)
                    "NOT EXISTS (SELECT 1 FROM punishment_templates t WHERE t.historyId = punishmenthistory.id)"
                else "EXISTS (SELECT 1 FROM punishment_templates t WHERE t.historyId = punishmenthistory.id " +
                    "AND t.templateName = ? AND t.playerUuid = ?)"
                val params = listOf(selected.name, selected.reason, selected.operator, selected.start, selected.server) +
                    if (template == null) emptyList() else listOf(template.string("templateName").orEmpty(), template.string("playerUuid").orEmpty())
                rows("SELECT * FROM punishmenthistory WHERE punishmentType = 'BANIP' AND name = ? " +
                    "AND reason = ? AND operator = ? AND start = ? AND server = ? AND $templateFilter", params)
            } else listOf(selected)
            reverse(related, operator)
        }
        true
    }

    private fun eventParams(row: PunishmentData): List<Any> =
        listOf(row.uuid, row.type, row.start, row.server, row.operator, row.reason, row.end)

    private fun DatabaseClient.reverse(rows: List<PunishmentData>, operator: String) {
        val now = System.currentTimeMillis()
        for (row in rows) {
            if (query("SELECT historyId FROM punishment_undos WHERE historyId = ?", listOf(row.id)).isEmpty()) {
                // The primary key prevents concurrent reversals from committing twice.
                execute("INSERT INTO punishment_undos (historyId, undoneBy, undoneAt) VALUES (?, ?, ?)",
                    listOf(row.id, operator, now))
            }
            // Active and history IDs are independent; match the full event, not those IDs.
            execute("DELETE FROM punishments WHERE uuid = ? AND punishmentType = ? AND start = ? " +
                "AND server = ? AND operator = ? AND reason = ? AND endTime = ?", eventParams(row))
            cancelBridge(row)
        }
    }

    private fun DatabaseClient.cancelBridge(row: PunishmentData) {
        if (row.type == "BAN" || row.type == "BANIP") {
            execute("DELETE FROM bridge_events WHERE action = ? AND target = ? AND reason = ? " +
                "AND endTime = ? AND processed = 0 AND NOT EXISTS " +
                "(SELECT 1 FROM punishments WHERE uuid = ? AND punishmentType = ?)",
                listOf(row.type, row.uuid, row.reason, row.end, row.uuid, row.type))
        }
    }


    private fun DatabaseClient.rows(sql: String, params: List<Any>): List<PunishmentData> =
        query(sql, params).map { it.punishment() }

    private fun Row.punishment() = PunishmentData(
        int("id") ?: 0, string("uuid").orEmpty(), string("punishmentType").orEmpty(), string("reason").orEmpty(),
        long("start") ?: 0L, long("endTime") ?: 0L, string("name").orEmpty(), string("operator").orEmpty(), string("server").orEmpty()
    )
}
