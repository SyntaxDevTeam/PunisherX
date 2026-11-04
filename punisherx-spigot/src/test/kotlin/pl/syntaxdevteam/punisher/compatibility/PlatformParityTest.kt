package pl.syntaxdevteam.punisher.compatibility

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Catches version-list and bundled-punishment drift between Paper and Spigot.
 * Platform-specific GUI dialog APIs and configuration comments are deliberately excluded.
 */
class PlatformParityTest {
    private fun repositoryRoot(): File =
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull {
                File(it, "punisherx-paper").isDirectory &&
                    File(it, "punisherx-spigot").isDirectory
            } ?: error("PunisherX repository root not found")

    @Test
    fun `Paper and Spigot recognize exactly the same Minecraft versions`() {
        val root = repositoryRoot()
        val regex = Regex("""SemanticVersion\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)""")
        fun versions(module: String): List<String> {
            val source = File(
                root,
                "punisherx-$module/src/main/kotlin/pl/syntaxdevteam/punisher/compatibility/VersionChecker.kt"
            ).readText()
            return regex.findAll(source).map { match ->
                match.value.replace(Regex("""\\s+"""), "")
            }.toList()
        }
        val paper = versions("paper")
        val spigot = versions("spigot")
        assertTrue(paper.isNotEmpty(), "Version checker must enumerate supported versions")
        assertEquals(paper, spigot, "Keep Spigot's supported versions in sync with Paper")
    }

    @Test
    fun `Paper and Spigot ship identical punishment escalation defaults`() {
        val root = repositoryRoot()
        fun templates(module: String): String =
            File(root, "punisherx-$module/src/main/resources/punish-templates.yml").readText()
        assertEquals(templates("paper"), templates("spigot"))
    }
}
