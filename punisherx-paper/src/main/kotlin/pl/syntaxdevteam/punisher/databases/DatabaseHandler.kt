package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.syntaxdb.Database
import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import pl.syntaxdevteam.syntaxdb.row.Row
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.identity.PunisherIdentityMigrationBridgeResult
import pl.syntaxdevteam.punisher.identity.PunisherIdentityMigrationStore
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

data class DatabaseHealthCheckResult(
    val type: DatabaseType,
    val ok: Boolean,
    val message: String,
    val durationMs: Long
)

/** PunisherX persistence backed by an owned SyntaxDB pool. JDBC escape hatches stay scoped. */
class DatabaseHandler(private val plugin: PunisherX) {
    private val logger = plugin.logger

    private companion object {
        private const val NETWORK_SCOPE = "network"
    }

    private val ready = AtomicBoolean(false)
    private val lifecycleErrorReported = AtomicBoolean(false)
    private val dbType = PunisherDatabaseSettings.parseType(plugin.config.getString("database.type") ?: "sqlite")
    private val config = PunisherDatabaseSettings.create(plugin.config, plugin.dataFolder, plugin.name, dbType)
    private val db = Database.create(config)
    private val migrationInProgress = AtomicBoolean(false)
    /** Opens connection pool. */
    fun openConnection() {
        db.start()
    }

    fun databaseType(): DatabaseType = dbType

    fun runHealthCheck(): DatabaseHealthCheckResult {
        val start = System.currentTimeMillis()
        return try {
            check(db.healthCheck()) { "Database validation failed" }
            val duration = System.currentTimeMillis() - start
            DatabaseHealthCheckResult(dbType, true, "Connection and simple query succeeded.", duration)
        } catch (exception: Exception) {
            val duration = System.currentTimeMillis() - start
            DatabaseHealthCheckResult(dbType, false, exception.message ?: "Unknown database error", duration)
        }
    }

    /** Closes the datasource. */
    fun closeConnection() {
        ready.set(false)
        db.close()
    }

    private fun reportDatabaseFailure(operation: String, error: Exception) {
        logger.err("$operation. ${error.message}")
        if (!db.isReady &&
            lifecycleErrorReported.compareAndSet(false, true)
        ) {
            // Keep the caller stack: hot reload can leave several plugin instances
            // sharing the same log prefix, so a message alone cannot identify the owner.
            plugin.server.logger.log(
                java.util.logging.Level.SEVERE,
                "[PunisherX] Database lifecycle failure: operation=$operation, " +
                    "enabled=${plugin.isEnabled}, ready=${ready.get()}, " +
                    "pluginInstance=${System.identityHashCode(plugin)}, " +
                    "databaseInstance=${System.identityHashCode(this)}, " +
                    "classLoader=${javaClass.classLoader}",
                error
            )
        }
    }

    /** Utility for executing update/insert/delete statements. */
    private fun execute(sql: String, vararg params: Any) {
        db.execute(sql, params.toList())
    }

    /** Utility for running select queries. */
    private fun <T> query(sql: String, vararg params: Any, mapper: (Row) -> T): List<T> {
        val (statement, parameters) = paginatedSql(sql, params.toList())
        return db.query(statement, parameters).map(mapper)
    }

    private fun paginatedSql(sql: String, parameters: List<Any?>): Pair<String, List<Any?>> {
        if (dbType != DatabaseType.SQL_SERVER) return sql to parameters
        val match = Regex(" LIMIT (\\?|[0-9]+)(?: OFFSET (\\?|[0-9]+))?$", RegexOption.IGNORE_CASE).find(sql)
            ?: return sql to parameters
        val values = parameters.toMutableList()
        val offsetToken = match.groupValues[2]
        val offset = if (offsetToken == "?") values.removeAt(values.lastIndex) else offsetToken.toIntOrNull() ?: 0
        val limit = if (match.groupValues[1] == "?") values.removeAt(values.lastIndex) else match.groupValues[1].toInt()
        val base = sql.substring(0, match.range.first)
        val order = if (base.contains("ORDER BY", ignoreCase = true)) "" else " ORDER BY id"
        return "$base$order OFFSET ? ROWS FETCH NEXT ? ROWS ONLY" to (values + offset + limit)
    }

