package pl.syntaxdevteam.databases

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.ResultSet
import pl.syntaxdevteam.PunisherX
import java.io.File
import java.io.IOException

class SQLiteDatabaseHandler(private val plugin: PunisherX) : DatabaseHandler {
    private var connection: Connection? = null
    private val dbFile: File = File(plugin.dataFolder, "database.db")

    override fun openConnection() {
        try {
            if (!dbFile.exists()) {
                dbFile.parentFile.mkdirs()
                dbFile.createNewFile()
            }
            connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            plugin.logger.debug("Connection to the SQLite database established.")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to establish connection to the SQLite database. ${e.message}")
        }
    }

    private fun isConnected(): Boolean {
        return connection != null && !connection!!.isClosed
    }

    override fun createTables() {
        if (isConnected()) {
            try {
                val statement = connection!!.createStatement()
                val createPunishmentsTable = """
                CREATE TABLE IF NOT EXISTS `punishments` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                  `name` TEXT,
                  `uuid` TEXT,
                  `reason` TEXT,
                  `operator` TEXT,
                  `punishmentType` TEXT,
                  `start` INTEGER,
                  `end` TEXT
                );
            """.trimIndent()

                val createPunishmentHistoryTable = """
                CREATE TABLE IF NOT EXISTS `punishmenthistory` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                  `name` TEXT,
                  `uuid` TEXT,
                  `reason` TEXT,
                  `operator` TEXT,
                  `punishmentType` TEXT,
                  `start` INTEGER,
                  `end` TEXT
                );
            """.trimIndent()

                statement.executeUpdate(createPunishmentsTable)
                statement.executeUpdate(createPunishmentHistoryTable)
                plugin.logger.debug("Tables `punishments` and `punishmenthistory` created or already exist.")
            } catch (e: SQLException) {
                plugin.logger.err("Failed to create tables. ${e.message}")
            }
        } else {
            plugin.logger.warning("Not connected to the database.")
        }
    }

    override fun closeConnection() {
        try {
            connection?.close()
            plugin.logger.info("Connection to the SQLite database closed.")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to close the connection to the SQLite database. ${e.message}")
        }
    }

    override fun addPunishment(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long): Boolean {
        if (!isConnected()) {
            openConnection()
        }

        if (isConnected()) {
            val query = """
        INSERT INTO `punishments` (name, uuid, reason, operator, punishmentType, start, end)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

            return try {
                val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
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
            } catch (e: SQLException) {
                plugin.logger.err("Failed to add punishment for player $name. ${e.message}")
                false
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
            return false
        }
    }

    override fun addPunishmentHistory(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long) {
        if (!isConnected()) {
            openConnection()
        }

        if (isConnected()) {
            val query = """
        INSERT INTO `punishmenthistory` (name, uuid, reason, operator, punishmentType, start, end)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

            try {
                val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
                preparedStatement.setString(1, name)
                preparedStatement.setString(2, uuid)
                preparedStatement.setString(3, reason)
                preparedStatement.setString(4, operator)
                preparedStatement.setString(5, punishmentType)
                preparedStatement.setLong(6, start)
                preparedStatement.setLong(7, end)
                preparedStatement.executeUpdate()
                plugin.logger.debug("Punishment history for player $name added to the database.")
            } catch (e: SQLException) {
                plugin.logger.err("Failed to add punishment history for player $name. ${e.message}")
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
        }
    }

    override fun removePunishment(uuidOrIp: String, punishmentType: String, removeAll: Boolean) {
        if (!isConnected()) {
            openConnection()
        }
        if (isConnected()) {
            try {
                if (removeAll) {
                    val query = """
                    DELETE FROM `punishments` 
                    WHERE `uuid` = ? AND `punishmentType` = ?
                """.trimIndent()
                    val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
                    preparedStatement.setString(1, uuidOrIp)
                    preparedStatement.setString(2, punishmentType)
                    val rowsAffected = preparedStatement.executeUpdate()
                    if (rowsAffected > 0) {
                        plugin.logger.debug("Punishment of type $punishmentType for UUID/IP: $uuidOrIp removed from the database.")
                    } else {
                        plugin.logger.warning("No punishment of type $punishmentType found for UUID/IP: $uuidOrIp.")
                    }
                } else {
                    val selectQuery = """
                    SELECT `id` FROM `punishments` 
                    WHERE `uuid` = ? AND `punishmentType` = ?
                    ORDER BY `start` DESC
                    LIMIT 1
                """.trimIndent()
                    val selectStatement: PreparedStatement = connection!!.prepareStatement(selectQuery)
                    selectStatement.setString(1, uuidOrIp)
                    selectStatement.setString(2, punishmentType)
                    val resultSet = selectStatement.executeQuery()
                    if (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val deleteQuery = """
                        DELETE FROM `punishments` 
                        WHERE `id` = ?
                    """.trimIndent()
                        val deleteStatement: PreparedStatement = connection!!.prepareStatement(deleteQuery)
                        deleteStatement.setInt(1, id)
                        val rowsAffected = deleteStatement.executeUpdate()
                        if (rowsAffected > 0) {
                            plugin.logger.debug("Punishment of type $punishmentType for UUID/IP: $uuidOrIp removed from the database.")
                        } else {
                            plugin.logger.warning("No punishment of type $punishmentType found for UUID/IP: $uuidOrIp.")
                        }
                    } else {
                        plugin.logger.warning("No punishment of type $punishmentType found for UUID/IP: $uuidOrIp.")
                    }
                }
            } catch (e: SQLException) {
                plugin.logger.err("Failed to remove punishment of type $punishmentType for UUID/IP: $uuidOrIp. ${e.message}")
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
        }
    }

    override fun getPunishments(uuid: String): List<PunishmentData> {
        if (!isConnected()) {
            openConnection()
        }

        val query = "SELECT * FROM punishments WHERE uuid = ?"
        val punishments = mutableListOf<PunishmentData>()
        try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, uuid)
            val resultSet: ResultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val type = resultSet.getString("punishmentType")
                val reason = resultSet.getString("reason")
                val start = resultSet.getLong("start")
                val end = resultSet.getLong("end")
                val punishment = PunishmentData(uuid, type, reason, start, end)
                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    punishments.add(punishment)
                } else {
                    removePunishment(uuid, type)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishments for UUID: $uuid. ${e.message}")
        }
        return punishments
    }

