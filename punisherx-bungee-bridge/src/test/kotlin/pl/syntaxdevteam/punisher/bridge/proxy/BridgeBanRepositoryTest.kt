package pl.syntaxdevteam.punisher.bridge.proxy

import pl.syntaxdevteam.syntaxdb.Database
import java.nio.file.Files
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BridgeBanRepositoryTest {
    private val folder = Files.createTempDirectory("punisherx-bungee-security-").toFile()
    private val db = Database.connect("sqlite") { file = folder.resolve("bridge.sqlite").path }
    private val uuid = UUID.randomUUID()

    @BeforeTest fun schema() {
        db.execute("""CREATE TABLE punishments (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            uuid TEXT, reason TEXT, punishmentType TEXT, start BIGINT, endTime BIGINT, server TEXT
        )""")
    }

    @AfterTest fun cleanup() {
        try { db.close() } finally { folder.deleteRecursively() }
    }

    private fun add(target: String, type: String, scope: String = "network",
                    reason: String = "security-check", end: Long = -1L, start: Long = 1L) {
        db.execute("INSERT INTO punishments (uuid, reason, punishmentType, start, endTime, server) VALUES (?, ?, ?, ?, ?, ?)",
            listOf(target, reason, type, start, end, scope))
    }

    private fun lookup(vararg ids: ConnectionIdentity): Map<UUID, ActiveBan> =
        BridgeBanRepository.findActiveBans(db, ids.toList())

    @Test fun `network UUID and IP bans are enforced only while persisted and active`() {
        val second = UUID.randomUUID()
        add(uuid.toString(), "BAN", reason = "uuid")
        add("198.51.100.7", "BANIP", reason = "ip")
        val bans = lookup(ConnectionIdentity(uuid, "203.0.113.8"),
            ConnectionIdentity(second, "198.51.100.7"))
        assertEquals(ActiveBan("uuid", -1, "BAN"), bans[uuid])
        assertEquals(ActiveBan("ip", -1, "BANIP"), bans[second])
        db.execute("DELETE FROM punishments")
        assertTrue(lookup(ConnectionIdentity(uuid, "203.0.113.8")).isEmpty())
    }

    @Test fun `local expired and future bans cannot disconnect network users`() {
        add(uuid.toString(), "BAN", scope = "survival", reason = "local")
        add(uuid.toString(), "BAN", end = 1, reason = "expired")
        add(uuid.toString(), "BAN", start = System.currentTimeMillis() + 60_000L,
            reason = "future")
        assertNull(lookup(ConnectionIdentity(uuid, "203.0.113.8"))[uuid])
    }

    @Test fun `two proxies receive the same SQL result without consuming a global event`() {
        add(uuid.toString(), "BAN")
        val identities = listOf(ConnectionIdentity(uuid, "203.0.113.8"))
        val first = BridgeBanRepository.findActiveBans(db, identities)
        val second = BridgeBanRepository.findActiveBans(db, identities)
        assertEquals(first, second)
        assertTrue(first.containsKey(uuid))
    }

    @Test fun `batch size above 128 is handled without unbounded SQL placeholders`() {
        add(uuid.toString(), "BAN")
        val others = (1..129).map { ConnectionIdentity(UUID.randomUUID(), "198.51.100.1") }
        val result = BridgeBanRepository.findActiveBans(db, others + ConnectionIdentity(uuid, "198.51.100.2"))
        assertEquals(setOf(uuid), result.keys)
    }

    @Test fun `SQL failure never looks like an empty ban list`() {
        db.execute("DROP TABLE punishments")
        assertFails { lookup(ConnectionIdentity(uuid, "203.0.113.8")) }
    }
}
