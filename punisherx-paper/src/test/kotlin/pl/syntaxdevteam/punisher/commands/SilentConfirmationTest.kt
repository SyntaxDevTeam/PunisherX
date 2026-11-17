package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SilentConfirmationTest {
    private val plugin = mock<PunisherX>()
    private val sender = mock<CommandSender>()
    private val target = mock<Player>()
    private val observer = mock<Player>()
    private val source = mock<CommandSourceStack>()
    private val placeholders = mapOf("player" to "Steve", "time" to "10m", "reason" to "Spam", "id" to "42")

    init {
        whenever(source.sender).thenReturn(sender)
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.server).thenReturn(mock())
        whenever(plugin.server.onlinePlayers).thenReturn(listOf(target, observer))
        whenever(observer.hasPermission(any<String>())).thenReturn(true)
    }

    @Test fun `silent confirmation uses its own message and reaches only the operator`() {
        val confirmation = Component.text("Silent: Steve, 10m, Spam")
        val sections = listOf("ban", "banip", "mute", "jail", "warn", "kick", "unmute", "unjail", "unban")
        for (section in sections) {
            whenever(plugin.messageHandler.getSmartMessage(section, "silent_success", placeholders))
                .thenReturn(listOf(confirmation))
            PunishmentCommandUtils.sendSenderMessages(plugin, source, section, "normal", placeholders, silent = true)
            verify(plugin.messageHandler).getSmartMessage(section, "silent_success", placeholders)
            verify(plugin.messageHandler, never()).getSmartMessage(section, "normal", placeholders)
        }
        PunishmentCommandUtils.sendTargetMessages(plugin, target, "mute", "mute_message", placeholders, silent = true)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_MUTE, "mute", "broadcast", placeholders, silent = true)
        verify(sender, times(sections.size)).sendMessage(confirmation)
        verify(target, never()).sendMessage(any<Component>())
        verify(observer, never()).sendMessage(any<Component>())
        verifyNoMoreInteractions(plugin.messageHandler)
    }

    @Test fun `normal confirmation keeps its existing message key`() {
        val confirmation = Component.text("Normal confirmation")
        whenever(plugin.messageHandler.getSmartMessage("ban", "ban", placeholders)).thenReturn(listOf(confirmation))
        PunishmentCommandUtils.sendSenderMessages(plugin, source, "ban", "ban", placeholders)
        verify(sender).sendMessage(confirmation)
        verify(plugin.messageHandler).getSmartMessage("ban", "ban", placeholders)
        verifyNoMoreInteractions(plugin.messageHandler)
    }

    @Test fun `all bundled languages define private confirmations with punishment details`() {
        val sections = listOf("ban", "banip", "mute", "jail", "warn", "kick", "unmute", "unjail", "unban", "unwarn", "unpunish", "clear")
        for (language in listOf("pl", "en", "fr", "de", "pt", "es", "it", "ar", "ua", "nl", "ko", "ru")) {
            val stream = assertNotNull(javaClass.getResourceAsStream("/lang/messages_$language.yml"))
            val config = InputStreamReader(stream, Charsets.UTF_8).use { YamlConfiguration.loadConfiguration(it) }
            for (section in sections) {
                val message = assertNotNull(config.getString("$section.silent_success"), "$language/$section")
                assertTrue(message.contains("<player>"), "$language/$section player")
                if (section in listOf("ban", "banip", "mute", "jail", "warn")) {
                    assertTrue(message.contains("<time>"), "$language/$section duration")
                    assertTrue(message.contains("<reason>"), "$language/$section reason")
                }
            }
        }
    }
}
