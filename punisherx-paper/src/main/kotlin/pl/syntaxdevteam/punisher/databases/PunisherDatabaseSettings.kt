package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.ConfigurationSection
import pl.syntaxdevteam.syntaxdb.config.DatabaseConfig
import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import pl.syntaxdevteam.syntaxdb.config.databaseConfig
import java.io.File
import java.util.Locale

/** Keeps existing PunisherX configuration keys and SQLite paths across the SyntaxDB upgrade. */
object PunisherDatabaseSettings {
    fun parseType(value: String): DatabaseType {
        val normalized = value.trim().lowercase(Locale.ROOT)
        require(normalized != "h2") {
            "H2 is not supported by SyntaxDB. Export and convert the existing H2 database before upgrading; do not change database.type on an existing H2 installation without transferring its data."
        }
        val type = if (normalized == "mariadb") DatabaseType.MYSQL else DatabaseType.from(value)
        require(type in setOf(DatabaseType.MYSQL, DatabaseType.POSTGRESQL, DatabaseType.SQL_SERVER, DatabaseType.SQLITE)) {
            "PunisherX supports mysql, mariadb, postgresql, mssql and sqlite."
        }
        return type
    }

    fun create(config: ConfigurationSection, folder: File, pluginName: String?, type: DatabaseType): DatabaseConfig =
        databaseConfig(type) {
            host = config.getString("database.sql.host") ?: "localhost"
            port = config.getInt("database.sql.port").takeIf { it > 0 } ?: type.defaultPort
            val name = config.getString("database.sql.dbname") ?: pluginName ?: "PunisherX"
            if (type == DatabaseType.SQLITE) {
                check(folder.isDirectory || folder.mkdirs()) { "Cannot create database directory: $folder" }
                // SyntaxCore used this exact path, without appending .db or .sqlite.
                file = File(folder, name).absolutePath
            } else {
                database = name
                username = config.getString("database.sql.username") ?: "ROOT"
                password = config.getString("database.sql.password") ?: "U5eV3ryStr0ngP4ssw0rd"
            }
            config.getString("database.sql.jdbc-url")?.takeIf { it.isNotBlank() }?.let { jdbcUrl = it }
            if (type == DatabaseType.MYSQL) {
                property("characterEncoding", "UTF-8")
            }
            pool {
                connectionTimeoutMs = 30_000
                poolName = "PunisherX-${type.configName}"
            }
        }
}
