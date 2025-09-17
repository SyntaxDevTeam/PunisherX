package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.core.database.*
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

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


    private val dbType: DatabaseType = plugin.config
        .getString("database.type")
        ?.uppercase()
        ?.let { DatabaseType.valueOf(it) }
        ?: DatabaseType.SQLITE
    private val config = DatabaseConfig(
        type = dbType,
        host = plugin.config.getString("database.sql.host") ?: "localhost",
        port = plugin.config.getInt("database.sql.port").takeIf { it != 0 } ?: 3306,
        database = plugin.config.getString("database.sql.dbname") ?: plugin.name,
        username = plugin.config.getString("database.sql.username") ?: "ROOT",
        password = plugin.config.getString("database.sql.password") ?: "U5eV3ryStr0ngP4ssw0rd"
    )
    private val db = DatabaseManager(config, logger)
    /** Opens connection pool. */
    fun openConnection() {
        db.connect()
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

    /** Returns database specific definition for auto increment column. */
    private fun idDefinition(): String = when (dbType) {
        DatabaseType.SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT"
        DatabaseType.POSTGRESQL -> "SERIAL PRIMARY KEY"
        else -> "INT AUTO_INCREMENT PRIMARY KEY"
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
                Column("endTime", "BIGINT")
            )
        )

        val historySchema = punishmentSchema.copy(name = "punishmenthistory")

        db.createTable(punishmentSchema)
        db.createTable(historySchema)

        val playerCacheSchema = TableSchema(
            "playercache",
            listOf(
                Column("id", idDefinition()),
                Column("data", "TEXT")
            )
        )
        db.createTable(playerCacheSchema)
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
    ): Boolean {
        return try {
            execute(
                """
                INSERT INTO punishments (name, uuid, reason, operator, punishmentType, start, endTime)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                name, uuid, reason, operator, punishmentType, start, end
            )
            plugin.discordWebhook.sendPunishmentWebhook(
                playerName = name,
                adminName = operator,
                reason = reason,
                type = punishmentType,
                duration = end
            )
            true
        } catch (e: Exception) {
            logger.err("Failed to add punishment for player $name. ${e.message}")
            false
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
                INSERT INTO punishmenthistory (name, uuid, reason, operator, punishmentType, start, endTime)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                name, uuid, reason, operator, punishmentType, start, end
            )
        } catch (e: Exception) {
            logger.err("Failed to add punishment history for player $name. ${e.message}")
        }
    }

    fun removePunishment(uuidOrIp: String, punishmentType: String, removeAll: Boolean = false) {
        val base = "DELETE FROM punishments WHERE uuid = ? AND punishmentType = ?"
        val query = if (removeAll) {
            base
        } else {
            when (dbType) {
                DatabaseType.POSTGRESQL, DatabaseType.H2, DatabaseType.SQLITE ->
                    "$base AND start = (SELECT start FROM punishments WHERE uuid = ? AND punishmentType = ? ORDER BY start DESC LIMIT 1)"
                else -> "$base ORDER BY start DESC LIMIT 1"
            }
        }

        try {
            val params = mutableListOf<Any>(uuidOrIp, punishmentType)
            if (!removeAll && (dbType == DatabaseType.POSTGRESQL || dbType == DatabaseType.H2 || dbType == DatabaseType.SQLITE)) {
                params.add(uuidOrIp)
                params.add(punishmentType)
            }
            execute(query, *params.toTypedArray())
        } catch (e: Exception) {
            logger.err("Failed to remove punishment of type $punishmentType for $uuidOrIp. ${e.message}")
        }
    }

    fun deletePlayerData(uuid: String) {
        try {
            execute("DELETE FROM punishments WHERE uuid = ?", uuid)
            execute("DELETE FROM punishmenthistory WHERE uuid = ?", uuid)
        } catch (e: Exception) {
            logger.err("Failed to delete player data. ${e.message}")
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
            query("SELECT data FROM playercache") { rs -> rs.getString("data") }
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

        var sql = "SELECT * FROM punishments WHERE uuid = ?"
        val params = mutableListOf<Any>(uuid)

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
                    rs.getString("operator")
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
            val rows = query(
                "SELECT * FROM punishments WHERE uuid = ?",
                ip
            ) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    ip,
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator")
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

        var sql = "SELECT * FROM punishmenthistory WHERE uuid = ?"
        val params = mutableListOf<Any>(uuid)
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
                    rs.getString("operator")
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

        var sql = "SELECT * FROM punishments WHERE punishmentType IN ('BAN', 'BANIP')"
        val params = mutableListOf<Any>()
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
                    rs.getString("operator")
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

        var sql = "SELECT * FROM punishmenthistory WHERE punishmentType IN ('BAN', 'BANIP')"
        val params = mutableListOf<Any>()
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
                    rs.getString("operator")
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

        var sql = "SELECT * FROM punishments WHERE punishmentType = 'JAIL'"
        val params = mutableListOf<Any>()
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
                    rs.getString("operator")
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
            query(
                "SELECT COUNT(*) AS cnt FROM punishments WHERE uuid = ? AND punishmentType = 'WARN' AND (endTime = -1 OR endTime > ?)",
                uuid, currentTime
            ) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count active warns: ${e.message}")
            0
        }
    }

    fun countAllPunishments(): Int {
        return try {
            query("SELECT COUNT(*) AS cnt FROM punishments") { rs -> rs.getInt("cnt") }
                .firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count punishments. ${e.message}")
            0
        }
    }

    fun countAllPunishmentHistory(): Int {
        return try {
            query("SELECT COUNT(*) AS cnt FROM punishmenthistory") { rs -> rs.getInt("cnt") }
                .firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count punishment history. ${e.message}")
            0
        }
    }

    fun countTodayPunishments(): Int {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return try {
            var count = 0
            count += query(
                "SELECT COUNT(*) AS cnt FROM punishments WHERE start >= ?",
                startOfDay
            ) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
            count += query(
                "SELECT COUNT(*) AS cnt FROM punishmenthistory WHERE start >= ?",
                startOfDay
            ) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
            count
        } catch (e: Exception) {
            logger.err("Failed to count today's punishments. ${e.message}")
            0
        }
    }

    fun countPlayerAllPunishmentHistory(uuid: UUID): Int {
        return try {
            query(
                "SELECT COUNT(*) AS cnt FROM punishmenthistory WHERE uuid = ?",
                uuid.toString()
            ) { rs -> rs.getInt("cnt") }.firstOrNull() ?: 0
        } catch (e: Exception) {
            logger.err("Failed to count punishment history. ${e.message}")
            0
        }
    }

    fun getLastTenPunishments(uuid: String): List<PunishmentData> {
        return try {
            query(
                "SELECT * FROM punishmenthistory WHERE uuid = ? ORDER BY start DESC LIMIT 10",
                uuid
            ) { rs ->
                PunishmentData(
                    rs.getInt("id"),
                    uuid,
                    rs.getString("punishmentType"),
                    rs.getString("reason"),
                    rs.getLong("start"),
                    rs.getLong("endTime"),
                    rs.getString("name"),
                    rs.getString("operator")
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


    fun migrateDatabase(from: DatabaseType, to: DatabaseType) {
        if (from != dbType) {
            logger.err("Migration aborted: configured database type is $dbType but received $from")
            return
        }

        exportDatabase()
        val backupFile = File(plugin.dataFolder, "dump/backup.sql")
        if (!backupFile.exists()) {
            logger.err("Backup file not found. Migration aborted.")
            return
        }

        val targetConfig = DatabaseConfig(
            type = to,
            host = plugin.config.getString("database.sql.host") ?: "localhost",
            port = plugin.config.getInt("database.sql.port").takeIf { it != 0 } ?: 3306,
            database = plugin.config.getString("database.sql.dbname") ?: plugin.name,
            username = plugin.config.getString("database.sql.username") ?: "ROOT",
            password = plugin.config.getString("database.sql.password") ?: "U5eV3ryStr0ngP4ssw0rd"
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
                    Column("endTime", "BIGINT")
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

            logger.success("Database migrated from $from to $to")
        } catch (e: Exception) {
            logger.err("Failed to migrate database. ${e.message}")
        } finally {
            targetDb.close()
        }
    }
}