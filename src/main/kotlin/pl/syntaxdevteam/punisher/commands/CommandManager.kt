package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin
import pl.syntaxdevteam.punisher.PunisherX

class CommandManager(private val plugin: PunisherX) {

    fun registerCommands() {
        val manager: LifecycleEventManager<Plugin> = plugin.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands: Commands = event.registrar()
            commands.register(
                "punisherx",
                "PunisherX plugin command. Type /punisherx help to check available commands",
                PunishesXCommands(plugin)
            )
            commands.register(
                "prx",
                "PunisherX plugin command. Type /prx help to check available commands",
                PunishesXCommands(plugin)
            )
            commands.register(
                "check",
                "Checking player penalties" + plugin.messageHandler.stringMessageToString("check", "usage"),
                CheckCommand(plugin, plugin.playerIPManager)
            )
            commands.register(
                "history",
                "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("history", "usage"),
                HistoryCommand(plugin, plugin.playerIPManager)
            )
            commands.register(
                "banlist",
                "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("banlist", "usage"),
                BanListCommand(plugin)
            )
            commands.register(
                "kick",
                plugin.messageHandler.stringMessageToString("kick", "usage"),
                KickCommand(plugin)
            )
            commands.register(
                "warn",
                plugin.messageHandler.stringMessageToString("warn", "usage"),
                WarnCommand(plugin)
            )
            commands.register(
                "punish",
                plugin.messageHandler.stringMessageToString("punish", "usage"),
                PunishCommand(plugin)
            )
            commands.register(
                "unwarn",
                plugin.messageHandler.stringMessageToString("unwarn", "usage"),
                UnWarnCommand(plugin)
            )
            commands.register(
                "mute",
                plugin.messageHandler.stringMessageToString("mute", "usage"),
                MuteCommand(plugin)
            )
            commands.register(
                "unmute", plugin.messageHandler.stringMessageToString("unmute", "usage"),
                UnMuteCommand(plugin)
            )
            commands.register(
                "setjail",
                plugin.messageHandler.stringMessageToString("setjail", "usage"),
                SetjailCommand(plugin)
            )
            commands.register(
                "setunjail",
                plugin.messageHandler.stringMessageToString("setunjail", "usage"),
                SetSpawnCommand(plugin)
            )
            commands.register(
                "jail",
                plugin.messageHandler.stringMessageToString("jail", "usage"),
                JailCommand(plugin)
            )
            commands.register(
                "unjail",
                plugin.messageHandler.stringMessageToString("unjail", "usage"),
                UnjailCommand(plugin)
            )
            commands.register(
                "ban",
                plugin.messageHandler.stringMessageToString("ban", "usage"),
                BanCommand(plugin)
            )
            commands.register(
                "banip",
                plugin.messageHandler.stringMessageToString("banip", "usage"),
                BanIpCommand(plugin)
            )
            commands.register(
                "unban",
                plugin.messageHandler.stringMessageToString("unban", "usage"),
                UnBanCommand(plugin)
            )
            commands.register(
                "cache",
                "debugging command",
                CacheCommand(plugin)
            )
            commands.register(
                "change-reason",
                plugin.messageHandler.stringMessageToString("change-reason", "usage"),
                ChangeReasonCommand(plugin)
            )
            commands.register(
                "clearall",
                plugin.messageHandler.stringMessageToString("clear", "usage"),
                ClearAllCommand(plugin)
            )
            commands.register(
                "panel",
                "Opens the PunisherX GUI with lots of useful information and commands.",
                PanelCommand(plugin)
            )
            commands.register(
                "langfix",
                "Converts legacy translation placeholders from {} to <>",
                PlaceholderFixCommand(plugin)
            )
            commands.register(
                "report",
                "Report a player for breaking rules",
                ReportCommand(plugin)
            )
            val aliases = plugin.config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { key ->
                val commandName = aliases.getString(key) ?: key
                when (key) {
                    "check" -> commands.register(
                        commandName,
                        "Checking player penalties" + plugin.messageHandler.stringMessageToString("check", "usage"),
                        CheckCommand(plugin, plugin.playerIPManager)
                    )

                    "history" -> commands.register(
                        commandName,
                        "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("history", "usage"),
                        HistoryCommand(plugin, plugin.playerIPManager)
                    )

                    "kick" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("kick", "usage"),
                        KickCommand(plugin)
                    )

                    "warn" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("warn", "usage"),
                        WarnCommand(plugin)
                    )

                    "punish" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("punish", "usage"),
                        PunishCommand(plugin)
                    )

                    "unwarn" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("unwarn", "usage"),
                        UnWarnCommand(plugin)
                    )

                    "mute" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("mute", "usage"),
                        MuteCommand(plugin)
                    )

                    "unmute" -> commands.register(
                        commandName, plugin.messageHandler.stringMessageToString("unmute", "usage"),
                        UnMuteCommand(plugin)
                    )

                    "setjail" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("setjail", "usage"),
                        SetjailCommand(plugin)
                    )

                    "setunjail" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("setunjail", "usage"),
                        SetSpawnCommand(plugin)
                    )

                    "jail" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("jail", "usage"),
                        JailCommand(plugin)
                    )

                    "unjail" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("unjail", "usage"),
                        UnjailCommand(plugin)
                    )

                    "ban" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("ban", "usage"),
                        BanCommand(plugin)
                    )

                    "banip" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("banip", "usage"),
                        BanIpCommand(plugin)
                    )

                    "unban" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("unban", "usage"),
                        UnBanCommand(plugin)
                    )

                    "change-reason" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("change-reason", "usage"),
                        ChangeReasonCommand(plugin)
                    )

                    "clearall" -> commands.register(
                        commandName,
                        plugin.messageHandler.stringMessageToString("clear", "usage"),
                        ClearAllCommand(plugin)
                    )
                    "panel" -> commands.register(
                        commandName,
                        "Opens the PunisherX GUI with lots of useful information and commands.",
                        PanelCommand(plugin)
                    )
                }
            }
        }
    }
}
