package pl.syntaxdevteam.punisher.gui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/** Prevent another panel from bypassing head resolution or inventory/session protection. */
class GuiArchitectureTest {
    @Test fun `all panel heads and opens go through the shared GUI lifecycle on both platforms`() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "punisherx-paper").isDirectory }
        for (module in listOf("paper", "spigot")) {
            val dir = File(root, "punisherx-$module/src/main/kotlin/pl/syntaxdevteam/punisher/gui")
            val files = dir.walkTopDown().filter { it.extension == "kt" }.toList()
            assertTrue(files.size > 20)
            files.filter { it.name != "BaseGUI.kt" }.forEach { file ->
                val text = file.readText()
                for (forbidden in listOf(".owningPlayer", ".ownerProfile", ".playerProfile", "createPlayerProfile(", "createProfile(", "setOwningPlayer(", "setPlayerProfile(", "setOwnerProfile(", "setOwner(", "gui.open(")) {
                    assertFalse(forbidden in text, "${file.name} bypasses BaseGUI: $forbidden")
                }
                if ("SkullMeta" in text) assertTrue("setPlayerHead(" in text, "${file.name} must use shared head binding")
            }
        }
    }
}
