package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import java.util.UUID
import kotlin.test.Test

class BanIpCommandTest {
    private val plugin = mock<PunisherX>()
    private val sender = mock<ConsoleCommandSender>()
    private val source = mock<CommandSourceStack>()
    private val player = mock<Player>()
    private val uuid = UUID.randomUUID()
    private val ips = listOf("192.0.2.1", "192.0.2.2")
    private val reason = "Toksyczna atmosfera w społeczności"

    private fun execute(firstId: Long?, secondId: Long?) {
        whenever(plugin.logger).thenReturn(mock())
        whenever(plugin.commandLoggerPlugin).thenReturn(mock())
        whenever(plugin.playerIPManager).thenReturn(mock())
        whenever(plugin.databaseHandler).thenReturn(mock())
        // Kotlin's default server argument invokes a private method on the database mock.
        DatabaseHandler::class.java.getDeclaredField("plugin").apply { isAccessible = true }
            .set(plugin.databaseHandler, plugin)
        whenever(plugin.config).thenReturn(YamlConfiguration())
        whenever(plugin.proxyBridgeMessenger).thenReturn(mock())
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.timeHandler).thenReturn(mock())
        whenever(plugin.actionExecutor).thenReturn(mock())
        whenever(plugin.server).thenReturn(mock())
        whenever(plugin.server.onlinePlayers).thenReturn(emptyList())
        whenever(source.sender).thenReturn(sender)
        whenever(sender.name).thenReturn("Admin")
        whenever(plugin.playerIPManager.getPlayerIPsByName("target")).thenReturn(ips)
        whenever(plugin.resolvePlayerUuid("target")).thenReturn(uuid)
        whenever(plugin.timeHandler.parseTime("999d")).thenReturn(86_313_600L)
        whenever(plugin.timeHandler.formatTime("999d")).thenReturn("999 dni")
        whenever(plugin.messageHandler.stringMessageToComponent(eq("error"), eq("banip_db_error"), any<Map<String, String>>()))
            .thenReturn(Component.text("Database error"))
        whenever(plugin.messageHandler.getSmartMessage(any(), any(), any()))
            .thenReturn(listOf(Component.text("Banned")))
        whenever(plugin.databaseHandler.addPunishment(any(), eq(ips[0]), any(), any(), any(), any(), any(), any()))
            .thenReturn(firstId)
        whenever(plugin.databaseHandler.addPunishment(any(), eq(ips[1]), any(), any(), any(), any(), any(), any()))
            .thenReturn(secondId)
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<Player?> { Bukkit.getPlayer(uuid) }.thenReturn(player)
            BanIpCommand(plugin).execute(source, arrayOf("target", "999d", reason))
            bukkit.verify { Bukkit.getPlayer(uuid) }
            // In particular, the command must never dispatch vanilla's permanent ban-ip.
            bukkit.verifyNoMoreInteractions()
        }
    }

    @Test
    fun `failed writes do not ban kick publish or announce success`() {
        execute(null, null)
        verifyNoInteractions(plugin.proxyBridgeMessenger, plugin.actionExecutor, plugin.commandLoggerPlugin)
        verify(plugin.databaseHandler, never()).addPunishmentHistory(any(), any(), any(), any(), any(), any(), any(), any())
        verify(player, never()).kick(any<Component>())
        verify(plugin.messageHandler, never()).getSmartMessage(any(), any(), any())
        verify(plugin.messageHandler).stringMessageToComponent("error", "banip_db_error",
            mapOf("failed_ips" to ips.joinToString(", "), "banned_ips" to "-"))
    }

    @Test
    fun `partial failure publishes only persisted IP and reports both outcomes`() {
        execute(42, null)
        verify(plugin.proxyBridgeMessenger).notifyIpBan(eq(ips[0]), eq(reason), any())
        verify(plugin.proxyBridgeMessenger, never()).notifyIpBan(eq(ips[1]), any(), any())
        verify(plugin.databaseHandler, never()).addPunishmentHistory(any(), eq(ips[1]), any(), any(), any(), any(), any(), any())
        verifyNoInteractions(plugin.actionExecutor, plugin.commandLoggerPlugin)
        verify(plugin.messageHandler, never()).getSmartMessage(any(), any(), any())
        verify(plugin.messageHandler).stringMessageToComponent("error", "banip_db_error",
            mapOf("failed_ips" to ips[1], "banned_ips" to ips[0]))
    }

    @Test
    fun `successful bans preserve expiration and announce success`() {
        execute(42, 43)
        val starts = argumentCaptor<Long>()
        val ends = argumentCaptor<Long>()
        verify(plugin.databaseHandler, times(2)).addPunishment(eq("target"), any(), eq(reason), eq("Admin"),
            eq("BANIP"), starts.capture(), ends.capture(), any())
        starts.allValues.zip(ends.allValues).forEach { (start, end) ->
            kotlin.test.assertEquals(86_313_600_000L, end - start)
        }
        verify(plugin.proxyBridgeMessenger, times(2)).notifyIpBan(any(), eq(reason), any())
        verify(player).kick(any<Component>())
        verify(plugin.actionExecutor).executeAction(eq("ip_banned"), eq("target"), any())
        verify(plugin.messageHandler, never()).stringMessageToComponent(eq("error"), eq("banip_db_error"), any<Map<String, String>>())
    }
}
