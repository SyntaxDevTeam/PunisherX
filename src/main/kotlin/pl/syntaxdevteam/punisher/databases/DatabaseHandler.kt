package pl.syntaxdevteam.punisher.databases

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.FileConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.sql.*

class DatabaseHandler(private val plugin: PunisherX, private val config: FileConfiguration) {
    private var dataSource: HikariDataSource? = null
    private var logger = plugin.logger
    private val dbType = config.getString("database.type")?.lowercase() ?: "sqlite"

    init {
        setupDataSource()
    }

    private fun setupDataSource() {
        val hikariConfig = HikariConfig()
        val dbName = config.getString("database.sql.dbname") ?: plugin.name
        val user = config.getString("database.sql.username") ?: "ROOT"
        val password = config.getString("database.sql.password") ?: "V3ryU5eStr0ngP4ssw0rd"
        when (dbType) {
            "mysql", "mariadb" -> {
                hikariConfig.jdbcUrl =
                    "jdbc:mysql://${config.getString("database.sql.host")}:${config.getString("database.sql.port")}/$dbName"
                hikariConfig.username = user
                hikariConfig.password = password
                hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"
            }

            "postgresql" -> {
                hikariConfig.jdbcUrl =
                    "jdbc:postgresql://${config.getString("database.sql.host")}:${config.getString("database.sql.port")}/$dbName"
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

        hikariConfig.maximumPoolSize = 10
        hikariConfig.minimumIdle = 2
        hikariConfig.connectionTimeout = 30000

        dataSource = HikariDataSource(hikariConfig)
        logger.debug("Connection to the ${dbType.uppercase()} database established")
    }

    fun openConnection() {
        if (dataSource == null) {
            setupDataSource()
        }
    }

    fun closeConnection() {
        try {
            dataSource?.close()
            logger.info("Connection to the database closed.")
        } catch (e: SQLException) {
            logger.err("Failed to close the connection. ${e.message}")
        }
    }

    private fun getConnection(): Connection? {
        return try {
            logger.debug("Database connection established")
            dataSource?.connection
        } catch (e: SQLException) {
            logger.err("Failed to get connection. ${e.message}")
            null
        }
    }

    fun createTables() {
        getConnection()?.use { conn ->
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
                        start INTEGER,
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
                        start INTEGER,
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
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to add punishment history for player $name. ${e.message}")
        }
    }

    fun getPunishments(uuid: String): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        try {
            getConnection()?.use { conn ->
                val query = "SELECT * FROM punishments WHERE uuid = ?"
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, uuid)
                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    while (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val type = resultSet.getString("punishmentType")
                        val reason = resultSet.getString("reason")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val punishment = PunishmentData(id, uuid, type, reason, start, end)
                        if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                            punishments.add(punishment)
                        } else {
                            removePunishment(uuid, type)
                        }
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
                        val punishment = PunishmentData(id, ip, type, reason, start, end)
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
                    if (!removeAll && (config.getString("database.type")?.lowercase() == "postgresql" || config.getString("database.type")?.lowercase() == "h2" || config.getString("database.type")?.lowercase() == "sqlite")) {
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
                        val punishment = PunishmentData(id, uuid, type, reason, start, end)
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

    fun getPunishmentHistory(uuid: String, limit: Int, offset: Int): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        try {
            getConnection()?.use { conn ->
                val query = "SELECT * FROM punishmentHistory WHERE uuid = ? ORDER BY start DESC LIMIT ? OFFSET ?"
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, uuid)
                    preparedStatement.setInt(2, limit)
                    preparedStatement.setInt(3, offset)
                    val resultSet: ResultSet = preparedStatement.executeQuery()
                    while (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val type = resultSet.getString("punishmentType")
                        val reason = resultSet.getString("reason")
                        val start = resultSet.getLong("start")
                        val end = resultSet.getLong("endTime")
                        val punishment = PunishmentData(id, uuid, type, reason, start, end)
                        punishments.add(punishment)
                    }
                    resultSet.close()
                    preparedStatement.close()
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishment history for UUID: $uuid. ${e.message}")
        }
        return punishments
    }

    fun getLastTenPunishments(uuid: String): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        try {
            getConnection()?.use { conn ->
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
                        val punishment = PunishmentData(id, uuid, type, reason, start, end)
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