    fun serverScope(): String = normalizedServerScope()

    fun isReady(): Boolean = ready.get()

    private fun normalizedServerScope(): String {
        val raw = plugin.config.getString("server")?.trim().orEmpty()
        return raw.ifEmpty { NETWORK_SCOPE }.lowercase()
    }

    private fun appendServerFilter(sql: String, params: MutableList<Any>, hasWhere: Boolean): String {
        val scope = normalizedServerScope()
        if (scope == NETWORK_SCOPE) return sql
        params.add(scope)
        params.add(NETWORK_SCOPE)
        return if (hasWhere) {
            "$sql AND (server = ? OR server = ?)"
        } else {
            "$sql WHERE (server = ? OR server = ?)"
        }
    }

    /** Existing tables are preserved; schema failure prevents the plugin becoming ready. */
    fun createTables() {
        ready.set(false)
        PunisherDatabaseSchema.create(db)
        ready.set(true)
    }

    // ---------------------------------------------------------------------
    // Data modification helpers
    // ---------------------------------------------------------------------

    fun addPunishment(
        name: String,
        uuid: String,
        reason: String,
        operator: String,
        punishmentType: String,
        start: Long,
        end: Long,
        server: String = normalizedServerScope()
    ): Long? {
        return try {
            val values = linkedMapOf<String, Any?>(
                "name" to name, "uuid" to uuid, "reason" to reason, "operator" to operator,
                "punishmentType" to punishmentType, "start" to start, "endTime" to end, "server" to server
            ).mapKeys { PunisherDatabaseSchema.column(db, it.key) }
            val punishmentId = (db.insert("punishments", values).generatedKey as? Number)?.toLong()
            if (punishmentId != null) {
                plugin.discordWebhook.sendPunishmentWebhook(
                    playerId = punishmentId.toString(),
                    playerName = name,
                    adminName = operator,
                    reason = reason,
                    type = punishmentType,
                    start = start,
                    end = end
                )
            }
            punishmentId
        } catch (e: Exception) {
            logger.err("Failed to add punishment for player $name. ${e.message}")
            null
        }
    }

    fun enqueueBridgeEvent(action: String, target: String, reason: String, end: Long) {
        runCatching {
            execute(
                """
                INSERT INTO bridge_events (action, target, reason, endTime, processed)
                VALUES (?, ?, ?, ?, 0)
                """.trimIndent(),
                action.uppercase(),
                target,
                reason,
                end
            )
        }.onFailure { exception ->
            logger.err("Failed to enqueue bridge event for $action on $target: ${exception.message}")
        }
    }

