package pl.syntaxdevteam.punisher.permissions

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.command.ConsoleCommandSender
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import java.nio.file.Files
import java.util.UUID
import kotlin.test.*

class IpVisibilityTest {
    private val folder = Files.createTempDirectory("ip-policy-").toFile()
    private val config = YamlConfiguration()
    private val plugin = mock<PunisherX>()
    private val viewer = mock<Player>()
    private val uuid = UUID.randomUUID()
    init {
        whenever(plugin.config).thenReturn(config)
        whenever(plugin.dataFolder).thenReturn(folder)
        whenever(plugin.server).thenReturn(mock())
        whenever(viewer.isOp).thenReturn(true)
    }
    @AfterTest fun cleanup() { folder.deleteRecursively() }
    private fun visible() = IpVisibility.canView(plugin, viewer, uuid, "Target")

    @Test fun `global switch overrides console and operator`() {
        config.set("privacy.ip.enabled", false)
        assertFalse(visible())
        assertFalse(IpVisibility.canView(plugin, mock<ConsoleCommandSender>(), uuid, "Target"))
    }
    @Test fun `protected name and uuid override operator`() {
        for (value in listOf("tArGeT", uuid.toString())) {
            config.set("privacy.ip.hidden_players", listOf(value))
            assertFalse(visible())
        }
    }
    @Test fun `restricted operator needs permission and normal operator retains access`() {
        assertTrue(visible())
        config.set("privacy.ip.restrict_operators", true)
        assertFalse(visible())
        whenever(viewer.isPermissionSet("punisherx.view_ip")).thenReturn(true)
        whenever(viewer.hasPermission("punisherx.view_ip")).thenReturn(true)
        assertTrue(visible())
        whenever(viewer.hasPermission("punisherx.view_ip")).thenReturn(false)
        assertFalse(visible())
    }
    @Test fun `online protection persists for offline target and can be removed`() {
        val target = mock<Player>()
        whenever(target.uniqueId).thenReturn(uuid)
        whenever(target.isPermissionSet("punisherx.privacy.hide_ip")).thenReturn(true)
        whenever(target.hasPermission("punisherx.privacy.hide_ip")).thenReturn(true)
        whenever(plugin.server.getPlayer(uuid)).thenReturn(target)
        assertFalse(visible())
        IpVisibility.rememberPreference(plugin, target)
        whenever(plugin.server.getPlayer(uuid)).thenReturn(null)
        assertFalse(visible())
        whenever(target.hasPermission("punisherx.privacy.hide_ip")).thenReturn(false)
        IpVisibility.rememberPreference(plugin, target)
        assertTrue(visible())
    }
    @Test fun `BANIP address masquerading as uuid or player name respects policy`() {
        val data = pl.syntaxdevteam.punisher.databases.PunishmentData(1, "192.0.2.1", "BANIP", "reason", 0, -1, "192.0.2.1", "Admin", "survival")
        config.set("privacy.ip.enabled", false)
        assertEquals("[hidden]", IpVisibility.displayField(plugin, viewer, data, data.uuid))
        assertEquals("[hidden]", IpVisibility.displayField(plugin, viewer, data, data.name))
        assertEquals("Admin", IpVisibility.displayField(plugin, viewer, data, data.operator))
        config.set("privacy.ip.enabled", true)
        whenever(plugin.playerIPManager).thenReturn(mock())
        whenever(plugin.playerIPManager.getAllDecryptedRecords()).thenReturn(emptyList())
        assertEquals(data.uuid, IpVisibility.displayField(plugin, viewer, data, data.uuid))
        whenever(plugin.playerIPManager.getAllDecryptedRecords()).thenReturn(listOf(
            pl.syntaxdevteam.punisher.players.PlayerIPManager.PlayerInfo("Target", uuid.toString(), data.uuid, "Earth", "now")))
        config.set("privacy.ip.hidden_players", listOf(uuid.toString()))
        assertEquals("[hidden]", IpVisibility.displayField(plugin, viewer, data, data.uuid))
    }
    @Test fun `bundled permission defaults cannot implicitly grant OP viewing or protection`() {
        val stream = javaClass.classLoader.getResourceAsStream("paper-plugin.yml")
            ?: javaClass.classLoader.getResourceAsStream("plugin.yml")!!
        val descriptor = stream.reader().use { YamlConfiguration.loadConfiguration(it) }
        assertEquals(false, descriptor.get("permissions.punisherx.view_ip.default"))
        assertEquals(false, descriptor.get("permissions.punisherx.privacy.hide_ip.default"))
    }
    @Test fun `shared address is hidden when another account is protected`() {
        whenever(plugin.playerIPManager).thenReturn(mock())
        val address = "192.0.2.10"
        whenever(plugin.playerIPManager.getAllDecryptedRecords()).thenReturn(listOf(
            pl.syntaxdevteam.punisher.players.PlayerIPManager.PlayerInfo("Target", uuid.toString(), address, "Earth", "now")))
        assertEquals(address, IpVisibility.displayAddress(plugin, viewer, address))
        config.set("privacy.ip.hidden_players", listOf("Target"))
        assertEquals("[hidden]", IpVisibility.displayAddress(plugin, viewer, address))
        config.set("privacy.ip.hidden_players", emptyList<String>())
        config.set("privacy.ip.enabled", false)
        assertEquals("[hidden]", IpVisibility.displayAddress(plugin, viewer, address))
    }
}
