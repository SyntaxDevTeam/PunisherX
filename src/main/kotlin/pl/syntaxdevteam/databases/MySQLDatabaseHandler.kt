package pl.syntaxdevteam.databases

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.ResultSet
import java.sql.Statement
import org.bukkit.configuration.file.FileConfiguration
import pl.syntaxdevteam.PunisherX

class MySQLDatabaseHandler(private val plugin: PunisherX, config: FileConfiguration) : DatabaseHandler {
    private var connection: Connection? = null
    private val url: String = "jdbc:mysql://${config.getString("database.sql.host")}:${config.getString("database.sql.port")}/${config.getString("database.sql.dbname")}"
    private val user: String = config.getString("database.sql.username") ?: ""
    private val password: String = config.getString("database.sql.password") ?: ""

    override fun openConnection() {
        try {
            connection = DriverManager.getConnection(url, user, password)
            plugin.logger.debug("Connection to the database established.")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to establish connection to the database. ${e.message}")
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
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `name` varchar(32) DEFAULT NULL,
                  `uuid` varchar(36) DEFAULT NULL,
                  `reason` varchar(255) DEFAULT NULL,
                  `operator` varchar(16) DEFAULT NULL,
                  `punishmentType` varchar(16) DEFAULT NULL,
                  `start` bigint(20) DEFAULT NULL,
                  `end` varchar(32) DEFAULT NULL,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
            """.trimIndent()

                val createPunishmentHistoryTable = """
                CREATE TABLE IF NOT EXISTS `punishmenthistory` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `name` varchar(32) DEFAULT NULL,
                  `uuid` varchar(36) DEFAULT NULL,
                  `reason` varchar(255) DEFAULT NULL,
                  `operator` varchar(16) DEFAULT NULL,
                  `punishmentType` varchar(16) DEFAULT NULL,
                  `start` bigint(20) DEFAULT NULL,
                  `end` varchar(32) DEFAULT NULL,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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

    override fun addPunishment(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long) {
        if (!isConnected()) {
            openConnection()
        }

        if (isConnected()) {
            val query = """
            INSERT INTO `punishments` (name, uuid, reason, operator, punishmentType, start, end)
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
                plugin.logger.debug("Punishment for player $name added to the database.")
            } catch (e: SQLException) {
                plugin.logger.err("Failed to add punishment for player $name. ${e.message}")
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
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

    override fun closeConnection() {
        try {
            connection?.close()
            plugin.logger.info("Connection to the database closed.")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to close the connection to the database. ${e.message}")
        }
    }
    override fun getPunishment(uuid: String): PunishmentData? {
        if (!isConnected()) {
            openConnection()
        }

        val statement: Statement? = connection?.createStatement()
        plugin.logger.debug("Wykonywanie zapytania SQL dla UUID: $uuid")
        val resultSet: ResultSet? = statement?.executeQuery("SELECT * FROM punishments WHERE uuid = '$uuid'")
        return if (resultSet != null && resultSet.next()) {
            val type = resultSet.getString("punishmentType")
            val reason = resultSet.getString("reason")
            val start = resultSet.getLong("start")
            val end = resultSet.getLong("end")
            val punishment = PunishmentData(uuid, type, reason, start, end)
            if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                plugin.logger.debug("Kara znaleziona dla UUID: $uuid, typ: $type, powód: $reason, start: $start, koniec: $end")
                punishment
            } else {
                plugin.logger.debug("Kara dla UUID: $uuid wygasła i została usunięta")
                removePunishment(uuid, type)
                null
            }
        } else {
            plugin.logger.debug("Brak kary dla UUID: $uuid")
            null
        }
    }

    override fun getPunishmentByIP(ip: String): PunishmentData? {
        if (!isConnected()) {
            openConnection()
        }

        val statement: Statement? = connection?.createStatement()
        plugin.logger.debug("Wykonywanie zapytania SQL dla IP: $ip")
        val resultSet: ResultSet? = statement?.executeQuery("SELECT * FROM punishments WHERE uuid = '$ip'")
        return if (resultSet != null && resultSet.next()) {
            val type = resultSet.getString("punishmentType")
            val reason = resultSet.getString("reason")
            val start = resultSet.getLong("start")
            val end = resultSet.getLong("end")
            val punishment = PunishmentData(ip, type, reason, start, end)
            if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                plugin.logger.debug("Kara znaleziona dla IP: $ip, typ: $type, powód: $reason, start: $start, koniec: $end")
                punishment
            } else {
                plugin.logger.debug("Kara dla IP: $ip wygasła i została usunięta")
                removePunishment(ip, type)
                null
            }
        } else {
            plugin.logger.debug("Brak kary dla IP: $ip")
            null
        }
    }

    override fun removePunishment(uuidOrIp: String, punishmentType: String) {
        if (!isConnected()) {
            openConnection()
        }
        if (isConnected()) {
            val query = """
        DELETE FROM `punishments` 
        WHERE `uuid` = ? AND `punishmentType` = ?
    """.trimIndent()
            try {
                val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
                preparedStatement.setString(1, uuidOrIp)
                preparedStatement.setString(2, punishmentType)
                val rowsAffected = preparedStatement.executeUpdate()
                if (rowsAffected > 0) {
                    plugin.logger.debug("Punishment of type $punishmentType for UUID/IP: $uuidOrIp removed from the database.")
                } else {
                    plugin.logger.warning("No punishment of type $punishmentType found for UUID/IP: $uuidOrIp.")
                }
            } catch (e: SQLException) {
                plugin.logger.err("Failed to remove punishment of type $punishmentType for UUID/IP: $uuidOrIp. ${e.message}")
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
        }
    }
}