package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import java.util.UUID
import kotlin.test.Test

class RemovalCommandSilentTest {
    private val plugin = mock<PunisherX>()
    private val source = mock<CommandSourceStack>()
    private val sender = mock<ConsoleCommandSender>()
    private val player = mock<Player>()
    private val uuid = UUID.randomUUID()

    init {
        whenever(source.sender).thenReturn(sender)
        whenever(sender.name).thenReturn("Admin")
        whenever(plugin.resolvePlayerUuid("Steve")).thenReturn(uuid)
        whenever(plugin.databaseHandler).thenReturn(mock())
        whenever(plugin.logger).thenReturn(mock())
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.commandLoggerPlugin).thenReturn(mock())
        whenever(plugin.cache).thenReturn(mock())
        whenever(plugin.messageHandler.getSmartMessage(any(), any(), any()))
            .thenReturn(listOf(Component.text("Silent confirmation")))
        whenever(plugin.messageHandler.stringMessageToComponent(any(), any(), any<Map<String, String>>()))
            .thenReturn(Component.text("Silent confirmation"))
    }

    private fun punishment(type: String, target: String = uuid.toString()) =
        PunishmentData(42, target, type, "Spam", 100, -1, "Steve", "Admin", "network")

    private fun assertPrivateConfirmation(section: String) {
        verify(sender).sendMessage(Component.text("Silent confirmation"))
        verify(player, never()).sendMessage(any<Component>())
        if (section in listOf("unwarn", "clear")) {
            verify(plugin.messageHandler).stringMessageToComponent(eq(section), eq("silent_success"), any<Map<String, String>>())
        } else {
            verify(plugin.messageHandler).getSmartMessage(eq(section), eq("silent_success"), any())
        }
        verifyNoMoreInteractions(plugin.messageHandler)
    }

    @Test fun `silent unmute still removes the active mute`() {
        whenever(plugin.databaseHandler.getPunishments(uuid.toString())).thenReturn(listOf(punishment("MUTE")))
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<Player?> { Bukkit.getPlayer("Steve") }.thenReturn(player)
            UnMuteCommand(plugin).execute(source, arrayOf("Steve", "--silent"))
        }
        verify(plugin.databaseHandler).removePunishment(uuid.toString(), "MUTE", false)
        assertPrivateConfirmation("unmute")
    }

    @Test fun `silent unwarn still removes warnings`() {
        whenever(plugin.databaseHandler.getPunishments(uuid.toString())).thenReturn(listOf(punishment("WARN")))
        UnWarnCommand(plugin).execute(source, arrayOf("Steve", "--silent"))
        verify(plugin.databaseHandler).removePunishment(uuid.toString(), "WARN", false)
        assertPrivateConfirmation("unwarn")
    }

    @Test fun `silent unban removes player ban without broadcasting`() {
        whenever(plugin.databaseHandler.getPunishments(uuid.toString())).thenReturn(listOf(punishment("BAN")))
        UnBanCommand(plugin).execute(source, arrayOf("Steve", "--silent"))
        verify(plugin.databaseHandler).removePunishment(uuid.toString(), "BAN", false)
        assertPrivateConfirmation("unban")
    }

    @Test fun `silent unban removes IP ban without broadcasting`() {
        val ip = "192.0.2.1"
        whenever(plugin.databaseHandler.getPunishmentsByIP(ip)).thenReturn(listOf(punishment("BANIP", ip)))
        UnBanCommand(plugin).execute(source, arrayOf(ip, "--silent"))
        verify(plugin.databaseHandler).removePunishment(ip, "BANIP", false)
        assertPrivateConfirmation("unban")
    }

    @Test fun `silent unjail releases an offline prisoner without announcements`() {
        whenever(plugin.cache.isPlayerInCache(uuid)).thenReturn(true)
        mockStatic(Bukkit::class.java).use {
            UnjailCommand(plugin).execute(source, arrayOf("Steve", "--silent"))
        }
        verify(plugin.cache).removePunishment(uuid, teleportPlayer = false, notify = false, removeDatabase = true)
        assertPrivateConfirmation("unjail")
    }

    @Test fun `silent clearall confirms privately without looking up or notifying the target`() {
        whenever(plugin.databaseHandler.getPunishments(uuid.toString()))
            .thenReturn(listOf(punishment("MUTE"), punishment("WARN")))
        mockStatic(Bukkit::class.java).use { bukkit ->
            ClearAllCommand(plugin).execute(source, arrayOf("Steve", "--silent"))
            bukkit.verifyNoInteractions()
        }
        verify(plugin.databaseHandler).removePunishment(uuid.toString(), "MUTE", true)
        verify(plugin.databaseHandler).removePunishment(uuid.toString(), "WARN", true)
        assertPrivateConfirmation("clear")
    }
}
