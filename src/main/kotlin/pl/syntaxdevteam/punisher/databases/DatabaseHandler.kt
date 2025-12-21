package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.core.database.*
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.sql.DatabaseMetaData
import java.sql.Statement
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

/**
 * Database access layer backed by SyntaxCore's [DatabaseManager].
 *
 * The previous implementation manually configured HikariCP and handled
 * connections on its own.  SyntaxCore 1.2.0 provides a lightweight wrapper
 * around a pooled datasource together with simple `execute` and `query`
 * helpers.  By delegating to [DatabaseManager] the plugin gains out of the box
 * support for multiple database engines while keeping the code much simpler.
 */
class DatabaseHandler(private val plugin: PunisherX) {
    private val logger = plugin.logger

    private companion object {
        private const val NETWORK_SCOPE = "network"
    }

    private val dbType: DatabaseType = plugin.config
        .getString("database.type")
        ?.uppercase()
        ?.let { DatabaseType.valueOf(it) }
        ?: DatabaseType.SQLITE
    private val config = DatabaseConfig(
        type = dbType,
        host = plugin.config.getString("database.sql.host") ?: "localhost",
        port = plugin.config.getInt("database.sql.port").takeIf { it != 0 } ?: 3306,
        database = resolveDatabaseName(),
        username = plugin.config.getString("database.sql.username") ?: "ROOT",
        password = plugin.config.getString("database.sql.password") ?: "U5eV3ryStr0ngP4ssw0rd"
    )
    private val db = DatabaseManager(config, logger)
    private val migrationInProgress = AtomicBoolean(false)
    /** Opens connection pool. */
    fun openConnection() {
        db.connect()
    }

    fun databaseType(): DatabaseType = dbType

    fun runHealthCheck(): DatabaseHealthCheckResult {
        val start = System.currentTimeMillis()
        return try {
            db.query("SELECT 1") { resultSet -> resultSet.getString(1) }
            val duration = System.currentTimeMillis() - start
            DatabaseHealthCheckResult(dbType, true, "Connection and simple query succeeded.", duration)
        } catch (exception: Exception) {
            val duration = System.currentTimeMillis() - start
            DatabaseHealthCheckResult(dbType, false, exception.message ?: "Unknown database error", duration)
        }
    }

    private fun resolveDatabaseName(): String {
        val configuredName = plugin.config.getString("database.sql.dbname") ?: plugin.name

        return when (dbType) {
            DatabaseType.H2, DatabaseType.SQLITE -> {
                val dataFolder = plugin.dataFolder.apply { mkdirs() }
                File(dataFolder, configuredName).absolutePath
            }
            else -> configuredName
        }
    }

    /** Closes the datasource. */
    fun closeConnection() {
        db.close()
    }

    /** Utility for executing update/insert/delete statements. */
    private fun execute(sql: String, vararg params: Any) {
        db.execute(sql, *params)
    }

    /** Utility for running select queries. */
    private fun <T> query(sql: String, vararg params: Any, mapper: (java.sql.ResultSet) -> T): List<T> {
        return db.query(sql, *params, mapper = mapper)
    }

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

    /** Returns database specific definition for auto increment column. */
    private fun idDefinition(): String = when (dbType) {
        DatabaseType.SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT"
        DatabaseType.POSTGRESQL -> "SERIAL PRIMARY KEY"
        else -> "INT AUTO_INCREMENT PRIMARY KEY"
    }

    private fun reportReasonDefinition(): String = when (dbType) {
        DatabaseType.MYSQL, DatabaseType.MARIADB, DatabaseType.POSTGRESQL, DatabaseType.SQLITE, DatabaseType.H2 ->
            "TEXT"

        else -> "NVARCHAR(MAX)"
    }

    private fun reportFiledAtDefinition(): String = when (dbType) {
        DatabaseType.SQLITE -> "DATETIME DEFAULT CURRENT_TIMESTAMP"
        DatabaseType.POSTGRESQL, DatabaseType.H2 -> "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        DatabaseType.MYSQL, DatabaseType.MARIADB -> "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        else -> "DATETIME2 DEFAULT SYSDATETIME()"
    }

