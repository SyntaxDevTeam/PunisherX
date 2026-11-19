package pl.syntaxdevteam.punisher.config

import org.bukkit.Server
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class IdentityConfigurationPreflightTest {
    private fun checkConfig(content: String?, online: Boolean, verify: (ConfigurationValidationReport, File) -> Unit) {
        val folder = Files.createTempDirectory("punisherx-identity-preflight-").toFile()
        try {
            val file = File(folder, "config.yml")
            if (content != null) file.writeText(content)
            val plugin = mock<PunisherX>()
            val server = mock<Server>()
            whenever(plugin.dataFolder).thenReturn(folder)
            whenever(plugin.server).thenReturn(server)
            whenever(server.onlineMode).thenReturn(online)
            val result = ConfigurationPreflight(plugin).validate(includeLanguageFiles = false)
            verify(result, folder)
            if (content == null) assertFalse(file.exists()) else assertEquals(content, file.readText())
            assertFalse(File(folder, "config.yml.169.bak").exists())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test fun `legacy offline configuration requires an explicit realm before migration`() {
        checkConfig("config-version: 169\n", false) { report, _ ->
            assertFalse(report.valid)
            assertContains(report.compactMessage(), "identity.realm")
            assertContains(report.compactMessage(), "online-mode=false")
            assertContains(report.compactMessage(), "existing database")
        }
    }

    @Test fun `online auto mode accepts missing identity settings`() {
        checkConfig("config-version: 169\n", true) { report, _ -> assertTrue(report.valid) }
    }

    @Test fun `offline realm is accepted without editing configuration`() {
        checkConfig("identity:\n  realm: moonvale\n  authority: auto\n", false) { report, _ -> assertTrue(report.valid) }
    }

    @Test fun `trusted proxy requires explicit shared realm`() {
        checkConfig("identity:\n  authority: trusted-proxy\n", false) { report, _ -> assertFalse(report.valid) }
        checkConfig("identity:\n  authority: trusted-proxy\n  realm: network-a\n", false) { report, _ -> assertTrue(report.valid) }
    }

    @Test fun `invalid authority or realm is rejected`() {
        checkConfig("identity:\n  authority: online\n  realm: network-a\n", false) { report, _ -> assertFalse(report.valid) }
        checkConfig("identity:\n  authority: offline\n  realm: network-a\n", true) { report, _ -> assertFalse(report.valid) }
        checkConfig("identity:\n  authority: unknown\n", true) { report, _ -> assertFalse(report.valid) }
        checkConfig("identity:\n  realm: Bad Realm\n", false) { report, _ -> assertFalse(report.valid) }
    }

    @Test fun `missing config is reported without creating files`() {
        checkConfig(null, false) { report, folder ->
            assertFalse(report.valid)
            assertContains(report.compactMessage(), "Missing config.yml")
            assertTrue(folder.listFiles().orEmpty().isEmpty())
        }
    }

    @Test fun `malformed YAML remains untouched`() {
        checkConfig("identity: [\n", false) { report, _ -> assertFalse(report.valid) }
    }
}
