package pl.syntaxdevteam.punisher.permissions

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mockito.kotlin.*
import kotlin.test.*

class PunishmentReadPermissionsTest {
    @Test fun `bundled defaults deny ordinary players access through current and legacy permissions`() {
        val stream = javaClass.classLoader.getResourceAsStream("paper-plugin.yml")
            ?: javaClass.classLoader.getResourceAsStream("plugin.yml")!!
        val descriptor = stream.reader().use { YamlConfiguration.loadConfiguration(it) }
        val player = mock<Player>()
        whenever(player.hasPermission(any<String>())).thenAnswer { invocation ->
            descriptor.get("permissions.${invocation.getArgument<String>(0)}.default") == true
        }

        for (node in listOf("punisherx.cmd.check", "punisherx.check", "punisherx.cmd.history")) {
            assertEquals("op", descriptor.get("permissions.$node.default"), node)
        }
        assertFalse(PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.CHECK))
        assertFalse(PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.HISTORY))

        whenever(player.hasPermission("punisherx.cmd.check")).thenReturn(true)
        assertTrue(PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.CHECK))
        assertFalse(PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.HISTORY))
        whenever(player.hasPermission("punisherx.cmd.history")).thenReturn(true)
        assertTrue(PermissionChecker.hasWithLegacy(player, PermissionChecker.PermissionKey.HISTORY))
    }
}