    /** Creates required plugin tables if missing. */
    fun createTables() {
        val punishmentSchema = TableSchema(
            "punishments",
            listOf(
                Column("id", idDefinition()),
                Column("name", "VARCHAR(32)"),
                Column("uuid", "VARCHAR(36)"),
                Column("reason", "VARCHAR(255)"),
                Column("operator", "VARCHAR(16)"),
                Column("punishmentType", "VARCHAR(16)"),
                Column("start", "BIGINT"),
                Column("endTime", "BIGINT"),
                Column("server", "VARCHAR(64) DEFAULT '$NETWORK_SCOPE'")
            )
        )

        val historySchema = punishmentSchema.copy(name = "punishmenthistory")

        db.createTable(punishmentSchema)
        db.createTable(historySchema)
        ensureServerScopeColumns()

        val playerCacheSchema = TableSchema(
            "playercache",
            listOf(
                Column("id", idDefinition()),
                Column("data", "TEXT")
            )
        )
        db.createTable(playerCacheSchema)

        val reportsSchema = TableSchema(
            "reports",
            listOf(
                Column("id", idDefinition()),
                Column("player", "VARCHAR(36)"),
                Column("suspect", "VARCHAR(36)"),
                Column("reason", reportReasonDefinition()),
                Column("filedAt", reportFiledAtDefinition())
            )
        )
        db.createTable(reportsSchema)

        val bridgeQueueSchema = TableSchema(
            "bridge_events",
            listOf(
                Column("id", idDefinition()),
                Column("action", "VARCHAR(16) NOT NULL"),
                Column("target", "VARCHAR(64) NOT NULL"),
                Column("reason", "TEXT"),
                Column("endTime", "BIGINT NOT NULL"),
                Column("processed", "TINYINT DEFAULT 0"),
                Column("processedAt", "BIGINT")
            )
        )
        db.createTable(bridgeQueueSchema)
    }

    private fun ensureServerScopeColumns() {
        val alterStatement = "ALTER TABLE %s ADD COLUMN server VARCHAR(64) DEFAULT '$NETWORK_SCOPE'"
        val tables = listOf("punishments", "punishmenthistory")
        runCatching {
            db.getConnection().use { connection ->
                val metadata = connection.metaData
                tables.forEach { table ->
                    val columnExists = hasColumn(metadata, table, "server")
                    if (!columnExists) {
                        runCatching {
                            execute(alterStatement.format(table))
                        }.onFailure { exception ->
                            logger.debug("Skipping server column migration on $table: ${exception.message}")
                        }
                    }
                }
            }
        }
        runCatching { execute("UPDATE punishments SET server = ? WHERE server IS NULL", NETWORK_SCOPE) }
        runCatching { execute("UPDATE punishmenthistory SET server = ? WHERE server IS NULL", NETWORK_SCOPE) }
    }

