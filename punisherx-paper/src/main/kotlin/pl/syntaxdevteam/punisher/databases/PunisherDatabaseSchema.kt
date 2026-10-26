package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.syntaxdb.Database
import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import java.util.Locale
import pl.syntaxdevteam.syntaxdb.schema.CreateTableBuilder

/** Existing names and column order are retained, including PostgreSQL's unquoted lower-case names. */
internal object PunisherDatabaseSchema {
    val tables = listOf("punishments", "punishmenthistory", "punishment_undos", "punishment_templates",
        "playercache", "reports", "report_resolutions", "bridge_events",
        "punisherx_identity_migrations", "punisherx_identity_migration_items")
    val identityTables = setOf("punishments", "punishmenthistory", "playercache", "reports", "bridge_events")

    fun column(database: Database, name: String): String =
        if (database.dialect.type == DatabaseType.POSTGRESQL) name.lowercase(Locale.ROOT) else name

    fun create(database: Database) {
        fun c(name: String) = column(database, name)
        for (table in listOf("punishments", "punishmenthistory")) {
            // MySQL needs explicit utf8mb4 even when the existing database defaults to latin1.
            if (database.dialect.type == DatabaseType.MYSQL) {
                database.execute("""CREATE TABLE IF NOT EXISTS $table (
                    id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(32), uuid VARCHAR(36),
                    reason VARCHAR(255) CHARACTER SET utf8mb4, operator VARCHAR(16), punishmentType VARCHAR(16),
                    start BIGINT, endTime BIGINT, server VARCHAR(64) DEFAULT 'network'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""".trimIndent())
            } else database.createTable(table, ifNotExists = true) {
                id(); varchar("name", 32); varchar("uuid", 36); varchar("reason", 255)
                varchar("operator", 16); varchar(c("punishmentType"), 16)
                bigInt("start"); bigInt(c("endTime")); varchar("server", 64).default("network")
            }
        }
        database.createTable("punishment_undos", ifNotExists = true) {
            integer(c("historyId")).primaryKey(); varchar(c("undoneBy"), 64).notNull(); bigInt(c("undoneAt")).notNull()
        }
        database.createTable("punishment_templates", ifNotExists = true) {
            integer(c("historyId")).primaryKey(); varchar(c("templateName"), 255).notNull(); varchar(c("playerUuid"), 36).notNull()
        }
        database.createTable("playercache", ifNotExists = true) { id(); text("data") }
        database.createTable("reports", ifNotExists = true) {
            id(); varchar("player", 36); varchar("suspect", 36); text("reason")
            timestamp(c("filedAt")).defaultCurrentTimestamp()
        }
        database.createTable("report_resolutions", ifNotExists = true) {
            integer(c("reportId")).primaryKey(); varchar("status", 16).notNull(); varchar(c("handledBy"), 64).notNull()
            varchar("note", 255).notNull(); bigInt(c("handledAt")).notNull()
        }
        if (database.dialect.type == DatabaseType.MYSQL) {
            database.execute("""CREATE TABLE IF NOT EXISTS bridge_events (
                id INT AUTO_INCREMENT PRIMARY KEY, action VARCHAR(16) NOT NULL, target VARCHAR(64) NOT NULL,
                reason TEXT CHARACTER SET utf8mb4, endTime BIGINT NOT NULL, processed TINYINT DEFAULT 0, processedAt BIGINT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""".trimIndent())
        } else database.createTable("bridge_events", ifNotExists = true) {
            id(); varchar("action", 16).notNull(); varchar("target", 64).notNull(); text("reason")
            bigInt(c("endTime")).notNull(); integer("processed").default(0); bigInt(c("processedAt"))
        }
        database.createTable("punisherx_identity_migrations", ifNotExists = true) {
            varchar("migration_id", 36).primaryKey(); varchar("source_uuid", 36).notNull()
            varchar("target_uuid", 36).notNull(); varchar("state", 24).notNull()
        }
        database.createTable("punisherx_identity_migration_items", ifNotExists = true) {
            varchar("migration_id", 36).notNull(); varchar("entity_type", 32).notNull(); varchar("entity_key", 64).notNull()
            primaryKey("migration_id", "entity_type", "entity_key")
        }
        ensureScope(database)
    }

    private fun Database.createTable(name: String, ifNotExists: Boolean, block: CreateTableBuilder.() -> Unit) {
        val plan = schema.compileCreateTable(name, ifNotExists, block)
        plan.statements.forEach { statement ->
            val sql = if (dialect.type == DatabaseType.MYSQL) statement.sql + " DEFAULT CHARSET=utf8mb4" else statement.sql
            execute(sql, statement.parameters)
        }
    }

    private fun ensureScope(database: Database) {
        for (table in listOf("punishments", "punishmenthistory")) {
            val exists = database.withConnection { connection ->
                if (database.dialect.type == DatabaseType.SQLITE) {
                    connection.createStatement().use { statement ->
                        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
                            var found = false
                            while (rows.next()) if (rows.getString("name").equals("server", true)) found = true
                            found
                        }
                    }
                } else connection.metaData.getColumns(connection.catalog, connection.schema, table, null).use { rows ->
                    var found = false
                    while (rows.next()) if (rows.getString("COLUMN_NAME").equals("server", true)) found = true
                    found
                }
            }
            if (!exists) database.schema.alterTable(table) { varchar("server", 64).default("network") }
            database.execute("UPDATE $table SET server = ? WHERE server IS NULL", listOf("network"))
        }
    }
}
