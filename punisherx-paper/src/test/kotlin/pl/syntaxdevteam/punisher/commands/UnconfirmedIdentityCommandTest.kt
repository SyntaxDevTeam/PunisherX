package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.identity.*
import pl.syntaxdevteam.punisher.templates.PunishTemplate
import pl.syntaxdevteam.punisher.templates.PunishTemplateLevel
import kotlin.test.Test

class UnconfirmedIdentityCommandTest {
    @Test fun `unconfirmed ambiguous and unavailable targets abort moderation before punishment effects`() {
        for (failure in listOf(IdentityResolution.NotFound, IdentityResolution.Ambiguous, IdentityResolution.Unavailable)) {
            val plugin = mock<PunisherX>()
            val sender = mock<CommandSender>()
            val source = mock<CommandSourceStack>()
            whenever(source.sender).thenReturn(sender)
            whenever(sender.hasPermission(any<String>())).thenReturn(true)
            whenever(plugin.resolvePlayerUuid("Steve")).thenThrow(IdentityNotResolvedException(failure))
            whenever(plugin.messageHandler).thenReturn(mock())
            whenever(plugin.messageHandler.stringMessageToComponent(any(), any(), any<Map<String, String>>())).thenReturn(Component.text("Use UUID"))
            whenever(plugin.commandLoggerPlugin).thenReturn(mock())
            whenever(plugin.databaseHandler).thenReturn(mock())
            whenever(plugin.playerIPManager).thenReturn(mock())
            whenever(plugin.actionExecutor).thenReturn(mock())
            whenever(plugin.proxyBridgeMessenger).thenReturn(mock())
            whenever(plugin.safeTeleportService).thenReturn(mock())
            whenever(plugin.punishTemplateManager).thenReturn(mock())
            whenever(plugin.punishTemplateManager.getTemplate("spam")).thenReturn(
                PunishTemplate("spam", "Spam", mapOf(1 to PunishTemplateLevel(1, "BAN", "1d"))))
            val commands: List<Pair<BasicCommand, Array<String>>> = listOf(
                BanCommand(plugin) to arrayOf("Steve", "1d"),
                BanIpCommand(plugin) to arrayOf("Steve", "1d"),
                MuteCommand(plugin) to arrayOf("Steve", "1d"),
                WarnCommand(plugin) to arrayOf("Steve", "1d"),
                JailCommand(plugin) to arrayOf("Steve", "1d"),
                UnBanCommand(plugin) to arrayOf("Steve"),
                UnMuteCommand(plugin) to arrayOf("Steve"),
                UnWarnCommand(plugin) to arrayOf("Steve"),
                UnjailCommand(plugin) to arrayOf("Steve"),
                ClearAllCommand(plugin) to arrayOf("Steve"),
                CheckCommand(plugin, plugin.playerIPManager) to arrayOf("Steve", "all"),
                HistoryCommand(plugin, plugin.playerIPManager) to arrayOf("Steve"),
                UnpunishCommand(plugin) to arrayOf("Steve", "spam"),
                UnmarkCommand(plugin) to arrayOf("Steve"),
                NoteCommand(plugin) to arrayOf("list", "Steve"),
                PunishCommand(plugin) to arrayOf("Steve", "spam", "1")
            )
            commands.forEach { (command, args) -> command.execute(source, args) }
            verifyNoInteractions(plugin.databaseHandler, plugin.playerIPManager, plugin.actionExecutor,
                plugin.proxyBridgeMessenger, plugin.safeTeleportService, plugin.commandLoggerPlugin)
            verify(sender, times(commands.size)).sendMessage(Component.text("Use UUID"))
        }
    }
}
