package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.syntaxdb.Database
import pl.syntaxdevteam.punisher.identity.IdentityRegistryStore
import pl.syntaxdevteam.punisher.identity.IdentitySettings
import java.nio.file.Files
import kotlin.test.*

class IdentityBackupTest {
    @Test fun `bound database accepts matching domain and rejects foreign or unlabelled backup atomically`() {
        val file = Files.createTempFile("identity-backup-", ".sqlite")
        Database.connect("sqlite") { this.file = file.toString() }.use { db ->
            PunisherDatabaseSchema.create(db)
            db.withConnection { IdentityRegistryStore.bind(it, IdentitySettings.create("network", "online", true)) }
            val row = mapOf<String, Any?>("id" to 1, "name" to "Target", "uuid" to "2001:0db8:0:0:0:0:0:1", "punishmentType" to "BANIP", "start" to 1, "endTime" to -1, "server" to "network")
            val matching = mapOf("punishments" to listOf(row), "player_identity_domain" to listOf(mapOf<String, Any?>("id" to 1, "domain" to "network:online")))
            assertFailsWith<IllegalArgumentException> { DatabaseBackup.restore(db, mapOf("punishments" to listOf(row))) }
            assertFailsWith<IllegalArgumentException> { DatabaseBackup.restore(db, matching + ("player_identity_domain" to listOf(mapOf("id" to 1, "domain" to "other:online")))) }
            assertEquals(0L, db.count("punishments"))
            DatabaseBackup.restore(db, matching)
            assertEquals("2001:db8::1", db.query("SELECT uuid FROM punishments").single().string("uuid"))
            assertEquals(1L, db.count("player_identity_domain"))
            val dump = Files.createTempFile("identity-dump-", ".sql").toFile()
            try {
                DatabaseBackup.write(DatabaseBackup.snapshot(db), dump)
                val read = DatabaseBackup.read(dump)
                assertEquals("network:online", read.getValue("player_identity_domain").single()["domain"])
            } finally { dump.delete() }
        }
        Files.deleteIfExists(file)
    }
}
