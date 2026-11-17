package pl.syntaxdevteam.punisher.common

import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import kotlin.test.Test

class PunishmentActionExecutorSilentTest {
    private val plugin = mock<PunisherX>()
    private val server = mock<org.bukkit.Server>()
    private val console = mock<org.bukkit.command.ConsoleCommandSender>()
    private val config = YamlConfiguration()

    init {
        whenever(plugin.config).thenReturn(config)
        whenever(plugin.server).thenReturn(server)
        whenever(server.consoleSender).thenReturn(console)
        whenever(plugin.logger).thenReturn(mock())
    }

    @Test fun `silent warnings propagate silence to automatic punishment commands`() {
        config.set("actions.warn.count.3", "kick {player} Warning {warn_no}")
        PunishmentActionExecutor(plugin).executeWarnCountActions("Steve", 3, silent = true)
        verify(server).dispatchCommand(console, "kick Steve Warning 3 --silent")
    }

    @Test fun `silent followups support aliases and namespace while preserving other actions`() {
        config.set("aliases.ban", "bx")
        config.set("actions.muted", listOf("punisherx:ban {player} 1h Spam", "bx {player} Spam",
            "eco take {player} 500", "other:ban {player} Spam"))
        PunishmentActionExecutor(plugin).executeAction("muted", "Steve", silent = true)
        verify(server).dispatchCommand(console, "punisherx:ban Steve 1h Spam --silent")
        verify(server).dispatchCommand(console, "bx Steve Spam --silent")
        verify(server).dispatchCommand(console, "eco take Steve 500")
        verify(server).dispatchCommand(console, "other:ban Steve Spam")
    }

    @Test fun `normal warnings retain their configured command`() {
        config.set("actions.warn.count.3", "kick {player} Warning {warn_no}")
        PunishmentActionExecutor(plugin).executeWarnCountActions("Steve", 3)
        verify(server).dispatchCommand(console, "kick Steve Warning 3")
    }
}
