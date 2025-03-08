package pl.syntaxdevteam.punisher.databases

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.sql.*

/**
 * Represents a handler for database operations.
 *
 * This class provides methods to interact with the database, such as creating tables, adding punishments,
 * removing punishments, and retrieving punishment data. It uses the HikariCP connection pool to manage
 * database connections efficiently and safely.
 *
 * @param plugin The PunisherX plugin instance.
 */
@Suppress("KDocUnresolvedReference")
class DatabaseHandler(private val plugin: PunisherX) {
    private var dataSource: HikariDataSource? = null
    private var logger = plugin.logger
    private val dbType = plugin.config.getString("database.type")?.lowercase() ?: "sqlite"

    init {
        setupDataSource()
    }

    /**
     * Configures the HikariCP data source for database connections.
     *
     * This method sets up the data source based on the database type defined in the plugin configuration.
     * It supports the following database types:
     * - MySQL/MariaDB
     * - PostgreSQL
     * - SQLite
     * - H2
     *
     * For SQLite databases, specific pool settings are applied to minimize resource usage.
     * For other database types, the pool is configured with more robust settings suitable for production.
     *
     * @throws IllegalArgumentException If the database type is unsupported.
     * @throws Exception If an error occurs during data source initialization.
     */

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

    /**
     * Ensures that the database connection is open.
     *
     * If the data source is not initialized, this method will invoke `setupDataSource`
     * to configure and start the connection pool.
     */
    fun openConnection() {
        if (dataSource == null) {
            setupDataSource()
        }
    }

    /**
     * Closes the HikariCP connection pool.
     *
     * This method gracefully shuts down the data source, releasing all resources associated with
     * the connection pool. It also logs the pool's statistics, such as total, active, and idle connections.
     *
     * Any errors encountered during shutdown are logged but do not interrupt the process.
     */
    fun closeConnection() {
        try {
            dataSource?.close()
            logger.info("HikariCP pool shut down. Total=${dataSource?.hikariPoolMXBean?.totalConnections}, Active=${dataSource?.hikariPoolMXBean?.activeConnections}, Idle=${dataSource?.hikariPoolMXBean?.idleConnections}")
        } catch (e: SQLException) {
            logger.err("Error while closing HikariCP pool: ${e.message}")
        }
    }

    /**
     * Obtains a connection from the HikariCP data source.
     *
     * This method retrieves a connection from the pool and, if the database type is SQLite,
     * enables Write-Ahead Logging (WAL) mode to improve performance.
     *
     * Connections must be properly closed after use to avoid resource leaks.
     *
     * @return A valid `Connection` object, or `null` if the connection could not be established.
     */
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

    /**
     * Enables Write-Ahead Logging (WAL) mode for SQLite.
     *
     * WAL mode improves SQLite's performance by allowing concurrent reads and writes.
     * This method executes the `PRAGMA journal_mode=WAL;` statement on the provided connection.
     *
     * @param connection The active SQLite connection.
     */
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

    /**
     * Creates necessary tables in the database.
     *
     * This method executes SQL statements to create the following tables if they do not already exist:
     * - `punishments`: Stores information about active punishments.
     * - `punishmenthistory`: Stores information about historical punishments.
     *
     * The table structures and types are adapted to the configured database type
     * (e.g., `SQLite`, `PostgreSQL`, `H2`, `MySQL`/`MariaDB`).
     *
     * SQL statements are constructed dynamically to match the syntax and capabilities of each database type.
     *
     * @throws SQLException If an error occurs while creating the tables.
     */
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

    /**
     * Adds a punishment to the punishments' database.
     *
     * This method establishes a connection to the database and executes an SQL insert statement
     * to add a punishment record to the `punishments` table. The record includes the player's
     * name, UUID, reason, operator, punishment type, start time, and end time.
     *
     * If the operation is successful, the method logs a debug message indicating the punishment was added
     * and returns `true`. If no connection is available or an SQL exception occurs, an error message is logged,
     * and the method returns `false`.
     *
     * @param name The name of the player to add the punishment for.
     * @param uuid The UUID of the player to add the punishment for.
     * @param reason The reason for the punishment.
     * @param operator The name of the operator who issued the punishment.
     * @param punishmentType The type of punishment (e.g., "BAN", "MUTE", "WARN").
     * @param start The start time of the punishment.
     * @param end The end time of the punishment.
     * @return `true` if the punishment was added successfully; `false` otherwise.
     */
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

