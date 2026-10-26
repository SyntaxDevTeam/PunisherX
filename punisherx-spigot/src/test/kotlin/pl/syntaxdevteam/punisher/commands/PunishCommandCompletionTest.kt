package pl.syntaxdevteam.punisher.commands

import kotlin.test.Test
import kotlin.test.assertEquals

class PunishCommandCompletionTest {
    private val names = listOf("Spam", "Scam", "Chat spam", "Griefing", "Zażółć", "A\"B", "Path\\Name")
    private fun suggest(input: String): List<String> = PunishCommandCompletion.suggest(
        input.split(' ').toTypedArray(), listOf("Steve", "Alex"), names
    ) { if (it.equals("Spam", true) || it == "Chat spam") listOf(1, 3, 12) else emptyList() }

    @Test fun `templates match typed prefixes without forced quotes`() {
        assertEquals(listOf("Scam", "Spam"), PunishCommandCompletion.templates(names, "s"))
        assertEquals(listOf("Spam"), suggest("Steve sp"))
        assertEquals(emptyList(), suggest("Steve unknown"))
        assertEquals(listOf("\"Spam\""), suggest("Steve \"sp"))
    }

    @Test fun `quoted names round trip through basic command arguments`() {
        for (name in names) {
            val completion = PunishCommandCompletion.templates(listOf(name), "").single()
            assertEquals(listOf("Steve", name, "3", "--silent"),
                PunishCommandCompletion.arguments(arrayOf("Steve", *completion.split(' ').toTypedArray(), "3", "--silent")).toList())
        }
        assertEquals(listOf("\"Chat spam\""), suggest("Steve Ch"))
        assertEquals(listOf("spam\""), suggest("Steve \"Chat s"))
        assertEquals(listOf("spam\""), suggest("Steve \"Chat "))
    }

    @Test fun `levels come from the selected template and flags do not repeat`() {
        assertEquals(listOf("1", "3", "12", "--force", "--silent"), suggest("Steve Spam "))
        assertEquals(listOf("1", "12"), suggest("Steve Spam 1"))
        assertEquals(listOf("1", "3", "12", "--silent"), suggest("Steve Spam --force "))
        assertEquals(listOf("--force"), suggest("Steve Spam --silent 3 --"))
        assertEquals(emptyList(), suggest("Steve Spam --silent 3 --force "))
        assertEquals(listOf("1", "3", "12", "--force", "--silent"), suggest("Steve \"Chat spam\" "))
    }

    @Test fun `player prefixes are filtered and configured names are read each time`() {
        assertEquals(listOf("Steve"), suggest("st"))
        assertEquals(listOf("Custom"), PunishCommandCompletion.templates(listOf("Custom"), "c"))
        assertEquals(emptyList(), PunishCommandCompletion.templates(emptyList(), ""))
    }
}