    private fun hasColumn(meta: DatabaseMetaData, table: String, column: String): Boolean {
        fun check(tableName: String, columnName: String): Boolean {
            meta.getColumns(null, null, tableName, columnName).use { result ->
                return result.next()
            }
        }

        return check(table, column)
            || check(table.lowercase(), column.lowercase())
            || check(table.uppercase(), column.uppercase())
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
        end: Long
    ): Long? {
        return try {
            val insertSql = """
                INSERT INTO punishments (name, uuid, reason, operator, punishmentType, start, endTime, server)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val punishmentId = db.getConnection().use { connection ->
                when (dbType) {
                    DatabaseType.POSTGRESQL, DatabaseType.SQLITE, DatabaseType.H2 -> {
                        val sql = "$insertSql RETURNING id"
                        connection.prepareStatement(sql).use { statement ->
                            statement.setString(1, name)
                            statement.setString(2, uuid)
                            statement.setString(3, reason)
                            statement.setString(4, operator)
                            statement.setString(5, punishmentType)
                            statement.setLong(6, start)
                            statement.setLong(7, end)
                            statement.setString(8, normalizedServerScope())
                            statement.executeQuery().use { resultSet ->
                                if (resultSet.next()) resultSet.getLong(1) else null
                            }
                        }
                    }
                    DatabaseType.MYSQL, DatabaseType.MARIADB -> {
                        connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                            statement.setString(1, name)
                            statement.setString(2, uuid)
                            statement.setString(3, reason)
                            statement.setString(4, operator)
                            statement.setString(5, punishmentType)
                            statement.setLong(6, start)
                            statement.setLong(7, end)
                            statement.setString(8, normalizedServerScope())
                            val rows = statement.executeUpdate()
                            if (rows == 0) {
                                null
                            } else {
                                statement.generatedKeys.use { resultSet ->
                                    if (resultSet.next()) resultSet.getLong(1) else null
                                }
                            }
                        }
                    }
                    else -> {
                        connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                            statement.setString(1, name)
                            statement.setString(2, uuid)
                            statement.setString(3, reason)
                            statement.setString(4, operator)
                            statement.setString(5, punishmentType)
                            statement.setLong(6, start)
                            statement.setLong(7, end)
                            statement.setString(8, normalizedServerScope())
                            val rows = statement.executeUpdate()
                            if (rows == 0) {
                                null
                            } else {
                                statement.generatedKeys.use { resultSet ->
                                    if (resultSet.next()) resultSet.getLong(1) else null
                                }
                            }
                        }
                    }
                }
            }
            if (punishmentId != null) {
                plugin.discordWebhook.sendPunishmentWebhook(
                    playerId = punishmentId.toString(),
                    playerName = name,
                    adminName = operator,
                    reason = reason,
                    type = punishmentType,
                    duration = end
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
        end: Long
    ) {
        try {
            execute(
                """
                INSERT INTO punishmenthistory (name, uuid, reason, operator, punishmentType, start, endTime, server)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                name, uuid, reason, operator, punishmentType, start, end, normalizedServerScope()
            )
        } catch (e: Exception) {
            logger.err("Failed to add punishment history for player $name. ${e.message}")
        }
    }

    fun removePunishment(uuidOrIp: String, punishmentType: String, removeAll: Boolean = false) {
        val params = mutableListOf<Any>(uuidOrIp, punishmentType)
        val base = appendServerFilter("DELETE FROM punishments WHERE uuid = ? AND punishmentType = ?", params, true)
        val query = if (removeAll) {
            base
        } else {
            when (dbType) {
                DatabaseType.POSTGRESQL, DatabaseType.H2, DatabaseType.SQLITE -> {
                    val subParams = mutableListOf<Any>(uuidOrIp, punishmentType)
                    val subSql = appendServerFilter(
                        "SELECT start FROM punishments WHERE uuid = ? AND punishmentType = ?",
                        subParams,
                        true
                    )
                    params.addAll(subParams)
                    "$base AND start = ($subSql ORDER BY start DESC LIMIT 1)"
                }
                else -> "$base ORDER BY start DESC LIMIT 1"
            }
        }

        try {
            execute(query, *params.toTypedArray())
        } catch (e: Exception) {
            logger.err("Failed to remove punishment of type $punishmentType for $uuidOrIp. ${e.message}")
        }
    }

    fun deletePlayerData(uuid: String) {
        try {
            val punishmentsParams = mutableListOf<Any>(uuid)
            val punishmentsSql = appendServerFilter("DELETE FROM punishments WHERE uuid = ?", punishmentsParams, true)
            execute(punishmentsSql, *punishmentsParams.toTypedArray())
            val historyParams = mutableListOf<Any>(uuid)
            val historySql = appendServerFilter("DELETE FROM punishmenthistory WHERE uuid = ?", historyParams, true)
            execute(historySql, *historyParams.toTypedArray())
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

    fun deleteReport(id: Int): Boolean {
        return try {
            execute("DELETE FROM reports WHERE id = ?", id)
            true
        } catch (e: Exception) {
            logger.err("Failed to delete report with ID $id. ${e.message}")
            false
        }
    }

    fun getReports(limit: Int? = null, offset: Int? = null): List<ReportData> {
        val supportsPagination = dbType in setOf(
            DatabaseType.MYSQL,
            DatabaseType.MARIADB,
            DatabaseType.POSTGRESQL,
            DatabaseType.SQLITE
        )

        var sql = "SELECT id, player, suspect, reason, filedAt FROM reports ORDER BY filedAt DESC, id DESC"
        val params = mutableListOf<Any>()

        if (supportsPagination && limit != null) {
            sql += " LIMIT ?"
            params.add(limit)
            if (offset != null) {
                sql += " OFFSET ?"
                params.add(offset)
            }
        } else if (!supportsPagination && (limit != null || offset != null)) {
            logger.warning("Pagination not supported for reports on database type $dbType. Returning full result set.")
        }

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                val id = rs.getInt("id")
                val playerId = runCatching { UUID.fromString(rs.getString("player")) }.getOrElse {
                    logger.warning("Skipping report $id due to invalid reporter UUID: ${rs.getString("player")}")
                    return@query null
                }
                val suspectId = runCatching { UUID.fromString(rs.getString("suspect")) }.getOrElse {
                    logger.warning("Skipping report $id due to invalid suspect UUID: ${rs.getString("suspect")}")
                    return@query null
                }

                val filedAt = rs.getTimestamp("filedAt")?.toInstant() ?: Instant.EPOCH

                ReportData(
                    id = id,
                    player = playerId,
                    suspect = suspectId,
                    reason = rs.getString("reason"),
                    filedAt = filedAt
                )
            }.filterNotNull()
        } catch (e: Exception) {
            logger.err("Failed to fetch reports. ${e.message}")
            emptyList()
        }
    }

    fun updatePunishmentReason(id: Int, newReason: String): Boolean {
        return try {
            execute("UPDATE punishmentHistory SET reason = ? WHERE id = ?", newReason, id)
            true
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
            logger.err("Failed to save player cache line. ${e.message}")
        }
    }

    fun getPlayerCacheLines(): List<String> {
        return try {
            query("SELECT data FROM playercache ORDER BY id ASC") { rs -> rs.getString("data") }
        } catch (e: Exception) {
            logger.err("Failed to load player cache lines. ${e.message}")
            emptyList()
        }
    }

    fun overwritePlayerCache(lines: List<String>) {
        try {
            execute("DELETE FROM playercache")
            lines.forEach { execute("INSERT INTO playercache (data) VALUES (?)", it) }
        } catch (e: Exception) {
            logger.err("Failed to overwrite player cache. ${e.message}")
        }
    }

    // ---------------------------------------------------------------------
    // Query helpers
    // ---------------------------------------------------------------------

    fun getPunishments(uuid: String, limit: Int? = null, offset: Int? = null): List<PunishmentData> {
        val supportsOrderAndLimit = dbType in setOf(
            DatabaseType.MYSQL,
            DatabaseType.MARIADB,
            DatabaseType.POSTGRESQL,
            DatabaseType.SQLITE
        )

        val params = mutableListOf<Any>(uuid)
        var sql = appendServerFilter("SELECT * FROM punishments WHERE uuid = ?", params, true)

        if (supportsOrderAndLimit && limit != null && offset != null) {
            sql += " ORDER BY start DESC LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return try {
            val rows = query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    uuid,
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator"),
                    rs.getString("server")
                )
            }
            val punishmentsToRemove = rows.filterNot { plugin.punishmentManager.isPunishmentActive(it) }
            punishmentsToRemove.forEach { removePunishment(it.uuid, it.type) }

            rows.filter { plugin.punishmentManager.isPunishmentActive(it) }
        } catch (e: Exception) {
            logger.err("Failed to get punishments for UUID: $uuid. ${e.message}")
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
                    rs.getInt("id"),
                    ip,
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator"),
                    rs.getString("server")
                )
            }
            rows.filter { plugin.punishmentManager.isPunishmentActive(it) }
        } catch (e: Exception) {
            logger.err("Failed to get punishments for IP: $ip. ${e.message}")
            emptyList()
        }
    }

    fun getPunishmentHistory(uuid: String, limit: Int? = null, offset: Int? = null): MutableList<PunishmentData> {
        val supportsOrderAndLimit = dbType in setOf(
            DatabaseType.MYSQL,
            DatabaseType.MARIADB,
            DatabaseType.POSTGRESQL,
            DatabaseType.SQLITE
        )

        val params = mutableListOf<Any>(uuid)
        var sql = appendServerFilter("SELECT * FROM punishmenthistory WHERE uuid = ?", params, true)
        if (supportsOrderAndLimit && limit != null && offset != null) {
            sql += " ORDER BY start DESC LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    uuid,
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator"),
                    rs.getString("server")
                )
            }.toMutableList()
        } catch (e: Exception) {
            logger.err("Failed to get punishment history for UUID: $uuid. ${e.message}")
            mutableListOf()
        }
    }

    fun getBannedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val supportsOrderAndLimit = dbType in setOf(
            DatabaseType.MYSQL,
            DatabaseType.MARIADB,
            DatabaseType.POSTGRESQL,
            DatabaseType.SQLITE
        )

        val params = mutableListOf<Any>()
        var sql = appendServerFilter(
            "SELECT * FROM punishments WHERE punishmentType IN ('BAN', 'BANIP')",
            params,
            true
        )
        if (supportsOrderAndLimit) {
            sql += " ORDER BY start DESC LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    rs.getString("uuid"),
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator"),
                    rs.getString("server")
                )
            }.toMutableList()
        } catch (e: Exception) {
            logger.err("Failed to get banned players: ${e.message}")
            mutableListOf()
        }
    }

    fun getHistoryBannedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val supportsOrderAndLimit = dbType in setOf(
            DatabaseType.MYSQL,
            DatabaseType.MARIADB,
            DatabaseType.POSTGRESQL,
            DatabaseType.SQLITE
        )

        val params = mutableListOf<Any>()
        var sql = appendServerFilter(
            "SELECT * FROM punishmenthistory WHERE punishmentType IN ('BAN', 'BANIP')",
            params,
            true
        )
        if (supportsOrderAndLimit) {
            sql += " ORDER BY start DESC LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    rs.getString("uuid"),
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator"),
                    rs.getString("server")
                )
            }.toMutableList()
        } catch (e: Exception) {
            logger.err("Failed to get banned players: ${e.message}")
            mutableListOf()
        }
    }

    fun getJailedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val supportsOrderAndLimit = dbType in setOf(
            DatabaseType.MYSQL,
            DatabaseType.MARIADB,
            DatabaseType.POSTGRESQL,
            DatabaseType.SQLITE
        )

        val params = mutableListOf<Any>()
        var sql = appendServerFilter(
            "SELECT * FROM punishments WHERE punishmentType = 'JAIL'",
            params,
            true
        )
        if (supportsOrderAndLimit) {
            sql += " ORDER BY start DESC LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return try {
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    rs.getString("uuid"),
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator"),
                    rs.getString("server")
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
            query(sql, *params.toTypedArray()) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count active warns: ${e.message}")
            0
        }
    }

    fun countAllPunishments(): Int {
        return try {
            val params = mutableListOf<Any>()
            val sql = appendServerFilter("SELECT COUNT(*) AS cnt FROM punishments", params, false)
            query(sql, *params.toTypedArray()) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count punishments. ${e.message}")
            0
        }
    }

    fun countAllPunishmentHistory(): Int {
        return try {
            val params = mutableListOf<Any>()
            val sql = appendServerFilter("SELECT COUNT(*) AS cnt FROM punishmenthistory", params, false)
            query(sql, *params.toTypedArray()) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
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
            count += query(punishSql, *punishParams.toTypedArray()) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
            val historyParams = mutableListOf<Any>(startOfDay)
            val historySql = appendServerFilter(
                "SELECT COUNT(*) AS cnt FROM punishmenthistory WHERE start >= ?",
                historyParams,
                true
            )
            count += query(historySql, *historyParams.toTypedArray()) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
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
            query(sql, *params.toTypedArray()) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
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
            sql += " ORDER BY start DESC LIMIT 10"
            query(sql, *params.toTypedArray()) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    uuid,
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator"),
                    rs.getString("server")
                )
            }
        } catch (e: Exception) {
            logger.err("Failed to get last ten punishments for UUID: $uuid. ${e.message}")
            emptyList()
        }
    }

    // ---------------------------------------------------------------------
    // Export & Import
    // ---------------------------------------------------------------------

    fun exportDatabase() {
        val tables = listOf("punishments", "punishmenthistory")
        try {
            val dumpDir = File(plugin.dataFolder, "dump").apply { mkdirs() }
            val writer = File(dumpDir, "backup.sql").bufferedWriter()

            tables.forEach { table ->
                val rows = query("SELECT * FROM $table") { rs ->
                    val meta = rs.metaData
                    val columnCount = meta.columnCount
                    val values = Array(columnCount) { i -> rs.getObject(i + 1) }
                    values
                }
                if (rows.isEmpty()) return@forEach

                writer.write("INSERT INTO $table VALUES\n")
                rows.forEachIndexed { index, row ->
                    if (index > 0) writer.write(",\n")
                    writer.write("(")
                    row.forEachIndexed { i, value ->
                        if (value == null) writer.write("NULL")
                        else writer.write("'" + value.toString().replace("'", "''") + "'")
                        if (i < row.size - 1) writer.write(", ")
                    }
                    writer.write(")")
                }
                writer.write(";\n")
            }
            writer.close()
            logger.success("Database exported to ${dumpDir}/backup.sql")
        } catch (e: Exception) {
            logger.err("Failed to export database. ${e.message}")
        }
    }




    fun importDatabase() {
        val filePath = File(plugin.dataFolder, "dump/backup.sql").absolutePath
        try {
            val lines = File(filePath).readLines()
            val sql = StringBuilder()
            for (line in lines) {
                sql.append(line)
                if (line.trim().endsWith(";")) {
                    execute(sql.toString())
                    sql.setLength(0)
                }
            }
            logger.success("Database imported from $filePath")
        } catch (e: IOException) {
            logger.err("Failed to read from file. ${e.message}")
        } catch (e: Exception) {
            logger.err("Failed to import database. ${e.message}")
        }
    }
    data class MigrationResult(val success: Boolean, val message: String)

    fun migrateDatabase(from: DatabaseType, to: DatabaseType): CompletableFuture<MigrationResult> {
        val future = CompletableFuture<MigrationResult>()
        if (!migrationInProgress.compareAndSet(false, true)) {
            future.complete(MigrationResult(false, "Another migration task is already running."))
            return future
        }

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                if (from != dbType) {
                    val message = "Migration aborted: configured database type is $dbType but received $from"
                    logger.err(message)
                    future.complete(MigrationResult(false, message))
                    return@Runnable
                }

                exportDatabase()
                val backupFile = File(plugin.dataFolder, "dump/backup.sql")
                if (!backupFile.exists()) {
                    val message = "Backup file not found. Migration aborted."
                    logger.err(message)
                    future.complete(MigrationResult(false, message))
                    return@Runnable
                }

                val configFile = File(plugin.dataFolder, "config.yml")
                val yaml = YamlConfiguration.loadConfiguration(configFile)
                val targetConfig = DatabaseConfig(
                    type = to,
                    host = yaml.getString("database.sql.host") ?: "localhost",
                    port = yaml.getInt("database.sql.port").takeIf { it != 0 } ?: 3306,
                    database = yaml.getString("database.sql.dbname") ?: plugin.name,
                    username = yaml.getString("database.sql.username") ?: "ROOT",
                    password = yaml.getString("database.sql.password") ?: "U5eV3ryStr0ngP4ssw0rd"
                )

                val targetDb = DatabaseManager(targetConfig, logger)
                try {
                    targetDb.connect()

                    val idDef = when (to) {
                        DatabaseType.SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT"
                        DatabaseType.POSTGRESQL -> "SERIAL PRIMARY KEY"
                        else -> "INT AUTO_INCREMENT PRIMARY KEY"
                    }
                    val tables = listOf("punishments", "punishmenthistory")
                    tables.forEach { table ->
                        try {
                            targetDb.execute("DROP TABLE IF EXISTS $table")
                        } catch (dropError: Exception) {
                            logger.warning("Unable to drop table '$table' before migration: ${dropError.message}")
                        }
                    }

                    val punishmentSchema = TableSchema(
                        "punishments",
                        listOf(
                            Column("id", idDef),
                            Column("name", "VARCHAR(32)"),
                            Column("uuid", "VARCHAR(36)"),
                            Column("reason", "VARCHAR(255)"),
                            Column("operator", "VARCHAR(16)"),
                            Column("punishmentType", "VARCHAR(16)"),
                            Column("start", "BIGINT"),
                            Column("endTime", "BIGINT"),
                            Column("server", "VARCHAR(64) DEFAULT '$NETWORK_SCOPE'")
                        )
                    )
                    val historySchema = punishmentSchema.copy(name = "punishmenthistory")
                    targetDb.createTable(punishmentSchema)
                    targetDb.createTable(historySchema)

                    val lines = backupFile.readLines()
                    val sql = StringBuilder()
                    for (line in lines) {
                        val trimmedLine = line.trim()
                        if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                            continue
                        }

                        sql.append(line)
                        if (trimmedLine.endsWith(";")) {
                            val statement = sql.toString().trim()
                            if (statement.isNotEmpty()) {
                                targetDb.execute(statement)
                            }
                            sql.setLength(0)
                        }
                    }

                    val configuredType = yaml.getString("database.type")?.uppercase()
                    if (configuredType != to.name) {
                        yaml.set("database.type", to.name.lowercase())
                        yaml.save(configFile)
                    }

                    logger.success("Database migrated from $from to $to")
                    future.complete(MigrationResult(true, "Database migrated from ${from.name.lowercase()} to ${to.name.lowercase()}"))

                    plugin.server.scheduler.runTask(plugin, Runnable {
                        plugin.onReload()
                    })
                } catch (e: Exception) {
                    val message = "Failed to migrate database. ${e.message}"
                    logger.err(message)
                    future.complete(MigrationResult(false, message))
                    return@Runnable
                } finally {
                    try {
                        targetDb.close()
                    } catch (closeError: Exception) {
                        logger.warning("Failed to close target database: ${closeError.message}")
                    }
                }
            } catch (e: Exception) {
                val message = "Failed to migrate database. ${e.message}"
                logger.err(message)
                future.complete(MigrationResult(false, message))
                return@Runnable
            } finally {
                migrationInProgress.set(false)
            }
        })

        return future
    }
}
