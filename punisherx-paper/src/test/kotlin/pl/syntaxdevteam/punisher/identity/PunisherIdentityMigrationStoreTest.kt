package pl.syntaxdevteam.punisher.identity

import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PunisherIdentityMigrationStoreTest {
    private val source = UUID.fromString("11111111-1111-3111-8111-111111111111")
    private val target = UUID.fromString("22222222-2222-4222-8222-222222222222")

    @Test
    fun `migration and rollback preserve exact punishment and report rows`() {
        database { connection ->
            insertFixtures(connection)
            val migrationId = UUID.randomUUID()

            val inspection = PunisherIdentityMigrationStore.inspect(connection, migrationId, source, target)
            assertEquals(PunisherIdentityMigrationStatus.READY, inspection.status)
            assertTrue(inspection.legacyEvidence)

            assertEquals(
                PunisherIdentityMigrationStatus.SUCCESS,
                PunisherIdentityMigrationStore.migrate(connection, migrationId, source, target).status,
            )
            assertEquals(target.toString(), scalar(connection, "SELECT uuid FROM punishments WHERE id = 1"))
            assertEquals(target.toString(), scalar(connection, "SELECT uuid FROM punishmenthistory WHERE id = 2"))
            assertEquals(target.toString(), scalar(connection, "SELECT player FROM reports WHERE id = 3"))
            assertEquals(target.toString(), scalar(connection, "SELECT suspect FROM reports WHERE id = 4"))
            assertEquals(target.toString(), scalar(connection, "SELECT target FROM bridge_events WHERE id = 5"))

            assertEquals(
                PunisherIdentityMigrationStatus.SUCCESS,
                PunisherIdentityMigrationStore.migrate(connection, migrationId, source, target).status,
            )

            assertEquals(
                PunisherIdentityMigrationStatus.ROLLED_BACK,
                PunisherIdentityMigrationStore.rollback(connection, migrationId, source, target).status,
            )
            assertEquals(source.toString(), scalar(connection, "SELECT uuid FROM punishments WHERE id = 1"))
            assertEquals(source.toString(), scalar(connection, "SELECT uuid FROM punishmenthistory WHERE id = 2"))
            assertEquals(source.toString(), scalar(connection, "SELECT player FROM reports WHERE id = 3"))
            assertEquals(source.toString(), scalar(connection, "SELECT suspect FROM reports WHERE id = 4"))
            assertEquals(source.toString(), scalar(connection, "SELECT target FROM bridge_events WHERE id = 5"))
        }
    }

    private fun database(test: (Connection) -> Unit) {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            listOf(
                "CREATE TABLE punishments(id INTEGER PRIMARY KEY, uuid VARCHAR(36))",
                "CREATE TABLE punishmenthistory(id INTEGER PRIMARY KEY, uuid VARCHAR(36))",
                "CREATE TABLE reports(id INTEGER PRIMARY KEY, player VARCHAR(36), suspect VARCHAR(36))",
                "CREATE TABLE bridge_events(id INTEGER PRIMARY KEY, target VARCHAR(64))",
            ).forEach { sql -> connection.createStatement().use { it.execute(sql) } }
            PunisherIdentityMigrationStore.migrateSchema(connection)
            test(connection)
        }
    }

    private fun insertFixtures(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate("INSERT INTO punishments(id, uuid) VALUES (1, '$source')")
            statement.executeUpdate("INSERT INTO punishmenthistory(id, uuid) VALUES (2, '$source')")
            statement.executeUpdate("INSERT INTO reports(id, player, suspect) VALUES (3, '$source', '33333333-3333-4333-8333-333333333333')")
            statement.executeUpdate("INSERT INTO reports(id, player, suspect) VALUES (4, '44444444-4444-4444-8444-444444444444', '$source')")
            statement.executeUpdate("INSERT INTO bridge_events(id, target) VALUES (5, '$source')")
        }
    }

    private fun scalar(connection: Connection, sql: String): String =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                assertTrue(rows.next())
                rows.getString(1)
            }
        }
}
