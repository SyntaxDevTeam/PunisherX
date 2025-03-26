package pl.syntaxdevteam.punisher.databases

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.sql.*

class DatabaseHandler(private val plugin: PunisherX) {
    private var dataSource: HikariDataSource? = null
    private var logger = plugin.logger
    private val dbType = plugin.config.getString("database.type")?.lowercase() ?: "sqlite"

    init {
        setupDataSource()
    }

    private fun setupDataSource() {
        val hikariConfig = HikariConfig()
        val dbName = plugin.config.getString("database.sql.dbname") ?: plugin.name
        val user = plugin.config.getString("database.sql.username") ?: "ROOT"
        val password = plugin.config.getString("database.sql.password") ?: "U5eV3ryStr0ngP4ssw0rd"
        when (dbType) {
            "mysql", "mariadb" -> {
                hikariConfig.jdbcUrl =
                    "jdbc:mysql://${plugin.config.getString("database.sql.host")}:${plugin.config.getString("database.sql.port")}/$dbName"
                hikariConfig.username = user
                hikariConfig.password = password
                hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"
            }

            "postgresql" -> {
                hikariConfig.jdbcUrl =
                    "jdbc:postgresql://${plugin.config.getString("database.sql.host")}:${plugin.config.getString("database.sql.port")}/$dbName"
                hikariConfig.username = user
                hikariConfig.password = password
                hikariConfig.driverClassName = "org.postgresql.Driver"
            }

            "sqlite" -> {
                hikariConfig.jdbcUrl = "jdbc:sqlite:${plugin.dataFolder}/$dbName.db"
                hikariConfig.driverClassName = "org.sqlite.JDBC"
            }

            "h2" -> {
                hikariConfig.jdbcUrl = "jdbc:h2:./${plugin.dataFolder}/$dbName"
                hikariConfig.username = user
                hikariConfig.password = password
                hikariConfig.driverClassName = "org.h2.Driver"
            }

            else -> throw IllegalArgumentException("Unsupported database type: $dbType")
        }
        if (dbType == "sqlite") {
            hikariConfig.maximumPoolSize = 2
            hikariConfig.minimumIdle = 1
            hikariConfig.connectionTimeout = 30000
            hikariConfig.idleTimeout = 10000
            hikariConfig.maxLifetime = 60000
            hikariConfig.keepaliveTime = 30000
        } else {
            hikariConfig.maximumPoolSize = 10
            hikariConfig.minimumIdle = 2
            hikariConfig.connectionTimeout = 30000
            hikariConfig.idleTimeout = 600000
            hikariConfig.maxLifetime = 1800000
            hikariConfig.keepaliveTime = 900000
            hikariConfig.leakDetectionThreshold = 2000
        }

        logger.debug("Setting up data source for database type: $dbType")
        try {
            dataSource = HikariDataSource(hikariConfig)
            logger.debug("HikariCP data source initialized successfully for $dbType.")
        } catch (e: Exception) {
            logger.err("Failed to initialize HikariCP data source: ${e.message}")
            throw e
        }
    }

    fun openConnection() {
        if (dataSource == null) {
            setupDataSource()
        }
    }

    fun closeConnection() {
        try {
            dataSource?.close()
            logger.info("HikariCP pool shut down. Total=${dataSource?.hikariPoolMXBean?.totalConnections}, Active=${dataSource?.hikariPoolMXBean?.activeConnections}, Idle=${dataSource?.hikariPoolMXBean?.idleConnections}")
        } catch (e: SQLException) {
            logger.err("Error while closing HikariCP pool: ${e.message}")
        }
    }

    private fun getConnection(): Connection? {
        return try {
            val connection = dataSource?.connection
            if (connection != null && dbType == "sqlite") {
                enableSQLiteWAL(connection)
            }
            logger.debug("Connection obtained: Active=${dataSource?.hikariPoolMXBean?.activeConnections}, Idle=${dataSource?.hikariPoolMXBean?.idleConnections}")
            connection
        } catch (e: SQLException) {
            logger.err("Failed to get connection. ${e.message}")
            null
        }
    }

