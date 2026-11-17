package pl.syntaxdevteam.punisher.commands

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PunishmentCommandOptionsTest {
    @Test fun `flags can surround a time or a level without becoming positional arguments`() {
        listOf(
            arrayOf("Steve", "--silent", "10m", "Spam", "--force"),
            arrayOf("--force", "Steve", "10m", "--silent", "Spam")
        ).forEach {
            val options = PunishmentCommandOptions.parse(it)
            assertEquals(listOf("Steve", "10m", "Spam"), options.arguments.toList())
            assertTrue(options.force)
            assertTrue(options.silent)
        }
        val options = PunishmentCommandOptions.parse(arrayOf("Steve", "Chat spam", "--force", "2", "--silent"))
        assertEquals(listOf("Steve", "Chat spam", "2"), options.arguments.toList())
    }

    @Test fun `only exact flags are reserved and flags do not leak between invocations`() {
        val original = arrayOf("Steve", "10m", "reason--silent", "--silent")
        assertTrue(PunishmentCommandOptions.parse(original).silent)
        assertEquals("--silent", original.last())
        val next = PunishmentCommandOptions.parse(arrayOf("Steve", "reason--silent", "--unknown"))
        assertFalse(next.silent)
        assertFalse(next.force)
        assertEquals(listOf("Steve", "reason--silent", "--unknown"), next.arguments.toList())
    }
    @Test fun `mark defaults and named check are invocation local and stripped from reasons`() {
        val named = PunishmentCommandOptions.parse(arrayOf("Target", "--mark=Grim.Speed", "10m", "reason", "--silent"))
        assertEquals("grim.speed", named.mark)
        assertEquals(listOf("Target", "10m", "reason"), named.arguments.toList())
        assertTrue(named.silent)
        assertEquals("default", PunishmentCommandOptions.parse(arrayOf("--mark", "Target", "reason")).mark)
        assertEquals(null, PunishmentCommandOptions.parse(arrayOf("Target", "reason")).mark)
    }
}
