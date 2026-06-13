package pl.syntaxdevteam.punisher.commands
import pl.syntaxdevteam.punisher.compatibility.*

import pl.syntaxdevteam.punisher.PunisherX

class CommandManager(private val plugin: PunisherX) {

    fun registerCommands() {
        register("punisherx", PunishesXCommands(plugin))
        register("check", CheckCommand(plugin, plugin.playerIPManager))
        register("history", HistoryCommand(plugin, plugin.playerIPManager))
        register("banlist", BanListCommand(plugin))
        register("kick", KickCommand(plugin))
        register("warn", WarnCommand(plugin))
        register("punish", PunishCommand(plugin))
        register("unwarn", UnWarnCommand(plugin))
        register("mute", MuteCommand(plugin))
        register("unmute", UnMuteCommand(plugin))
        register("setjail", SetjailCommand(plugin))
        register("setunjail", SetSpawnCommand(plugin))
        register("jail", JailCommand(plugin))
        register("unjail", UnjailCommand(plugin))
        register("ban", BanCommand(plugin))
        register("banip", BanIpCommand(plugin))
        register("unban", UnBanCommand(plugin))
        register("change-reason", ChangeReasonCommand(plugin))
        register("clearall", ClearAllCommand(plugin))
        register("panel", PanelCommand(plugin))
        register("langfix", PlaceholderFixCommand(plugin))
    }

    private fun register(name: String, command: BasicCommand) {
        val pluginCommand = plugin.getCommand(name)
        if (pluginCommand == null) {
            plugin.logger.warning("Command '$name' is missing from plugin.yml")
            return
        }

        val adapter = SpigotCommandAdapter(command)
        pluginCommand.setExecutor(adapter)
        pluginCommand.tabCompleter = adapter
    }
}
