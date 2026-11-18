package pl.syntaxdevteam.punisher.commands

import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.PunishmentActionExecutor
import kotlin.test.Test
import org.bukkit.Server
import org.bukkit.command.ConsoleCommandSender

class MarkedPunishmentActionsTest {
    private val plugin = mock<PunisherX>()
    private val server = mock<Server>()
    private val console = mock<ConsoleCommandSender>()
    private val config = YamlConfiguration()

    init {
        whenever(plugin.config).thenReturn(config)
        whenever(plugin.server).thenReturn(server)
        whenever(server.consoleSender).thenReturn(console)
        whenever(plugin.logger).thenReturn(mock())
    }

    @Test fun `warning threshold penalties inherit originating mark and silence`() {
        config.set("actions.warn.count.3", "ban {player} Warnings {warn_no}")
        PunishmentActionExecutor(plugin).executeWarnCountActions("Steve", 3, silent = true, mark = "speed")
        verify(server).dispatchCommand(console, "ban Steve Warnings 3 --silent --mark=speed")
    }

    @Test fun `followups mark aliases and namespace but preserve external and removal commands`() {
        config.set("aliases.ban", "bx")
        config.set("actions.muted", listOf("punisherx:ban {player} Spam", "bx {player} Spam --mark=other",
            "eco take {player} 500", "other:ban {player} Spam", "unban {player}"))
        PunishmentActionExecutor(plugin).executeAction("muted", "Steve", mark = "speed")
        verify(server).dispatchCommand(console, "punisherx:ban Steve Spam --mark=speed")
        verify(server).dispatchCommand(console, "bx Steve Spam --mark=speed")
        verify(server).dispatchCommand(console, "eco take Steve 500")
        verify(server).dispatchCommand(console, "other:ban Steve Spam")
        verify(server).dispatchCommand(console, "unban Steve")
    }
}
