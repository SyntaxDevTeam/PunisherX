package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.platform.SchedulerAdapter
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GuiWarnMessageTest {
    @Test fun `GUI warning count reaches sender target broadcasts and actions`() {
        val plugin = mock<PunisherX>()
        val operator = mock<Player>()
        val target = mock<OfflinePlayer>()
        val victim = mock<Player>()
        val observer = mock<Player>()
        val uuid = UUID.randomUUID()
        whenever(operator.name).thenReturn("Admin")
        whenever(operator.uniqueId).thenReturn(UUID.randomUUID())
        whenever(operator.hasPermission(any<String>())).thenReturn(true)
        whenever(observer.uniqueId).thenReturn(UUID.randomUUID())
        whenever(observer.hasPermission(any<String>())).thenReturn(true)
        whenever(target.name).thenReturn("Target")
        whenever(target.uniqueId).thenReturn(uuid)
        whenever(target.player).thenReturn(victim)
        whenever(plugin.config).thenReturn(YamlConfiguration())
        whenever(plugin.databaseHandler).thenReturn(mock())
        pl.syntaxdevteam.punisher.databases.DatabaseHandler::class.java.getDeclaredField("plugin")
            .apply { isAccessible = true }.set(plugin.databaseHandler, plugin)
        whenever(plugin.databaseHandler.addPunishment(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(42L)
        whenever(plugin.databaseHandler.getActiveWarnCount(uuid.toString())).thenReturn(3)
        whenever(plugin.timeHandler).thenReturn(mock())
        whenever(plugin.timeHandler.formatTime(anyOrNull())).thenReturn("permanent")
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.messageHandler.getSmartMessage(any(), any(), any())).thenReturn(listOf(Component.text("Warning")))
        whenever(plugin.commandLoggerPlugin).thenReturn(mock())
        whenever(plugin.actionExecutor).thenReturn(mock())
        whenever(plugin.server).thenReturn(mock())
        whenever(plugin.server.onlinePlayers).thenReturn(listOf(operator, observer))
        val scheduler = mock<SchedulerAdapter>()
        whenever(plugin.schedulerAdapter).thenReturn(scheduler)
        doAnswer { it.getArgument<Runnable>(0).run(); null }.whenever(scheduler).runAsync(any())
        doAnswer { it.getArgument<Runnable>(1).run(); null }.whenever(scheduler).runForPlayer(any(), any())

        GuiPunishmentService(plugin).apply(operator, target, "WARN", "perm", "Spam")

        for (key in listOf("warn", "broadcast", "warn_message")) {
            verify(plugin.messageHandler).getSmartMessage(eq("warn"), eq(key), check {
                assertEquals("3", it["warn_no"])
                assertEquals("Target", it["player"])
            })
        }
        verify(plugin.actionExecutor).executeAction(eq("warned"), eq("Target"), check {
            assertEquals("3", it["warn_no"])
        }, eq(false), isNull())
        verify(plugin.actionExecutor).executeWarnCountActions("Target", 3)
        verify(operator).sendMessage(Component.text("Warning"))
        verify(observer).sendMessage(Component.text("Warning"))
        verify(victim).sendMessage(Component.text("Warning"))
    }
}