    /**
     * Adds a punishment to the punishment history database.
     *
     * This method establishes a connection to the database and executes an SQL insert statement
     * to add a punishment record to the `punishmenthistory` table. The record includes the player's
     * name, UUID, reason, operator, punishment type, start time, and end time.
     *
     * If the operation is successful, the method logs a debug message indicating the punishment was added.
     * If no connection is available or an SQL exception occurs, an error message is logged.
     *
     * @param name The name of the player to add the punishment for.
     * @param uuid The UUID of the player to add the punishment for.
     * @param reason The reason for the punishment.
     * @param operator The name of the operator who issued the punishment.
     * @param punishmentType The type of punishment (e.g., "BAN", "MUTE", "WARN").
     * @param start The start time of the punishment.
     * @param end The end time of the punishment.
     */
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
                }
            } ?: throw SQLException("No connection available")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to add punishment history for player $name. ${e.message}")
        }
    }

    /**
     * Retrieves all active punishments for a given player UUID from the punishments' database.
     *
     * This method establishes a connection to the database, executes a query to fetch all active
     * punishments for the specified UUID, and returns a list of PunishmentData objects representing
     * the retrieved punishments. If no connection is available or an SQL exception occurs, an error
     * message is logged, and an empty list is returned.
     *
     * @param uuid The UUID of the player whose active punishments are to be retrieved.
     * @return A list of PunishmentData objects representing the active punishments for the specified UUID.
     */
    fun getPunishments(uuid: String): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        logger.debug("Database connection established from getPunishments")
        try {
            getConnection()?.use { conn ->
                val query = "SELECT * FROM punishments WHERE uuid = ?"
                conn.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, uuid)
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
                            punishmentsToRemove.add(uuid to type) // Zbieramy dane do usunięcia
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

    /**
     * Retrieves all active punishments for a given player UUID from the punishments' database.
     *
     * This method establishes a connection to the database, executes a query to fetch all active
     * punishments for the specified UUID, and returns a list of PunishmentData objects representing
     * the retrieved punishments. If no connection is available or an SQL exception occurs, an error
     * message is logged, and an empty list is returned.
     *
     * @param uuid The UUID of the player whose active punishments are to be retrieved.
     * @return A list of PunishmentData objects representing the active punishments for the specified UUID.
     */
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

    /**
     * Removes a punishment from the database.
     *
     * This method establishes a connection to the database and executes an SQL delete statement
     * to remove the punishment with the specified UUID/IP and punishment type. If `removeAll` is `true`,
     * all punishments of the specified type for the given UUID/IP are removed. Otherwise, only the most
     * recent punishment of the specified type is deleted.
     *
     * If the operation is successful, the method logs a debug message indicating the punishment was removed.
     * If no connection is available or an SQL exception occurs, an error message is logged.
     *
     * @param uuidOrIp The UUID or IP address of the player to remove the punishment for.
     * @param punishmentType The type of punishment to remove (e.g., "BAN", "MUTE", "WARN").
     * @param removeAll `true` to remove all punishments of the specified type; `false` to remove only the most recent one.
     */
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

    /**
     * Retrieves the active warn count for a given player UUID from the punishments' database.
     *
     * This method establishes a connection to the database, executes a query to fetch the active
     * warn count for the specified UUID, and returns the number of active warn punishments.
     * If no connection is available or an SQL exception occurs, an error message is logged,
     * and the method returns 0.
     *
     * @param uuid The UUID of the player whose active warn count is to be retrieved.
     * @return The number of active warn punishments for the specified UUID.
     */
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

    /**
     * Retrieves the punishment history for a given player UUID from the punishment history database.
     *
     * This method establishes a connection to the database, executes a query to fetch the punishment
     * history for the specified UUID, and returns a list of PunishmentData objects representing the
     * retrieved punishments. If no connection is available or an SQL exception occurs, an error message
     * is logged and an empty list is returned.
     *
     * @param uuid The UUID of the player whose punishment history is to be retrieved.
     * @param limit The maximum number of punishments to retrieve.
     * @param offset The number of punishments to skip before retrieving the results.
     * @return A list of PunishmentData objects representing the punishment history for the specified UUID.
     */
    fun getPunishmentHistory(uuid: String, limit: Int, offset: Int): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        try {
            getConnection()?.use { conn ->
                logger.debug("Database connection established from getPunishmentHistory")
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
            plugin.logger.err("Failed to get punishment history for UUID: $uuid. ${e.message}")
        }
        return punishments
    }

    /**
     * Retrieves the last ten punishments for a given player UUID from the punishment history database.
     *
     * This method establishes a connection to the database, executes a query to fetch the last ten
     * punishments for the specified UUID, and returns a list of PunishmentData objects representing
     * the retrieved punishments. If no connection is available or an SQL exception occurs, an error
     * message is logged and an empty list is returned.
     *
     * @param uuid The UUID of the player whose punishment history is to be retrieved.
     * @return A list of PunishmentData objects representing the last ten punishments for the specified UUID.
     */
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

    /**
     * Updates the reason for a punishment in the punishment history database.
     *
     * This method establishes a connection to the database and executes an SQL update statement
     * to change the reason for a punishment with the specified ID. If the update is successful,
     * the method returns `true`; otherwise, it returns `false`.
     *
     * @param id The ID of the punishment to update.
     * @param newReason The new reason for the punishment.
     * @return `true` if the update was successful; `false` otherwise.
     */
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