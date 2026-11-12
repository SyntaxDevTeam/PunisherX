package pl.syntaxdevteam.punisher.gui.admin

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import kotlin.test.*

class ConfigGuiSettingsTest {
    private val config = YamlConfiguration()
    private val plugin = mock<PunisherX>()
    private val player = mock<Player>()
    private val settings = ConfigGuiSettings(plugin)
    init {
        whenever(plugin.config).thenReturn(config)
        whenever(plugin.guiRuntime).thenReturn(mock())
        whenever(plugin.databaseHandler).thenReturn(mock())
    }
    @Test fun `unauthorized clicks and delayed input do not modify or save configuration`() {
        ConfigGuiSettings.Toggle.entries.forEach { assertEquals(ConfigGuiSettings.Result.DENIED, settings.toggle(player, it)) }
        assertEquals(ConfigGuiSettings.Result.DENIED, settings.hiddenPlayers(player, "Target"))
        assertTrue(config.getKeys(true).isEmpty())
        verify(plugin, never()).saveConfig()
        verify(plugin.guiRuntime, never()).reset()
    }
    @Test fun `all switches preserve defaults toggle both ways save and invalidate stale panels`() {
        whenever(player.isOp).thenReturn(true)
        ConfigGuiSettings.Toggle.entries.forEach { option ->
            assertEquals(ConfigGuiSettings.Result.SAVED, settings.toggle(player, option))
            assertEquals(!option.default, config.getBoolean(option.path))
            assertEquals(ConfigGuiSettings.Result.SAVED, settings.toggle(player, option))
            assertEquals(option.default, config.getBoolean(option.path))
        }
        verify(plugin, times(10)).saveConfig()
        verify(plugin.guiRuntime, times(10)).reset()
    }
    @Test fun `jail permission cannot edit privacy`() {
        whenever(player.hasPermission("punisherx.manage.set_jail")).thenReturn(true)
        assertEquals(ConfigGuiSettings.Result.SAVED, settings.toggle(player, ConfigGuiSettings.Toggle.MUTE))
        assertEquals(ConfigGuiSettings.Result.DENIED, settings.toggle(player, ConfigGuiSettings.Toggle.IP))
    }
    @Test fun `countdown cannot change for active permanent or paused sentences`() {
        whenever(player.isOp).thenReturn(true)
        for (end in listOf(-1L, System.currentTimeMillis() + 600000)) {
            whenever(plugin.databaseHandler.getJailedPlayers(Int.MAX_VALUE, 0)).thenReturn(mutableListOf(
                PunishmentData(1, "uuid", "JAIL", "reason", 0, end, "Target", "Admin", "server")))
            assertEquals(ConfigGuiSettings.Result.ACTIVE_JAIL, settings.toggle(player, ConfigGuiSettings.Toggle.ONLINE))
        }
        verify(plugin, never()).saveConfig()
    }
    @Test fun `protected list validates deduplicates and supports clearing`() {
        whenever(player.isOp).thenReturn(true)
        val uuid = "65c3954d-b100-42f2-8353-11bfe20ed0ac"
        assertEquals(ConfigGuiSettings.Result.SAVED, settings.hiddenPlayers(player, "Target, target $uuid"))
        assertEquals(listOf("Target", uuid), config.getStringList("privacy.ip.hidden_players"))
        assertEquals(ConfigGuiSettings.Result.INVALID, settings.hiddenPlayers(player, "<red>invalid"))
        assertEquals(listOf("Target", uuid), config.getStringList("privacy.ip.hidden_players"))
        assertEquals(ConfigGuiSettings.Result.SAVED, settings.hiddenPlayers(player, "-"))
        assertTrue(config.getStringList("privacy.ip.hidden_players").isEmpty())
    }
    @Test fun `protected names containing s and whitespace separators are preserved`() {
        whenever(player.isOp).thenReturn(true)
        assertEquals(ConfigGuiSettings.Result.SAVED, settings.hiddenPlayers(player, "syntax Steve\tAlex, Test\nOther"))
        assertEquals(listOf("syntax", "Steve", "Alex", "Test", "Other"), config.getStringList("privacy.ip.hidden_players"))
    }
}
