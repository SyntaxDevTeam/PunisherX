package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import kotlin.test.*

class SyntaxDbSettingsContractTest : SyntaxDbTestFixture() {
    @Test fun `all documented engine aliases are accepted with whitespace and mixed case`() {
        val aliases = mapOf(
            DatabaseType.SQLITE to listOf("sqlite", "sqlite3"),
            DatabaseType.MYSQL to listOf("mysql", "mariadb"),
            DatabaseType.POSTGRESQL to listOf("postgresql", "postgres", "pgsql"),
            DatabaseType.SQL_SERVER to listOf("mssql", "sqlserver", "sql_server", "sql-server")
        )
        for ((type, names) in aliases) for (name in names) {
            assertEquals(type, PunisherDatabaseSettings.parseType(" ${name.uppercase()} "), name)
        }
    }

    @Test fun `unsupported and unknown engines fail before opening a database`() {
        for (name in listOf("h2", "duckdb", "unknown", "", "   ")) {
            assertFailsWith<IllegalArgumentException>(name) { PunisherDatabaseSettings.parseType(name) }
        }
    }

    @Test fun `default ports and explicit ports are preserved for remote engines`() {
        config.set("database.sql.host", "db.example")
        config.set("database.sql.dbname", "punisher")
        for ((type, port) in mapOf(DatabaseType.MYSQL to 3306, DatabaseType.POSTGRESQL to 5432, DatabaseType.SQL_SERVER to 1433)) {
            for (unset in listOf(null, 0, -1)) {
                config.set("database.sql.port", unset)
                assertContains(PunisherDatabaseSettings.create(config, folder, "PunisherX", type).jdbcUrl, ":$port")
            }
            config.set("database.sql.port", 15432)
            assertContains(PunisherDatabaseSettings.create(config, folder, "PunisherX", type).jdbcUrl, ":15432")
        }
    }

    @Test fun `custom URL wins and blank URL uses configured engine`() {
        val url = "jdbc:sqlserver://db.example:1444;databaseName=punisher;encrypt=true"
        config.set("database.sql.jdbc-url", url)
        assertEquals(url, PunisherDatabaseSettings.create(config, folder, "PunisherX", DatabaseType.SQL_SERVER).jdbcUrl)
        config.set("database.sql.jdbc-url", " ")
        assertEquals("jdbc:sqlite:${folder.resolve("contract.sqlite")}", PunisherDatabaseSettings.create(config, folder, "PunisherX", DatabaseType.SQLITE).jdbcUrl)
    }

    @Test fun `SQLite fallback name uses plugin name then PunisherX without extra extension`() {
        config.set("database.sql.dbname", null)
        assertEquals("jdbc:sqlite:${folder.resolve("CustomPlugin")}", PunisherDatabaseSettings.create(config, folder, "CustomPlugin", DatabaseType.SQLITE).jdbcUrl)
        assertEquals("jdbc:sqlite:${folder.resolve("PunisherX")}", PunisherDatabaseSettings.create(config, folder, null, DatabaseType.SQLITE).jdbcUrl)
    }

    @Test fun `SQL Server pagination handles all literal and bound combinations`() {
        config.set("database.type", "mssql")
        val server = DatabaseHandler(plugin)
        try {
            val method = DatabaseHandler::class.java.getDeclaredMethod("paginatedSql", String::class.java, List::class.java).apply { isAccessible = true }
            val cases = listOf(
                Triple(" LIMIT ? OFFSET ?", listOf<Any>("uuid", 5, 10), listOf<Any>("uuid", 10, 5)),
                Triple(" LIMIT 5 OFFSET ?", listOf<Any>("uuid", 10), listOf<Any>("uuid", 10, 5)),
                Triple(" LIMIT ? OFFSET 10", listOf<Any>("uuid", 5), listOf<Any>("uuid", 10, 5)),
                Triple(" LIMIT 5 OFFSET 10", listOf<Any>("uuid"), listOf<Any>("uuid", 10, 5)),
                Triple(" LIMIT ?", listOf<Any>("uuid", 5), listOf<Any>("uuid", 0, 5))
            )
            for ((suffix, params, expected) in cases) {
                val result = method.invoke(server, "SELECT * FROM punishments WHERE uuid = ?$suffix", params) as Pair<*, *>
                assertEquals("SELECT * FROM punishments WHERE uuid = ? ORDER BY id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", result.first)
                assertEquals(expected, result.second)
            }
            val sql = "SELECT * FROM punishments WHERE uuid = ? ORDER BY start DESC, id DESC LIMIT 10"
            val result = method.invoke(server, sql, listOf("uuid")) as Pair<*, *>
            assertEquals("SELECT * FROM punishments WHERE uuid = ? ORDER BY start DESC, id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", result.first)
        } finally { server.closeConnection() }
    }
}