    fun addPunishmentHistory(
        name: String,
        uuid: String,
        reason: String,
        operator: String,
        punishmentType: String,
        start: Long,
        end: Long,
        server: String = normalizedServerScope()
    ) {
        try {
            execute(
                """
                INSERT INTO punishmenthistory (name, uuid, reason, operator, punishmentType, start, endTime, server)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                name, uuid, reason, operator, punishmentType, start, end, server
            )
        } catch (e: Exception) {
            logger.err("Failed to add punishment history for player $name. ${e.message}")
        }
    }

    /** The original history and template identity are persisted together. */
    fun addTemplatePunishmentHistory(
        name: String, uuid: String, reason: String, operator: String, punishmentType: String,
        start: Long, end: Long, templateName: String, templateUuid: String,
        server: String = normalizedServerScope()
    ) {
        try {
            PunishmentUndoStore(db, normalizedServerScope()).record(
                name, uuid, reason, operator, punishmentType, start, end, server, templateName, templateUuid)
        } catch (error: Exception) {
            logger.err("Failed to add template punishment history for $name. ${error.message}")
        }
    }

    /** Throws on read errors so a database failure cannot reset escalation to level one. */
    fun countEscalationHistory(uuid: String, name: String, reason: String, template: String): Int =
        PunishmentUndoStore(db, normalizedServerScope()).count(uuid, name, reason, template)

    fun undoPunishment(uuid: String, name: String, reason: String, template: String, operator: String): List<PunishmentData>? = try {
        PunishmentUndoStore(db, normalizedServerScope()).undo(uuid, name, reason, template, operator)
    } catch (error: Exception) {
        logger.err("Failed to undo punishment for $uuid. ${error.message}")
        null
    }

    fun removePunishment(uuidOrIp: String, punishmentType: String, removeAll: Boolean = false) {
        try {
            db.transaction {
                val params = mutableListOf<Any>(uuidOrIp, punishmentType)
                val filter = appendServerFilter("SELECT id FROM punishments WHERE uuid = ? AND punishmentType = ?", params, true)
                if (removeAll) {
                    execute(filter.replace("SELECT id", "DELETE"), params)
                } else {
                    val (sql, values) = paginatedSql("$filter ORDER BY start DESC, id DESC LIMIT 1", params)
                    query(sql, values).firstOrNull()?.int("id")?.let { id ->
                        delete("punishments") { "id" eq id }
                    }
                }
            }
        } catch (e: Exception) {
            logger.err("Failed to remove punishment of type $punishmentType for $uuidOrIp. ${e.message}")
        }
    }

    fun removePunishmentById(id: Int): Boolean {
        return try {
            val params = mutableListOf<Any>(id)
            val sql = appendServerFilter("DELETE FROM punishments WHERE id = ?", params, true)
            db.execute(sql, params) > 0
        } catch (e: Exception) {
            logger.err("Failed to remove punishment id $id. ${e.message}")
            false
        }
    }

    fun deletePlayerData(uuid: String) {
        try {
            db.transaction {
                val punishmentsParams = mutableListOf<Any>(uuid)
                val punishmentsSql = appendServerFilter("DELETE FROM punishments WHERE uuid = ?", punishmentsParams, true)
                execute(punishmentsSql, punishmentsParams)
                val historyParams = mutableListOf<Any>(uuid)
                val historySql = appendServerFilter("DELETE FROM punishmenthistory WHERE uuid = ?", historyParams, true)
                for (table in listOf("punishment_templates", "punishment_undos")) {
                    val params = mutableListOf<Any>(uuid)
                    val historyIds = appendServerFilter("SELECT id FROM punishmenthistory WHERE uuid = ?", params, true)
                    execute("DELETE FROM $table WHERE historyId IN ($historyIds)", params)
                }
                execute(historySql, historyParams)
            }
        } catch (e: Exception) {
            logger.err("Failed to delete player data. ${e.message}")
        }
    }

    fun addReport(player: UUID, suspect: UUID, reason: String): Boolean {
        return try {
            execute(
                """
                INSERT INTO reports (player, suspect, reason)
                VALUES (?, ?, ?)
                """.trimIndent(),
                player.toString(),
                suspect.toString(),
                reason
            )
            true
        } catch (e: Exception) {
            logger.err("Failed to add report from $player against $suspect. ${e.message}")
            false
        }
    }

    fun countOpenReportsByReporter(player: UUID): Int {
        return try {
            query(
                "SELECT COUNT(*) AS reportCount FROM reports WHERE player = ? AND NOT EXISTS (SELECT 1 FROM report_resolutions rr WHERE rr.reportId = reports.id)",
                player.toString()
            ) { (it.int("reportCount") ?: 0) }
                .firstOrNull()
                ?: 0
        } catch (e: Exception) {
            logger.err("Failed to check reports submitted by $player. ${e.message}")
            0
        }
    }

    @Synchronized
    fun submitReport(player: UUID, suspect: UUID, reason: String, maxOpenReports: Int): ReportSubmissionResult {
        return try {
            val duplicateReports = query(
                "SELECT COUNT(*) AS reportCount FROM reports WHERE player = ? AND suspect = ? AND NOT EXISTS (SELECT 1 FROM report_resolutions rr WHERE rr.reportId = reports.id)",
                player.toString(),
                suspect.toString()
            ) { (it.int("reportCount") ?: 0) }.firstOrNull() ?: 0
            if (duplicateReports > 0) {
                return ReportSubmissionResult.ReporterAlreadyReportedSuspect
            }

            val openReports = query(
                "SELECT COUNT(*) AS reportCount FROM reports WHERE player = ? AND NOT EXISTS (SELECT 1 FROM report_resolutions rr WHERE rr.reportId = reports.id)",
                player.toString()
            ) { (it.int("reportCount") ?: 0) }.firstOrNull() ?: 0
            if (openReports >= maxOpenReports.coerceAtLeast(1)) {
                return ReportSubmissionResult.ReporterReachedOpenReportLimit
            }

            if (!addReport(player, suspect, reason)) {
                return ReportSubmissionResult.DatabaseError
            }

            val suspectReportCount = query(
                "SELECT COUNT(*) AS reportCount FROM reports WHERE suspect = ? AND NOT EXISTS (SELECT 1 FROM report_resolutions rr WHERE rr.reportId = reports.id)",
                suspect.toString()
            ) { (it.int("reportCount") ?: 0) }.firstOrNull() ?: 1
            ReportSubmissionResult.Accepted(suspectReportCount)
        } catch (e: Exception) {
            logger.err("Failed to submit report from $player against $suspect. ${e.message}")
            ReportSubmissionResult.DatabaseError
        }
    }

    @Synchronized
    fun resolveReport(id: Int, status: String, handledBy: String, note: String): Boolean {
        require(status == "RESOLVED" || status == "REJECTED")
        require(note.length in 3..255)
        val report = getReports(reportId = id).firstOrNull() ?: return false
        if (report.status != "OPEN") return false
        // The primary key prevents two admins (including on different servers)
        // from overwriting each other's decision.
        execute("INSERT INTO report_resolutions (reportId, status, handledBy, note, handledAt) VALUES (?, ?, ?, ?, ?)",
            id, status, handledBy, note, System.currentTimeMillis())
        return true
    }

    fun deleteReport(id: Int): Boolean {
        return try {
            db.transaction {
                execute("DELETE FROM report_resolutions WHERE reportId = ?", listOf(id))
                execute("DELETE FROM reports WHERE id = ?", listOf(id)) > 0
            }
        } catch (e: Exception) {
            logger.err("Failed to delete report with ID $id. ${e.message}")
            false
        }
    }

    fun getReports(limit: Int? = null, offset: Int? = null, closed: Boolean = false, reportId: Int? = null): List<ReportData> {
        var sql = "SELECT r.id, r.player, r.suspect, r.reason, r.filedAt, rr.status, rr.handledBy, rr.note, rr.handledAt FROM reports r LEFT JOIN report_resolutions rr ON rr.reportId = r.id"
        val params = mutableListOf<Any>()
        if (reportId != null) {
            sql += " WHERE r.id = ?"
            params.add(reportId)
        } else {
            sql += if (closed) " WHERE rr.reportId IS NOT NULL" else " WHERE rr.reportId IS NULL"
        }
        sql += " ORDER BY r.filedAt DESC, r.id DESC"

        if (limit != null) {
            sql += " LIMIT ?"
            params.add(limit)
            if (offset != null) {
                sql += " OFFSET ?"
                params.add(offset)
            }
        }

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                val id = (rs.int("id") ?: 0)
                val playerId = runCatching { UUID.fromString(rs.string("player").orEmpty()) }.getOrElse {
                    logger.warning("Skipping report $id due to invalid reporter UUID: ${rs.string("player").orEmpty()}")
                    return@query null
                }
                val suspectId = runCatching { UUID.fromString(rs.string("suspect").orEmpty()) }.getOrElse {
                    logger.warning("Skipping report $id due to invalid suspect UUID: ${rs.string("suspect").orEmpty()}")
                    return@query null
                }

                val filedAt = reportTimestamp(rs["filedAt"])

                ReportData(
                    id = id,
                    player = playerId,
                    suspect = suspectId,
                    reason = rs.string("reason").orEmpty(),
                    filedAt = filedAt,
                    status = rs.string("status") ?: "OPEN",
                    handledBy = rs.string("handledBy"),
                    note = rs.string("note"),
                    handledAt = rs.long("handledAt")?.let(Instant::ofEpochMilli)
                )
            }.filterNotNull()
        } catch (e: Exception) {
            logger.err("Failed to fetch reports. ${e.message}")
            throw e
        }
    }

    fun updatePunishmentReason(id: Int, newReason: String): Boolean {
        return try {
            val params = mutableListOf<Any>(newReason, id)
            val sql = appendServerFilter("UPDATE punishmenthistory SET reason = ? WHERE id = ?", params, true)
            db.execute(sql, params) > 0
        } catch (e: Exception) {
            logger.err("Failed to update punishment reason for ID: $id. ${e.message}")
            false
        }
    }

    // Player cache helpers -------------------------------------------------

    fun savePlayerCacheLine(data: String) {
        try {
            execute("INSERT INTO playercache (data) VALUES (?)", data)
        } catch (e: Exception) {
            reportDatabaseFailure("Failed to save player cache line", e)
        }
    }

    fun getPlayerCacheLines(): List<String> {
        return try {
            query("SELECT data FROM playercache ORDER BY id ASC") { rs -> rs.string("data").orEmpty() }
        } catch (e: Exception) {
            reportDatabaseFailure("Failed to load player cache lines", e)
            emptyList()
        }
    }

    fun overwritePlayerCache(lines: List<String>) {
        try { replacePlayerCacheLinesTransactional(lines) }
        catch (e: Exception) { logger.err("Failed to overwrite player cache. ${e.message}") }
    }

    internal fun replacePlayerCacheLinesTransactional(lines: List<String>) {
        db.transaction {
            deleteAll("playercache")
            if (lines.isNotEmpty()) batchInsert("playercache", lines.map { mapOf("data" to it) })
        }
    }

    internal fun inspectIdentityMigration(
        migrationId: UUID,
        sourceUuid: UUID,
        targetUuid: UUID,
    ): PunisherIdentityMigrationBridgeResult =
        db.withConnection { connection ->
            PunisherIdentityMigrationStore.inspect(connection, migrationId, sourceUuid, targetUuid)
        }

    internal fun migrateIdentity(
        migrationId: UUID,
        sourceUuid: UUID,
        targetUuid: UUID,
    ): PunisherIdentityMigrationBridgeResult =
        db.withConnection { connection ->
            PunisherIdentityMigrationStore.migrate(connection, migrationId, sourceUuid, targetUuid)
        }

    internal fun rollbackIdentityMigration(
        migrationId: UUID,
        sourceUuid: UUID,
        targetUuid: UUID,
    ): PunisherIdentityMigrationBridgeResult =
        db.withConnection { connection ->
            PunisherIdentityMigrationStore.rollback(connection, migrationId, sourceUuid, targetUuid)
        }

    // ---------------------------------------------------------------------
    // Query helpers
    // ---------------------------------------------------------------------

    fun getPunishments(uuid: String, limit: Int? = null, offset: Int? = null): List<PunishmentData> {
        val params = mutableListOf<Any>(uuid)
        var sql = appendServerFilter("SELECT * FROM punishments WHERE uuid = ?", params, true)

        if (limit != null && offset != null) {
            sql += " ORDER BY start DESC, id DESC LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return try {
            val rows = query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0),
                    uuid,
                    rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(),
                    (rs.long("start") ?: 0L),
                    (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(),
                    rs.string("operator").orEmpty(),
                    rs.string("server").orEmpty()
                )
            }
            val punishmentsToRemove = rows.filterNot { plugin.punishmentManager.isPunishmentActive(it) }
            punishmentsToRemove.forEach { removePunishmentById(it.id) }

            rows.filter { plugin.punishmentManager.isPunishmentActive(it) }
        } catch (e: Exception) {
            reportDatabaseFailure("Failed to get punishments for UUID: $uuid", e)
            emptyList()        }
    }

    fun getActivePunishmentsString(uuid: UUID): String? {
        val punishments = getPunishments(uuid.toString())
        if (punishments.isEmpty()) return null
        return punishments.joinToString(", ") { it.type }
    }

    fun getPunishmentsByIP(ip: String): List<PunishmentData> {
        return try {
            val params = mutableListOf<Any>(ip)
            val sql = appendServerFilter("SELECT * FROM punishments WHERE uuid = ?", params, true)
            val rows = query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0),
                    ip,
                    rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(),
                    (rs.long("start") ?: 0L),
                    (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(),
                    rs.string("operator").orEmpty(),
                    rs.string("server").orEmpty()
                )
            }
            rows.filter { plugin.punishmentManager.isPunishmentActive(it) }
        } catch (e: Exception) {
            reportDatabaseFailure("Failed to get punishments for IP: $ip", e)
            emptyList()
        }
    }

    fun getPunishmentHistory(uuid: String, limit: Int? = null, offset: Int? = null): MutableList<PunishmentData> {
        val params = mutableListOf<Any>(uuid)
        var sql = appendServerFilter("SELECT *, CASE WHEN EXISTS (SELECT 1 FROM punishment_undos u " +
            "WHERE u.historyId = punishmenthistory.id) THEN 1 ELSE 0 END AS undone FROM punishmenthistory WHERE uuid = ?", params, true)
        if (limit != null && offset != null) {
            sql += " ORDER BY start DESC, id DESC LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0),
                    uuid,
                    rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(),
                    (rs.long("start") ?: 0L),
                    (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(),
                    rs.string("operator").orEmpty(),
                    rs.string("server").orEmpty(),
                    (rs.boolean("undone") ?: false)
                )
            }.toMutableList()
        } catch (e: Exception) {
            logger.err("Failed to get punishment history for UUID: $uuid. ${e.message}")
            mutableListOf()
        }
    }

    fun getBannedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val params = mutableListOf<Any>()
        var sql = appendServerFilter(
            "SELECT * FROM punishments WHERE punishmentType IN ('BAN', 'BANIP')",
            params,
            true
        )
        sql += " ORDER BY start DESC, id DESC LIMIT ? OFFSET ?"
        params.add(limit)
        params.add(offset)

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0),
                    rs.string("uuid").orEmpty(),
                    rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(),
                    (rs.long("start") ?: 0L),
                    (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(),
                    rs.string("operator").orEmpty(),
                    rs.string("server").orEmpty()
                )
            }.toMutableList()
        } catch (e: Exception) {
            logger.err("Failed to get banned players: ${e.message}")
            mutableListOf()
        }
    }

    fun getActivePunishmentsFiltered(
        types: Set<String>,
        nameQuery: String,
        limit: Int,
        offset: Int
    ): List<PunishmentData> {
        val normalizedTypes = types.map { it.uppercase() }.filter { it in setOf("BAN", "BANIP", "JAIL", "MUTE", "WARN") }
        val params = mutableListOf<Any>()
        val conditions = mutableListOf<String>()
        if (normalizedTypes.isNotEmpty()) {
            conditions += "punishmentType IN (${normalizedTypes.joinToString(",") { "?" }})"
            params.addAll(normalizedTypes)
        }
        if (nameQuery.isNotBlank()) {
            conditions += "LOWER(name) LIKE ?"
            params += "%${nameQuery.lowercase()}%"
        }
        var sql = "SELECT * FROM punishments"
        if (conditions.isNotEmpty()) sql += " WHERE " + conditions.joinToString(" AND ")
        sql = appendServerFilter(sql, params, conditions.isNotEmpty())
        sql += " ORDER BY start DESC, id DESC LIMIT ? OFFSET ?"
        params += limit
        params += offset
        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0), rs.string("uuid").orEmpty(), rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(), (rs.long("start") ?: 0L), (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(), rs.string("operator").orEmpty(), rs.string("server").orEmpty()
                )
            }.filter { plugin.punishmentManager.isPunishmentActive(it) }
        } catch (e: Exception) {
            logger.err("Failed to filter active punishments: ${e.message}")
            emptyList()
        }
    }

    fun getHistoryBannedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val params = mutableListOf<Any>()
        var sql = appendServerFilter(
            "SELECT * FROM punishmenthistory WHERE punishmentType IN ('BAN', 'BANIP')",
            params,
            true
        )
        sql += " ORDER BY start DESC, id DESC LIMIT ? OFFSET ?"
        params.add(limit)
        params.add(offset)

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0),
                    rs.string("uuid").orEmpty(),
                    rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(),
                    (rs.long("start") ?: 0L),
                    (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(),
                    rs.string("operator").orEmpty(),
                    rs.string("server").orEmpty()
                )
            }.toMutableList()
        } catch (e: Exception) {
            logger.err("Failed to get banned players: ${e.message}")
            mutableListOf()
        }
    }

    fun getJailedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val params = mutableListOf<Any>()
        var sql = appendServerFilter(
            "SELECT * FROM punishments WHERE punishmentType = 'JAIL'",
            params,
            true
        )
        sql += " ORDER BY start DESC, id DESC LIMIT ? OFFSET ?"
        params.add(limit)
        params.add(offset)

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0),
                    rs.string("uuid").orEmpty(),
                    rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(),
                    (rs.long("start") ?: 0L),
                    (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(),
                    rs.string("operator").orEmpty(),
                    rs.string("server").orEmpty()
                )
            }.toMutableList()
        } catch (e: Exception) {
            logger.err("Failed to get jailed players: ${e.message}")
            mutableListOf()
        }
    }

    fun getActiveWarnCount(uuid: String): Int {
        val currentTime = System.currentTimeMillis()
        return try {
            val params = mutableListOf<Any>(uuid, currentTime)
            val sql = appendServerFilter(
                "SELECT COUNT(*) AS cnt FROM punishments WHERE uuid = ? AND punishmentType = 'WARN' AND (endTime = -1 OR endTime > ?)",
                params,
                true
            )
            query(sql, *params.toTypedArray()) { rs -> (rs.int("cnt") ?: 0) }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count active warns: ${e.message}")
            0
        }
    }

    fun countAllPunishments(): Int {
        return try {
            val params = mutableListOf<Any>()
            val sql = appendServerFilter("SELECT COUNT(*) AS cnt FROM punishments", params, false)
            query(sql, *params.toTypedArray()) { rs -> (rs.int("cnt") ?: 0) }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count punishments. ${e.message}")
            0
        }
    }

    fun countAllPunishmentHistory(): Int {
        return try {
            val params = mutableListOf<Any>()
            val sql = appendServerFilter("SELECT COUNT(*) AS cnt FROM punishmenthistory", params, false)
            query(sql, *params.toTypedArray()) { rs -> (rs.int("cnt") ?: 0) }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count punishment history. ${e.message}")
            0
        }
    }

    fun countTodayPunishments(): Int {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return try {
            var count = 0
            val punishParams = mutableListOf<Any>(startOfDay)
            val punishSql = appendServerFilter(
                "SELECT COUNT(*) AS cnt FROM punishments WHERE start >= ?",
                punishParams,
                true
            )
            count += query(punishSql, *punishParams.toTypedArray()) { rs -> (rs.int("cnt") ?: 0) }.firstOrNull() ?: 0
            val historyParams = mutableListOf<Any>(startOfDay)
            val historySql = appendServerFilter(
                "SELECT COUNT(*) AS cnt FROM punishmenthistory WHERE start >= ?",
                historyParams,
                true
            )
            count += query(historySql, *historyParams.toTypedArray()) { rs -> (rs.int("cnt") ?: 0) }.firstOrNull() ?: 0
            count
        } catch (e: Exception) {
            logger.err("Failed to count today's punishments. ${e.message}")
            0
        }
    }

    fun countPlayerAllPunishmentHistory(uuid: UUID): Int {
        return try {
            val params = mutableListOf<Any>(uuid.toString())
            val sql = appendServerFilter(
                "SELECT COUNT(*) AS cnt FROM punishmenthistory WHERE uuid = ?",
                params,
                true
            )
            query(sql, *params.toTypedArray()) { rs -> (rs.int("cnt") ?: 0) }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count punishment history. ${e.message}")
            0
        }
    }

    fun getLastTenPunishments(uuid: String): List<PunishmentData> {
        return try {
            val params = mutableListOf<Any>(uuid)
            var sql = appendServerFilter(
                "SELECT * FROM punishmenthistory WHERE uuid = ?",
                params,
                true
            )
            sql += " ORDER BY start DESC, id DESC LIMIT 10"
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    (rs.int("id") ?: 0),
                    uuid,
                    rs.string("punishmentType").orEmpty(),
                    rs.string("reason").orEmpty(),
                    (rs.long("start") ?: 0L),
                    (rs.long("endTime") ?: 0L),
                    rs.string("name").orEmpty(),
                    rs.string("operator").orEmpty(),
                    rs.string("server").orEmpty()
                )
            }
        } catch (e: Exception) {
            logger.err("Failed to get last ten punishments for UUID: $uuid. ${e.message}")
            emptyList()
        }
    }

    private fun reportTimestamp(value: Any?): Instant = when (value) {
        null -> Instant.EPOCH
        is java.sql.Timestamp -> value.toInstant()
        is java.util.Date -> value.toInstant()
        is Number -> Instant.ofEpochMilli(value.toLong())
        else -> runCatching { Instant.parse(value.toString()) }.getOrElse {
            java.time.LocalDateTime.parse(value.toString().replace(' ', 'T')).atZone(ZoneId.systemDefault()).toInstant()
        }
    }

    fun exportDatabase() {
        try {
            DatabaseBackup.write(DatabaseBackup.snapshot(db), File(plugin.dataFolder, "dump/backup.sql"))
            logger.success("Database exported to ${plugin.dataFolder}/dump/backup.sql")
        } catch (error: Exception) { logger.err("Failed to export database. ${error.message}"); throw error }
    }

    fun importDatabase() {
        try {
            DatabaseBackup.restore(db, DatabaseBackup.read(File(plugin.dataFolder, "dump/backup.sql")))
            logger.success("Database imported from ${plugin.dataFolder}/dump/backup.sql")
        } catch (error: Exception) { logger.err("Failed to import database. ${error.message}"); throw error }
    }

    data class MigrationResult(val success: Boolean, val message: String)

    fun migrateDatabase(from: DatabaseType, to: DatabaseType): CompletableFuture<MigrationResult> {
        if (!migrationInProgress.compareAndSet(false, true))
            return CompletableFuture.completedFuture(MigrationResult(false, "Another migration task is already running."))
        val future = CompletableFuture<MigrationResult>()
        if (from != dbType || from == to) {
            migrationInProgress.set(false)
            return CompletableFuture.completedFuture(MigrationResult(false, "Source must match the active database and target must use a different engine."))
        }
        try {
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                try {
                    val configFile = File(plugin.dataFolder, "config.yml")
                    val yaml = YamlConfiguration.loadConfiguration(configFile)
                    val targetConfig = PunisherDatabaseSettings.create(yaml, plugin.dataFolder, plugin.name, to)
                    require(targetConfig.jdbcUrl != config.jdbcUrl) { "Source and target point to the same database." }
                    val snapshot = DatabaseBackup.snapshot(db)
                    DatabaseBackup.write(snapshot, File(plugin.dataFolder, "dump/backup.sql"))
                    Database.connect(targetConfig).use { target ->
                        PunisherDatabaseSchema.create(target)
                        require(PunisherDatabaseSchema.tables.all { target.count(it) == 0L }) {
                            "Target contains PunisherX data. Migration aborted without deleting it."
                        }
                        DatabaseBackup.restore(target, snapshot)
                        check(snapshot.all { (table, rows) -> target.count(table) == rows.size.toLong() }) {
                            "Target row-count verification failed; source configuration has not changed."
                        }
                    }
                    yaml.set("database.type", to.configName)
                    yaml.save(configFile)
                    // Live server traffic can still write during transfer; perform migrations in maintenance mode.
                    plugin.server.scheduler.runTask(plugin, Runnable { plugin.onReload() })
                    future.complete(MigrationResult(true, "Database migrated from ${from.configName} to ${to.configName}"))
                } catch (error: Exception) {
                    logger.err("Failed to migrate database. ${error.message}")
                    future.complete(MigrationResult(false, error.message ?: "Database migration failed"))
                } finally { migrationInProgress.set(false) }
            })
        } catch (error: Exception) {
            migrationInProgress.set(false)
            future.complete(MigrationResult(false, error.message ?: "Could not schedule migration"))
        }
        return future
    }
}