    private fun enableSQLiteWAL(connection: Connection) {
        logger.debug("SQLite connection detected! I'm enabling WAL mode")
        try {
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=WAL;")
                logger.debug("SQLite WAL mode enabled.")
            }
        } catch (e: SQLException) {
            logger.err("Failed to enable SQLite WAL mode. ${e.message}")
        }
    }

    fun createTables() {
        getConnection()?.use { conn ->
            logger.debug("Database connection established from createTables")
            conn.createStatement().use { statement ->
                try {
                    val createTablesPunishments = when (dbType) {
                        "sqlite" -> """
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT,
                        uuid TEXT,
                        reason TEXT,
                        operator TEXT,
                        punishmentType TEXT,
                        start TEXT,
                        endTime TEXT
                    );
                """.trimIndent()

                        "postgresql" -> """ 
                    CREATE TABLE IF NOT EXISTS punishments ( 
                        id SERIAL PRIMARY KEY, 
                        name VARCHAR(32), 
                        uuid VARCHAR(36), 
                        reason VARCHAR(255), 
                        operator VARCHAR(16), 
                        punishmentType VARCHAR(16), 
                        start BIGINT, 
                        endTime VARCHAR(32) 
                    ); 
                """.trimIndent()

                        "h2" -> """
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(32),
                        uuid VARCHAR(36),
                        reason VARCHAR(255),
                        operator VARCHAR(16),
                        punishmentType VARCHAR(16),
                        start BIGINT,
                        endTime VARCHAR(32)
                    );
                """.trimIndent()

                        else -> """
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(32),
                        uuid VARCHAR(36),
                        reason VARCHAR(255),
                        operator VARCHAR(16),
                        punishmentType VARCHAR(16),
                        start BIGINT,
                        endTime VARCHAR(32)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
                """.trimIndent()
                    }
                    statement.executeUpdate(createTablesPunishments)
                    logger.debug("Operacje na tabeli 'punishments' ukończone.")

                    val createTablesPunishmenthistory = when (dbType) {
                        "sqlite" -> """
                    CREATE TABLE IF NOT EXISTS punishmenthistory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT,
                        uuid TEXT,
                        reason TEXT,
                        operator TEXT,
                        punishmentType TEXT,
                        start TEXT,
                        endTime TEXT
                    );
                """.trimIndent()

                        "postgresql" -> """
                    CREATE TABLE IF NOT EXISTS punishmenthistory (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(32),
                        uuid VARCHAR(36),
                        reason VARCHAR(255),
                        operator VARCHAR(16),
                        punishmentType VARCHAR(16),
                        start BIGINT,
                        endTime VARCHAR(32)
                    );
                """.trimIndent()

                        "h2" -> """
                    CREATE TABLE IF NOT EXISTS punishmenthistory (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(32),
                        uuid VARCHAR(36),
                        reason VARCHAR(255),
                        operator VARCHAR(16),
                        punishmentType VARCHAR(16),
                        start BIGINT,
                        endTime VARCHAR(32)
                    );
                """.trimIndent()

                        else -> """
                    CREATE TABLE IF NOT EXISTS punishmenthistory (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(32),
                        uuid VARCHAR(36),
                        reason VARCHAR(255),
                        operator VARCHAR(16),
                        punishmentType VARCHAR(16),
                        start BIGINT,
                        endTime VARCHAR(32)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
                """.trimIndent()
                    }
                    statement.executeUpdate(createTablesPunishmenthistory)
                    logger.debug("Operacje na tabeli 'punishmenthistory' ukończone.")
                } catch (ex: SQLException) {
                    logger.err("Błąd podczas tworzenia tabel: ${ex.message}")
                }
            }
        } ?: logger.err("Brak połączenia z bazą danych.")
        logger.debug("Operacje tworzenia tabel zakończone.")
    }

    fun addPunishment(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long): Boolean {
        return try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from addPunishment")
                val query = """
                INSERT INTO punishments (name, uuid, reason, operator, punishmentType, start, endTime)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, name)
                    preparedStatement.setString(2, uuid)
                    preparedStatement.setString(3, reason)
                    preparedStatement.setString(4, operator)
                    preparedStatement.setString(5, punishmentType)
                    preparedStatement.setLong(6, start)
                    preparedStatement.setLong(7, end)
                    preparedStatement.executeUpdate()
                    plugin.logger.debug("Punishment for player $name added to the database.")
                    true
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to add punishment for player $name. ${e.message}")
            false
        }
    }

    fun addPunishmentHistory(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long) {
        try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from addPunishmentHistory")
                val query = """
                INSERT INTO punishmenthistory (name, uuid, reason, operator, punishmentType, start, endTime)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, name)
                    preparedStatement.setString(2, uuid)
                    preparedStatement.setString(3, reason)
                    preparedStatement.setString(4, operator)
                    preparedStatement.setString(5, punishmentType)
                    preparedStatement.setLong(6, start)
                    preparedStatement.setLong(7, end)
                    preparedStatement.executeUpdate()
                    plugin.logger.debug("Punishment history for player $name added to the database.")
                    plugin.discordWebhook.sendPunishmentWebhook(
                        playerName = name,
                        adminName = operator,
                        reason = reason,
                        type = punishmentType,
                        duration = end
                    )
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to add punishment history for player $name. ${e.message}")
        }
    }

    fun getPunishments(uuid: String, limit: Int? = null, offset: Int? = null): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        logger.debug("Database connection established from getPunishments")
        try {
            getConnection()?.use { conn ->
                val supportsOrderAndLimit = when (dbType.lowercase()) {
                    "mysql", "mariadb", "postgresql", "sqlite" -> true
                    "h2" -> false
                    else -> false
                }

                val query = when {
                    supportsOrderAndLimit && limit != null && offset != null -> """
                        SELECT * FROM punishments
                        WHERE uuid = ?
                        ORDER BY start DESC
                        LIMIT ? OFFSET ?
                    """.trimIndent()
                    else -> """
                        SELECT * FROM punishments
                        WHERE uuid = ?
                    """.trimIndent()
                }

                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, uuid)
                    if (supportsOrderAndLimit && limit != null && offset != null) {
                        preparedStatement.setInt(2, limit)
                        preparedStatement.setInt(3, offset)
                    }
                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    val punishmentsToRemove = mutableListOf<Pair<String, String>>()
                    while (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val type = resultSet.getString("punishmentType")
                        val reason = resultSet.getString("reason")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val name = resultSet.getString("name")
                        val operator = resultSet.getString("operator")
                        val punishment = PunishmentData(id, uuid, type, reason, start, end, name, operator)

                        if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                            punishments.add(punishment)
                        } else {
                            punishmentsToRemove.add(uuid to type)
                        }
                    }
                    punishmentsToRemove.forEach { (uuid, type) ->
                        removePunishment(uuid, type)
                    }
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishments for UUID: $uuid. ${e.message}")
        }
        return punishments
    }

    fun getPunishmentsByIP(ip: String): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        try {
            getConnection()?.use { conn ->
            logger.debug("Database connection established from getPunishmentsByIP")
            val query = "SELECT * FROM punishments WHERE uuid = ?"
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, ip)
                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    while (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val type = resultSet.getString("punishmentType")
                        val reason = resultSet.getString("reason")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val name = resultSet.getString("name")
                        val operator = resultSet.getString("operator")
                        val punishment = PunishmentData(id, ip, type, reason, start, end, name, operator)
                        if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                            punishments.add(punishment)
                        } else {
                            removePunishment(ip, type)
                        }
                    }
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishments for IP: $ip. ${e.message}")
        }
        return punishments
    }

    fun removePunishment(uuidOrIp: String, punishmentType: String, removeAll: Boolean = false) {
        try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from removePunishment")
                val query = when (dbType) {
                    "postgresql", "h2", "sqlite" -> if (removeAll) {
                        """
                    DELETE FROM punishments 
                    WHERE uuid = ? AND punishmentType = ?
                    """.trimIndent()
                    } else {
                        """
                    DELETE FROM punishments 
                    WHERE uuid = ? AND punishmentType = ? 
                    AND start = (SELECT start FROM punishments WHERE uuid = ? AND punishmentType = ? ORDER BY start DESC LIMIT 1)
                    """.trimIndent()
                    }
                    else -> if (removeAll) {
                        """
                    DELETE FROM punishments 
                    WHERE uuid = ? AND punishmentType = ?
                    """.trimIndent()
                    } else {
                        """
                    DELETE FROM punishments 
                    WHERE uuid = ? AND punishmentType = ?
                    ORDER BY start DESC
                    LIMIT 1
                    """.trimIndent()
                    }
                }
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, uuidOrIp)
                    preparedStatement.setString(2, punishmentType)
                    if (!removeAll && (plugin.config.getString("database.type")?.lowercase() == "postgresql" || plugin.config.getString("database.type")?.lowercase() == "h2" || plugin.config.getString("database.type")?.lowercase() == "sqlite")) {
                        preparedStatement.setString(3, uuidOrIp)
                        preparedStatement.setString(4, punishmentType)
                    }
                    val rowsAffected = preparedStatement.executeUpdate()
                    if (rowsAffected > 0) {
                        plugin.logger.debug("Punishment of type $punishmentType for UUID/IP: $uuidOrIp removed from the database.")
                    } else {
                        plugin.logger.warning("No punishment of type $punishmentType found for UUID/IP: $uuidOrIp.")
                    }
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to remove punishment of type $punishmentType for UUID/IP: $uuidOrIp. ${e.message}")
        }
    }

    fun getActiveWarnCount(uuid: String): Int {
        val punishments = mutableListOf<PunishmentData>()
        try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from getActiveWarnCount")
                val query = "SELECT * FROM punishments WHERE uuid = ? AND punishmentType = 'WARN'"
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, uuid)
                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    while (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val type = resultSet.getString("punishmentType")
                        val reason = resultSet.getString("reason")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val name = resultSet.getString("name")
                        val operator = resultSet.getString("operator")
                        val punishment = PunishmentData(id, uuid, type, reason, start, end, name, operator)
                        if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                            punishments.add(punishment)
                        }
                    }
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get active warn count for UUID: $uuid. ${e.message}")
        }
        return punishments.size
    }

    fun getPunishmentHistory(uuid: String, limit: Int? = null, offset: Int? = null): List<PunishmentData> {
    val punishments = mutableListOf<PunishmentData>()
    logger.debug("Database connection established from getPunishmentHistory")
    try {
        getConnection()?.use { conn ->
            val supportsOrderAndLimit = when (dbType.lowercase()) {
                "mysql", "mariadb", "postgresql", "sqlite" -> true
                "h2" -> false
                else -> false
            }

            val query = when {
                supportsOrderAndLimit && limit != null && offset != null -> """
                    SELECT * FROM punishmentHistory
                    WHERE uuid = ?
                    ORDER BY start DESC
                    LIMIT ? OFFSET ?
                """.trimIndent()
                else -> """
                    SELECT * FROM punishmentHistory
                    WHERE uuid = ?
                """.trimIndent()
            }

            conn.prepareStatement(query).use { preparedStatement ->
                preparedStatement.setString(1, uuid)
                if (supportsOrderAndLimit && limit != null && offset != null) {
                    preparedStatement.setInt(2, limit)
                    preparedStatement.setInt(3, offset)
                }
                val resultSet: ResultSet = preparedStatement.executeQuery()
                while (resultSet.next()) {
                    val id = resultSet.getInt("id")
                    val type = resultSet.getString("punishmentType")
                    val reason = resultSet.getString("reason")
                    val start = resultSet.getLong("start")
                    val end = resultSet.getLong("endTime")
                    val name = resultSet.getString("name")
                    val operator = resultSet.getString("operator")
                    val punishment = PunishmentData(id, uuid, type, reason, start, end, name, operator)
                    punishments.add(punishment)
                }
                resultSet.close()
            }
        } ?: throw SQLException("No connection available")
    } catch (e: SQLException) {
        plugin.logger.err("Failed to get punishment history for UUID: $uuid. ${e.message}")
    }
        return punishments
    }

    /**
     * Retrieves the last ten punishment records for a given player.
     *
     * @param uuid The UUID of the player.
     * @return A list of PunishmentData objects representing the player's last ten punishments.
     */
    @Deprecated("This method is deprecated and will be removed in future releases. Use getPunishment or getPunishmentHistory instead.")
    @Suppress("unused")
    fun getLastTenPunishments(uuid: String): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from getLastTenPunishments")
                val query = "SELECT * FROM punishmenthistory WHERE uuid = ? ORDER BY start DESC LIMIT 10"
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, uuid)
                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    while (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val type = resultSet.getString("punishmentType")
                        val reason = resultSet.getString("reason")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val name = resultSet.getString("name")
                        val operator = resultSet.getString("operator")
                        val punishment = PunishmentData(id, uuid, type, reason, start, end, name, operator)
                        punishments.add(punishment)
                    }
                    resultSet.close()
                    preparedStatement.close()
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get last ten punishments for UUID: $uuid. ${e.message}")
        }
        return punishments
    }

    fun updatePunishmentReason(id: Int, newReason: String): Boolean {
        return try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from updatePunishmentReason")
                val query = "UPDATE punishmentHistory SET reason = ? WHERE id = ?"
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, newReason)
                    preparedStatement.setInt(2, id)
                    val rowsUpdated = preparedStatement.executeUpdate()
                    preparedStatement.close()
                    rowsUpdated > 0
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to update punishment reason for ID: $id. ${e.message}")
            false
        }
    }

    fun getBannedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val bannedPlayers = mutableListOf<PunishmentData>()

        try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from getBannedPlayers")

                val supportsOrderAndLimit = when (dbType.lowercase()) {
                    "mysql", "mariadb", "postgresql", "sqlite" -> true
                    "h2" -> false
                    else -> false
                }

                val query = when {
                    supportsOrderAndLimit -> """
                    SELECT * FROM punishments 
                    WHERE punishmentType IN ('BAN', 'BANIP') 
                    ORDER BY start DESC 
                    LIMIT ? OFFSET ?
                """.trimIndent()

                    else -> """
                    SELECT * FROM punishments 
                    WHERE punishmentType IN ('BAN', 'BANIP')
                """.trimIndent()
                }

                conn.prepareStatement(query).use { preparedStatement ->
                    if (supportsOrderAndLimit) {
                        preparedStatement.setInt(1, limit)
                        preparedStatement.setInt(2, offset)
                    }

                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    var skipped = 0
                    while (resultSet.next()) {
                        if (!supportsOrderAndLimit && skipped < offset) {
                            skipped++
                            continue
                        }
                        if (!supportsOrderAndLimit && bannedPlayers.size >= limit) break
                        val id = resultSet.getInt("id")
                        val name = resultSet.getString("name")
                        val uuid = resultSet.getString("uuid")
                        val reason = resultSet.getString("reason")
                        val operator = resultSet.getString("operator")
                        val punishmentType = resultSet.getString("punishmentType")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val punishment = PunishmentData(id, uuid, punishmentType, reason, start, end, name, operator)
                        bannedPlayers.add(punishment)
                    }
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get banned players: ${e.message}")
        } catch (e: IllegalArgumentException) {
            plugin.logger.err("Unsupported database type: $dbType")
        }

        return bannedPlayers
    }

    fun getHistoryBannedPlayers(limit: Int, offset: Int): MutableList<PunishmentData> {
        val bannedPlayers = mutableListOf<PunishmentData>()

        try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from getHistoryBannedPlayers")

                val supportsOrderAndLimit = when (dbType.lowercase()) {
                    "mysql", "mariadb", "postgresql", "sqlite" -> true
                    "h2" -> false
                    else -> false
                }

                val query = when {
                    supportsOrderAndLimit -> """
                    SELECT * FROM punishments 
                    WHERE punishmentType IN ('BAN', 'BANIP') 
                    ORDER BY start DESC 
                    LIMIT ? OFFSET ?
                """.trimIndent()

                    else -> """
                    SELECT * FROM punishments 
                    WHERE punishmentType IN ('BAN', 'BANIP')
                """.trimIndent()
                }

                conn.prepareStatement(query).use { preparedStatement ->
                    if (supportsOrderAndLimit) {
                        preparedStatement.setInt(1, limit)
                        preparedStatement.setInt(2, offset)
                    }

                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    var skipped = 0
                    while (resultSet.next()) {
                        if (!supportsOrderAndLimit && skipped < offset) {
                            skipped++
                            continue
                        }
                        if (!supportsOrderAndLimit && bannedPlayers.size >= limit) break
                        val id = resultSet.getInt("id")
                        val name = resultSet.getString("name")
                        val uuid = resultSet.getString("uuid")
                        val reason = resultSet.getString("reason")
                        val operator = resultSet.getString("operator")
                        val punishmentType = resultSet.getString("punishmentType")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val punishment = PunishmentData(id, uuid, punishmentType, reason, start, end, name, operator)
                        bannedPlayers.add(punishment)
                    }
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get banned players: ${e.message}")
        } catch (e: IllegalArgumentException) {
            plugin.logger.err("Unsupported database type: $dbType")
        }

        return bannedPlayers
    }

    /**
     * Exports the database to a SQL dump file.
     *
     * This method retrieves the data from the database tables and writes it to a SQL dump file.
     * The dump file contains valid SQL statements to recreate the tables and insert the data.
     *
     * The dump file is saved in the `dump` directory inside the plugin's data folder.
     */
    fun exportDatabase() {
        val tables = listOf("punishments", "punishmenthistory")
        try {
            getConnection()?.use { conn ->
                val dumpDir = File(plugin.dataFolder, "dump")
                if (!dumpDir.exists()) {
                    dumpDir.mkdirs()
                }
                val writer = File(dumpDir, "backup.sql").bufferedWriter()
                for (table in tables) {
                    val resultSet = conn.createStatement().executeQuery("SELECT * FROM $table")
                    val metaData = resultSet.metaData
                    val columnCount = metaData.columnCount

                    if (!resultSet.isBeforeFirst) {
                        continue
                    }

                    writer.write("INSERT INTO $table VALUES\n")
                    var first = true
                    while (resultSet.next()) {
                        if (!first) {
                            writer.write(",\n")
                        }
                        first = false
                        writer.write("(")
                        for (i in 1..columnCount) {
                            val value = resultSet.getObject(i)
                            if (value == null) {
                                writer.write("NULL")
                            } else {
                                writer.write("'${value.toString().replace("'", "''")}'")
                            }
                            if (i < columnCount) writer.write(", ")
                        }
                        writer.write(")")
                    }
                    writer.write(";\n")
                }
                writer.close()
                plugin.logger.success("Database exported to ${dumpDir}/backup.sql")
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to export database. ${e.message}")
        } catch (e: IOException) {
            plugin.logger.err("Failed to write to file. ${e.message}")
        }
    }

    /**
     * Imports the database from a SQL dump file.
     *
     * This method reads the SQL dump file line by line and executes the SQL statements to recreate
     * the database tables and insert the data. The dump file must contain valid SQL statements
     * separated by semicolons.
     */
    fun importDatabase() {
        val filePath = File(plugin.dataFolder, "dump/backup.sql").absolutePath
        try {
            getConnection()?.use { conn ->
                val lines = File(filePath).readLines()
                val statement = conn.createStatement()
                val sql = StringBuilder()
                for (line in lines) {
                    sql.append(line)
                    if (line.trim().endsWith(";")) {
                        statement.execute(sql.toString())
                        sql.setLength(0)
                    }
                }
                plugin.logger.success("Database imported from $filePath")
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to import database. ${e.message}")
        } catch (e: IOException) {
            plugin.logger.err("Failed to read from file. ${e.message}")
        }
    }
}