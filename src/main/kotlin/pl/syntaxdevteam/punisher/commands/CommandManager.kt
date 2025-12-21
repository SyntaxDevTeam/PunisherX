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
                PunishesXCommands(plugin).build("punisherx"),
                "PunisherX plugin command. Type /punisherx help to check available commands"
            )
            commands.register(
                PunishesXCommands(plugin).build("prx"),
                "PunisherX plugin command. Type /prx help to check available commands"
            )
            commands.register(
                CheckCommand(plugin, plugin.playerIPManager).build("check"),
                "Checking player penalties" + plugin.messageHandler.stringMessageToString("check", "usage")
            )
            commands.register(
                HistoryCommand(plugin, plugin.playerIPManager).build("history"),
                "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("history", "usage")
            )
            commands.register(
                BanListCommand(plugin).build("banlist"),
                "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("banlist", "usage")
            )
            commands.register(
                KickCommand(plugin).build("kick"),
                plugin.messageHandler.stringMessageToString("kick", "usage")
            )
            commands.register(
                WarnCommand(plugin).build("warn"),
                plugin.messageHandler.stringMessageToString("warn", "usage")
            )
            commands.register(
                PunishCommand(plugin).build("punish"),
                plugin.messageHandler.stringMessageToString("punish", "usage")
            )
            commands.register(
                UnWarnCommand(plugin).build("unwarn"),
                plugin.messageHandler.stringMessageToString("unwarn", "usage")
            )
            commands.register(
                MuteCommand(plugin).build("mute"),
                plugin.messageHandler.stringMessageToString("mute", "usage")
            )
            commands.register(
                UnMuteCommand(plugin).build("unmute"),
                plugin.messageHandler.stringMessageToString("unmute", "usage")
            )
            commands.register(
                SetjailCommand(plugin).build("setjail"),
                plugin.messageHandler.stringMessageToString("setjail", "usage")
            )
            commands.register(
                SetSpawnCommand(plugin).build("setunjail"),
                plugin.messageHandler.stringMessageToString("setunjail", "usage")
            )
            commands.register(
                JailCommand(plugin).build("jail"),
                plugin.messageHandler.stringMessageToString("jail", "usage")
            )
            commands.register(
                UnjailCommand(plugin).build("unjail"),
                plugin.messageHandler.stringMessageToString("unjail", "usage")
            )
            commands.register(
                BanCommand(plugin).build("ban"),
                plugin.messageHandler.stringMessageToString("ban", "usage")
            )
            commands.register(
                BanIpCommand(plugin).build("banip"),
                plugin.messageHandler.stringMessageToString("banip", "usage")
            )
            commands.register(
                UnBanCommand(plugin).build("unban"),
                plugin.messageHandler.stringMessageToString("unban", "usage")
            )
            commands.register(
                CacheCommand(plugin).build("cache"),
                "debugging command"
            )
            commands.register(
                ChangeReasonCommand(plugin).build("change-reason"),
                plugin.messageHandler.stringMessageToString("change-reason", "usage")
            )
            commands.register(
                ClearAllCommand(plugin).build("clearall"),
                plugin.messageHandler.stringMessageToString("clear", "usage")
            )
            commands.register(
                PanelCommand(plugin).build("panel"),
                "Opens the PunisherX GUI with lots of useful information and commands."
            )
            commands.register(
                PlaceholderFixCommand(plugin).build("langfix"),
                "Converts legacy translation placeholders from {} to <>"
            )
            commands.register(
                ReportCommand(plugin).build("report"),
                "Report a player for breaking rules"
            )
            val aliases = plugin.config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { key ->
                val commandName = aliases.getString(key) ?: key
                when (key) {
                    "check" -> commands.register(
                        CheckCommand(plugin, plugin.playerIPManager).build(commandName),
                        "Checking player penalties" + plugin.messageHandler.stringMessageToString("check", "usage")
                    )

                    "history" -> commands.register(
                        HistoryCommand(plugin, plugin.playerIPManager).build(commandName),
                        "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("history", "usage")
                    )

                    "kick" -> commands.register(
                        KickCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("kick", "usage")
                    )

                    "warn" -> commands.register(
                        WarnCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("warn", "usage")
                    )

                    "punish" -> commands.register(
                        PunishCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("punish", "usage")
                    )

                    "unwarn" -> commands.register(
                        UnWarnCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("unwarn", "usage")
                    )

                    "mute" -> commands.register(
                        MuteCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("mute", "usage")
                    )

                    "unmute" -> commands.register(
                        UnMuteCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("unmute", "usage")
                    )

                    "setjail" -> commands.register(
                        SetjailCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("setjail", "usage")
                    )

                    "setunjail" -> commands.register(
                        SetSpawnCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("setunjail", "usage")
                    )

                    "jail" -> commands.register(
                        JailCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("jail", "usage")
                    )

                    "unjail" -> commands.register(
                        UnjailCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("unjail", "usage")
                    )

                    "ban" -> commands.register(
                        BanCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("ban", "usage")
                    )

                    "banip" -> commands.register(
                        BanIpCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("banip", "usage")
                    )

                    "unban" -> commands.register(
                        UnBanCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("unban", "usage")
                    )

                    "change-reason" -> commands.register(
                        ChangeReasonCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("change-reason", "usage")
                    )

                    "clearall" -> commands.register(
                        ClearAllCommand(plugin).build(commandName),
                        plugin.messageHandler.stringMessageToString("clear", "usage")
                    )
                    "panel" -> commands.register(
                        PanelCommand(plugin).build(commandName),
                        "Opens the PunisherX GUI with lots of useful information and commands."
                    )
                }
            }
        }
    }
}