    override fun getPunishmentsByIP(ip: String): List<PunishmentData> {
        if (!isConnected()) {
            openConnection()
        }

        val query = "SELECT * FROM punishments WHERE uuid = ?"
        val punishments = mutableListOf<PunishmentData>()
        try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, ip)
            val resultSet: ResultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val type = resultSet.getString("punishmentType")
                val reason = resultSet.getString("reason")
                val start = resultSet.getLong("start")
                val end = resultSet.getLong("end")
                val punishment = PunishmentData(ip, type, reason, start, end)
                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    punishments.add(punishment)
                } else {
                    removePunishment(ip, type)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishments for IP: $ip. ${e.message}")
        }
        return punishments
    }

    override fun getActiveWarnCount(uuid: String): Int {
        if (!isConnected()) {
            openConnection()
        }

        val query = "SELECT * FROM punishments WHERE uuid = ? AND punishmentType = 'WARN'"
        val punishments = mutableListOf<PunishmentData>()
        try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, uuid)
            val resultSet: ResultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val type = resultSet.getString("punishmentType")
                val reason = resultSet.getString("reason")
                val start = resultSet.getLong("start")
                val end = resultSet.getLong("end")
                val punishment = PunishmentData(uuid, type, reason, start, end)
                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    punishments.add(punishment)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get active warn count for UUID: $uuid. ${e.message}")
        }
        return punishments.size
    }
    override fun exportDatabase() {
        val tables = listOf("punishments", "punishmenthistory")
        try {
            connection?.let { conn ->
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
                        // Tabela jest pusta, pomijamy eksport
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
                plugin.logger.success("SQLite database exported to ${dumpDir.absolutePath}/backup.sql")
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to export SQLite database. ${e.message}")
        } catch (e: IOException) {
            plugin.logger.err("Failed to write to file. ${e.message}")
        }
    }

    override fun importDatabase() {
        val filePath = File(plugin.dataFolder, "dump/backup.sql").absolutePath
        try {
            connection?.let { conn ->
                val lines = File(filePath).readLines()
                val statement = conn.createStatement()
                val sql = StringBuilder()
                for (line in lines) {
                    sql.append(line)
                    if (line.trim().endsWith(";")) {
                        statement.execute(sql.toString())
                        sql.setLength(0) // Clear the StringBuilder
                    }
                }
                plugin.logger.info("SQLite database imported from $filePath")
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to import SQLite database. ${e.message}")
        } catch (e: IOException) {
            plugin.logger.err("Failed to read from file. ${e.message}")
        }
    }
}
