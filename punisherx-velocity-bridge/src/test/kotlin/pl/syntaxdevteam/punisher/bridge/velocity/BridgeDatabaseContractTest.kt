package pl.syntaxdevteam.punisher.bridge.velocity

import org.slf4j.LoggerFactory
import pl.syntaxdevteam.syntaxdb.Database
import java.nio.file.Files
import java.util.UUID
import kotlin.test.*

class BridgeDatabaseContractTest {
    private val folder = Files.createTempDirectory("punisher-bridge-contract-").toFile()
    private val db = Database.connect("sqlite") { file = folder.resolve("bridge.sqlite").path }
    private val bridge = BridgeDatabase(LoggerFactory.getLogger(javaClass), db)
    private val uuid = UUID.randomUUID()
    private val text = "Zażółć 🐉 O'Brien;\nsecond line"

    @BeforeTest fun schema() {
        db.execute("CREATE TABLE bridge_events (id INTEGER PRIMARY KEY AUTOINCREMENT, action TEXT, target TEXT, reason TEXT, endTime BIGINT, processed INT DEFAULT 0, processedAt BIGINT)")
        db.execute("CREATE TABLE punishments (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT, reason TEXT, punishmentType TEXT, start BIGINT, endTime BIGINT, server TEXT)")
    }

    private fun ban(type: String = "BAN", scope: String = "network", start: Long = 100, end: Long = -1, reason: String = text) {
        db.execute("INSERT INTO punishments (uuid, reason, punishmentType, start, endTime, server) VALUES (?, ?, ?, ?, ?, ?)",
            listOf(uuid.toString(), reason, type, start, end, scope))
    }

    @AfterTest fun cleanup() { try { bridge.close() } finally { folder.deleteRecursively() } }

    @Test fun `pending events map every field and respect ID order and limit`() {
        repeat(3) { db.execute("INSERT INTO bridge_events (action, target, reason, endTime) VALUES (?, ?, ?, ?)", listOf("BANIP", "192.0.2.$it", text, -1)) }
        val events = bridge.fetchPendingEvents(2)
        assertEquals(listOf(1L, 2L), events.map { it.id })
        assertEquals(listOf("192.0.2.0", "192.0.2.1"), events.map { it.target })
        events.forEach { assertEquals("BANIP", it.action); assertEquals(text, it.reason); assertEquals(-1L, it.end) }
    }

    @Test fun `marking processed records timestamp and leaves other events pending`() {
        repeat(2) { db.execute("INSERT INTO bridge_events (action, target, reason, endTime) VALUES ('BAN', ?, ?, -1)", listOf(uuid.toString(), text)) }
        bridge.markProcessed(1)
        assertEquals(listOf(2L), bridge.fetchPendingEvents().map { it.id })
        val row = db.query("SELECT processed, processedAt FROM bridge_events WHERE id = 1").single()
        assertEquals(1, row.int("processed")); assertTrue(row.long("processedAt")!! > 0)
        bridge.markProcessed(999)
        assertEquals(2L, db.count("bridge_events"))
    }

    @Test fun `login lookup returns permanent network ban and keeps Unicode reason`() {
        ban()
        assertEquals(ActiveBan(text, -1), bridge.findActiveBan(uuid, 500))
    }

    @Test fun `login lookup rejects expired local and non BAN punishments`() {
        ban(end = 500); ban(scope = "survival"); ban(type = "BANIP"); ban(type = "MUTE")
        assertNull(bridge.findActiveBan(uuid, 500))
        assertNull(bridge.findActiveBan(UUID.randomUUID(), 0))
    }

    @Test fun `login lookup chooses newest active network ban`() {
        ban(start = 10, reason = "Older")
        ban(start = 20, end = 1000, reason = "Newer")
        ban(start = 30, end = 100, reason = "Expired")
        assertEquals(ActiveBan("Newer", 1000), bridge.findActiveBan(uuid, 500))
    }

    @Test fun `closed pool returns no events and bans and closes idempotently`() {
        bridge.close(); bridge.close()
        assertTrue(bridge.fetchPendingEvents().isEmpty())
        assertNull(bridge.findActiveBan(uuid))
        bridge.markProcessed(1)
        assertFalse(db.isReady)
    }
}
