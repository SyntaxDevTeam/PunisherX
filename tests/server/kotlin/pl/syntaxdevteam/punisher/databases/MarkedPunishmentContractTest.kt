package pl.syntaxdevteam.punisher.databases

import kotlin.test.*

/** Real SQLite transactions, exercised by both server modules. */
class MarkedPunishmentContractTest : SyntaxDbTestFixture() {
    @BeforeTest fun prepare() = start()

    private fun marked(type: String = "BAN", mark: String = "speed", target: String = uuid.toString(),
                       player: String = uuid.toString(), template: String? = null): Long? =
        handler.addMarkedPunishment("Target", target, reason, "AntiCheat", type, 100, -1, mark, player, template)

    @Test fun `reversal keeps manual punishments even with identical event fields and independent ids`() {
        marked()
        handler.addPunishment("Target", uuid.toString(), reason, "AntiCheat", "BAN", 100, -1)
        assertEquals(1, handler.undoMarkedPunishments(uuid.toString(), null, "Admin")!!.size)
        assertEquals(1, count("punishments"))
        assertEquals(1, count("punishmenthistory"))
        assertEquals(1, count("punishment_undos"))
        assertTrue(handler.undoMarkedPunishments(null, null, "Admin")!!.isEmpty())
    }

    @Test fun `player and check filters only reverse their own entries`() {
        marked(mark = "speed")
        marked(mark = "fly")
        marked(mark = "speed", target = "other", player = "other")
        assertEquals(1, handler.undoMarkedPunishments(uuid.toString(), "SPEED", "Admin")!!.size)
        assertEquals(2, count("punishments"))
        assertEquals(1, handler.undoMarkedPunishments(null, "speed", "Admin")!!.size)
        assertEquals(1, count("punishments"))
    }

    @Test fun `pause reverses all types and prevents future writes until explicitly resumed`() {
        for (type in listOf("BAN", "BANIP", "MUTE", "WARN", "JAIL", "KICK")) marked(type)
        assertEquals(6, handler.undoMarkedPunishments(null, "speed", "Admin", pause = true)!!.size)
        assertTrue(handler.isMarkPaused("SPEED"))
        assertNull(marked())
        assertEquals(0, count("punishments"))
        assertEquals(6, count("punishmenthistory"))
        assertNotNull(marked(mark = "fly"))
        assertTrue(handler.resumeMark("speed"))
        assertFalse(handler.isMarkPaused("speed"))
        assertNotNull(marked())
    }

    @Test fun `pauses survive reconnecting and recreating the schema`() {
        handler.undoMarkedPunishments(null, "speed", "Admin", pause = true)
        handler.closeConnection()
        val reopened = DatabaseHandler(plugin)
        try {
            reopened.openConnection(); reopened.createTables()
            assertTrue(reopened.isMarkPaused("speed"))
            assertNull(reopened.addMarkedPunishment("Target", uuid.toString(), reason, "AC", "BAN", 2, -1, "speed"))
        } finally { reopened.closeConnection() }
    }

    @Test fun `local scope cannot reverse or resume a foreign scope but network pause covers all servers`() {
        config.set("server", "creative"); marked()
        handler.undoMarkedPunishments(null, "speed", "Admin", pause = true)
        config.set("server", "survival")
        assertFalse(handler.isMarkPaused("speed"))
        assertTrue(handler.undoMarkedPunishments(null, "speed", "Admin")!!.isEmpty())
        assertNotNull(marked())
        config.set("server", "network")
        assertTrue(handler.isMarkPaused("speed"))
        assertEquals(1, handler.undoMarkedPunishments(null, "speed", "Admin", pause = true)!!.size)
        config.set("server", "survival")
        assertTrue(handler.resumeMark("speed"))
        assertTrue(handler.isMarkPaused("speed"))
        assertNull(marked())
    }

    @Test fun `failure writing metadata rolls back both active punishment and history`() {
        sql("CREATE TRIGGER fail_mark BEFORE INSERT ON punishment_marks BEGIN SELECT RAISE(ABORT, 'injected'); END")
        assertNull(marked())
        assertEquals(0, count("punishments")); assertEquals(0, count("punishmenthistory"))
    }

    @Test fun `failed reversal rolls back pause deletion and undo audit`() {
        marked()
        sql("CREATE TRIGGER fail_delete BEFORE DELETE ON punishments BEGIN SELECT RAISE(ABORT, 'injected'); END")
        assertNull(handler.undoMarkedPunishments(null, "speed", "Admin", pause = true))
        assertFalse(handler.isMarkPaused("speed"))
        assertEquals(1, count("punishments")); assertEquals(0, count("punishment_undos"))
    }

    @Test fun `template reversal removes escalation contribution and multi IP bans use player identity`() {
        marked("BANIP", target = "192.0.2.1", template = "cheating")
        marked("BANIP", target = "192.0.2.2", template = "cheating")
        assertEquals(1, handler.countEscalationHistory(uuid.toString(), "Renamed", reason, "cheating"))
        assertEquals(2, handler.undoMarkedPunishments(uuid.toString(), "speed", "Admin")!!.size)
        assertEquals(0, handler.countEscalationHistory(uuid.toString(), "Renamed", reason, "cheating"))
        assertEquals(0, count("punishments"))
    }

    @Test fun `queued proxy bans disappear only when no other active ban remains`() {
        marked()
        handler.enqueueBridgeEvent("BAN", uuid.toString(), reason, -1)
        handler.undoMarkedPunishments(null, null, "Admin")
        assertEquals(0, count("bridge_events"))
        marked()
        add(type = "BAN")
        handler.enqueueBridgeEvent("BAN", uuid.toString(), reason, -1)
        handler.undoMarkedPunishments(null, null, "Admin")
        assertEquals(1, count("bridge_events"))
    }

    @Test fun `backup roundtrip retains marks pauses and undo capability`() {
        marked(mark = "fly")
        handler.undoMarkedPunishments(null, "speed", "Admin", pause = true)
        handler.exportDatabase()
        sql("DELETE FROM punishment_marks"); sql("DELETE FROM punishments"); sql("DELETE FROM punishmenthistory")
        sql("DELETE FROM punishment_mark_pauses")
        handler.importDatabase()
        assertTrue(handler.isMarkPaused("speed"))
        assertEquals(1, handler.undoMarkedPunishments(null, "fly", "Admin")!!.size)
        assertEquals(0, count("punishments"))
    }

    @Test fun `deleting a player also removes metadata and retains other players`() {
        marked()
        marked(target = "other", player = "other")
        handler.deletePlayerData(uuid.toString())
        assertEquals(1, count("punishment_marks"))
        assertEquals(1, handler.undoMarkedPunishments(null, null, "Admin")!!.size)
    }
}
