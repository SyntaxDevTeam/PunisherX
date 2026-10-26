package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.templates.PunishTemplate
import pl.syntaxdevteam.punisher.templates.PunishTemplateLevel
import java.util.UUID
import kotlin.test.Test

class PunishCommandFlagsTest {
    private class Fixture(type: String) {
        val plugin = mock<PunisherX>()
        val source = mock<CommandSourceStack>()
        val sender = mock<ConsoleCommandSender>()
        val player = mock<Player>()
        val observer = mock<Player>()
        val world = mock<World>()
        val uuid = UUID.randomUUID()
        val template = PunishTemplate("spam", "Spam", mapOf(1 to PunishTemplateLevel(1, type, "10m")))

        init {
            whenever(source.sender).thenReturn(sender)
            whenever(sender.name).thenReturn("Admin")
            whenever(player.name).thenReturn("Steve")
            whenever(player.location).thenReturn(Location(world, 0.0, 64.0, 0.0))
            whenever(player.hasPermission("punisherx.bypass")).thenReturn(true)
            whenever(observer.name).thenReturn("Observer")
            whenever(observer.hasPermission("punisherx.see")).thenReturn(true)
            whenever(plugin.logger).thenReturn(mock())
            whenever(plugin.config).thenReturn(YamlConfiguration().apply { set("jail.location.world", "world") })
            whenever(plugin.punishTemplateManager).thenReturn(mock())
            whenever(plugin.punishTemplateManager.getTemplate("spam")).thenReturn(template)
            whenever(plugin.resolvePlayerUuid("Steve")).thenReturn(uuid)
            whenever(plugin.databaseHandler).thenReturn(mock())
            DatabaseHandler::class.java.getDeclaredField("plugin").apply { isAccessible = true }
                .set(plugin.databaseHandler, plugin)
            whenever(plugin.databaseHandler.addPunishment(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(42L)
            whenever(plugin.messageHandler).thenReturn(mock())
            whenever(plugin.messageHandler.getSmartMessage(any(), any(), any())).thenReturn(listOf(Component.text("Notice")))
            whenever(plugin.messageHandler.stringMessageToComponent(any(), any(), any<Map<String, String>>()))
                .thenReturn(Component.text("Notice"))
            whenever(plugin.timeHandler).thenReturn(mock())
            whenever(plugin.timeHandler.parseTime("10m")).thenReturn(600)
            whenever(plugin.timeHandler.formatTime(anyOrNull())).thenReturn("10 minutes")
            whenever(plugin.actionExecutor).thenReturn(mock())
            whenever(plugin.proxyBridgeMessenger).thenReturn(mock())
            whenever(plugin.playerIPManager).thenReturn(mock())
            whenever(plugin.playerIPManager.getPlayerIPsByUUID(uuid.toString())).thenReturn(listOf("192.0.2.1"))
            whenever(plugin.server).thenReturn(mock())
            whenever(plugin.server.onlinePlayers).thenReturn(listOf(player, observer))
            whenever(plugin.cache).thenReturn(mock())
            whenever(plugin.safeTeleportService).thenReturn(mock())
            val teleportService = plugin.safeTeleportService
            doAnswer { invocation -> invocation.getArgument<(Boolean) -> Unit>(2)(true); null }
                .whenever(teleportService).teleportSafely(eq(player), any(), any())
        }

        fun execute(vararg options: String) {
            mockStatic(Bukkit::class.java).use { bukkit ->
                bukkit.`when`<Player?> { Bukkit.getPlayer(uuid) }.thenReturn(player)
                bukkit.`when`<World?> { Bukkit.getWorld("world") }.thenReturn(world)
                PunishCommand(plugin).execute(source, arrayOf("Steve", "spam", *options))
            }
        }
    }

    @Test fun `silent alone respects immunity for all template punishment types`() {
        listOf("BAN", "BANIP", "MUTE", "WARN", "KICK", "JAIL").forEach { type ->
            val f = Fixture(type)
            f.execute("--silent")
            verify(f.plugin.messageHandler).stringMessageToComponent("error", "bypass", mapOf("player" to "Steve"))
            verify(f.plugin.databaseHandler, never()).addPunishment(any(), any(), any(), any(), any(), any(), any(), any())
            verifyNoInteractions(f.plugin.actionExecutor)
        }
    }

    @Test fun `force and silent confirm privately for all template types without target or staff chat`() {
        listOf("BAN", "BANIP", "MUTE", "WARN", "KICK", "JAIL").forEach { type ->
            val f = Fixture(type)
            f.execute("--force", "1", "--silent")
            verify(f.plugin.databaseHandler).addTemplatePunishmentHistory(eq("Steve"), any(), eq("Spam"), eq("Admin"),
                eq(type), any(), any(), eq("spam"), eq(f.uuid.toString()), any())
            if (type != "KICK") {
                verify(f.plugin.databaseHandler).addPunishment(eq("Steve"), any(), eq("Spam"), eq("Admin"),
                    eq(type), any(), any(), any())
            }
            if (type in listOf("BAN", "BANIP", "KICK")) verify(f.player).kick(any<Component>())
            if (type == "JAIL") verify(f.plugin.cache).addOrUpdatePunishment(eq(f.uuid), any(), anyOrNull())
            verify(f.sender).sendMessage(Component.text("Notice"))
            verify(f.plugin.messageHandler).getSmartMessage(eq(type.lowercase()), eq("silent_success"),
                argThat { get("player") == "Steve" && get("reason") == "Spam" && get("time") == "10 minutes" })
            verify(f.player, never()).sendMessage(any<Component>())
            verify(f.observer, never()).sendMessage(any<Component>())
            verify(f.plugin.messageHandler, never()).stringMessageToComponent(eq("punish"), eq("invalid_level"), any<Map<String, String>>())
        }
    }

    @Test fun `force without silent keeps template chat announcements`() {
        val f = Fixture("MUTE")
        f.execute("--force")
        verify(f.sender).sendMessage(Component.text("Notice"))
        verify(f.player, atLeastOnce()).sendMessage(Component.text("Notice"))
        verify(f.observer).sendMessage(Component.text("Notice"))
    }
}
