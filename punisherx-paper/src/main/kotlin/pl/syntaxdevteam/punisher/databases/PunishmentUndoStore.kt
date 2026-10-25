package pl.syntaxdevteam.punisher.databases

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.util.Locale

/** Keeps the original history and records each reversal once, in the same transaction as removal. */
internal class PunishmentUndoStore(private val connection: () -> Connection, private val scope: String) {
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
               start: Long, end: Long, server: String, template: String, playerUuid: String) = connection().use { conn ->
        conn.autoCommit = false
        try {
            val id = conn.prepareStatement(
                "INSERT INTO punishmenthistory (name, uuid, reason, operator, punishmentType, start, endTime, server) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS
            ).use { statement ->
                listOf(name, uuid, reason, operator, type, start, end, server).forEachIndexed { index, value ->
                    statement.setObject(index + 1, value)
                }
                statement.executeUpdate()
                statement.generatedKeys.use { keys -> check(keys.next()) { "History insert returned no ID" }; keys.getInt(1) }
            }
            conn.update("INSERT INTO punishment_templates (historyId, templateName, playerUuid) VALUES (?, ?, ?)",
                listOf(id, template.lowercase(Locale.ROOT), playerUuid))
            conn.commit()
        } catch (error: Exception) {
            conn.rollback()
            throw error
        }
    }

    fun count(uuid: String, name: String, reason: String, template: String): Int = connection().use { conn ->
        conn.rows("SELECT * FROM punishmenthistory WHERE $eligible", eligibleParams(uuid, name, reason, template))
            .distinctBy { row ->
                // One /punish BANIP can create multiple rows, one per known IP.
                if (row.type == "BANIP") listOf(row.name.lowercase(Locale.ROOT), row.type, row.start, row.operator, row.server)
                else listOf(row.id)
            }.size
    }

    /** Empty means missing, already undone, wrong player or inaccessible server. Errors propagate. */
    fun undo(uuid: String, name: String, reason: String, template: String, operator: String): List<PunishmentData> = connection().use { conn ->
        conn.autoCommit = false
        try {
            val selected = conn.rows(
                "SELECT * FROM punishmenthistory WHERE $eligible ORDER BY start DESC, id DESC",
                eligibleParams(uuid, name, reason, template)
            ).firstOrNull()
            if (selected == null) {
                conn.rollback()
                return@use emptyList()
            }
            val rows = if (selected.type == "BANIP") conn.rows(
                "SELECT * FROM punishmenthistory WHERE $eligible AND punishmentType = 'BANIP' AND name = ? AND reason = ? " +
                    "AND operator = ? AND start = ? AND server = ?",
                eligibleParams(uuid, name, reason, template) + listOf(selected.name, selected.reason, selected.operator, selected.start, selected.server)
            ) else listOf(selected)
            val now = System.currentTimeMillis()
            for (row in rows) {
                // The primary key also prevents concurrent or repeated reversals of the same entry.
                conn.update("INSERT INTO punishment_undos (historyId, undoneBy, undoneAt) VALUES (?, ?, ?)",
                    listOf(row.id, operator, now))
                // Active and history IDs are independent; match the original event, never the IDs.
                conn.update("DELETE FROM punishments WHERE uuid = ? AND punishmentType = ? AND start = ? AND server = ? AND operator = ?",
                    listOf(row.uuid, row.type, row.start, row.server, row.operator))
                if (row.type == "BAN" || row.type == "BANIP") {
                    // A queued disconnect must not fire later for a ban that no longer exists.
                    conn.update("DELETE FROM bridge_events WHERE action = ? AND target = ? AND reason = ? " +
                        "AND endTime = ? AND processed = 0 AND NOT EXISTS " +
                        "(SELECT 1 FROM punishments WHERE uuid = ? AND punishmentType = ?)",
                        listOf(row.type, row.uuid, row.reason, row.end, row.uuid, row.type))
                }
            }
            conn.commit()
            rows
        } catch (error: Exception) {
            conn.rollback()
            throw error
        }
    }

    private fun Connection.update(sql: String, params: List<Any>) = prepareStatement(sql).use { statement ->
        params.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
        statement.executeUpdate()
    }

    private fun Connection.rows(sql: String, params: List<Any>): List<PunishmentData> = prepareStatement(sql).use { statement ->
        params.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
        statement.executeQuery().use { result -> buildList { while (result.next()) add(result.punishment()) } }
    }

    private fun ResultSet.punishment() = PunishmentData(
        getInt("id"), getString("uuid"), getString("punishmentType"), getString("reason"),
        getLong("start"), getLong("endTime"), getString("name"), getString("operator"), getString("server")
    )
}
