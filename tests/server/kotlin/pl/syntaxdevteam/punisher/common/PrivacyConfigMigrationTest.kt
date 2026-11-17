package pl.syntaxdevteam.punisher.common

import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import java.nio.file.Files
import kotlin.test.*

class PrivacyConfigMigrationTest {
    @Test fun `168 migration adds privacy and jail defaults preserving choices and creates backup`() {
        val folder = Files.createTempDirectory("privacy-migration-").toFile()
        try {
            folder.resolve("config.yml").writeText("config-version: 168\njail:\n  restrictions:\n    mute: false\nprivacy:\n  ip:\n    enabled: false\n")
            val plugin = mock<PunisherX>()
            whenever(plugin.dataFolder).thenReturn(folder)
            whenever(plugin.logger).thenReturn(mock())
            whenever(plugin.getResource("config.yml")).thenAnswer { javaClass.classLoader.getResourceAsStream("config.yml") }
            val manager = ConfigManager(plugin)
            manager.load()
            assertEquals(169, manager.config.getInt("config-version"))
            assertFalse(manager.config.getBoolean("jail.restrictions.mute"))
            assertFalse(manager.config.getBoolean("privacy.ip.enabled"))
            assertTrue(manager.config.getBoolean("jail.restrictions.commands"))
            assertFalse(manager.config.getBoolean("jail.online_time_only"))
            assertFalse(manager.config.getBoolean("privacy.ip.restrict_operators"))
            assertTrue(manager.config.contains("privacy.ip.hidden_players"))
            assertTrue(folder.resolve("config.yml.168.bak").exists())
            manager.load()
            assertFalse(manager.config.getBoolean("privacy.ip.enabled"))
        } finally { folder.deleteRecursively() }
    }
}
