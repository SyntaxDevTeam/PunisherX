package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
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
                "Checking player penalties" + plugin.messageHandler.getMessage("check", "usage"),
                CheckCommand(plugin, plugin.playerIPManager)
            )
            commands.register(
                "history",
                "Checking player all penalties history" + plugin.messageHandler.getMessage("history", "usage"),
                HistoryCommand(plugin, plugin.playerIPManager)
            )
            commands.register(
                "kick",
                plugin.messageHandler.getMessage("kick", "usage"),
                KickCommand(plugin)
            )
            commands.register(
                "warn",
                plugin.messageHandler.getMessage("warn", "usage"),
                WarnCommand(plugin)
            )
            commands.register(
                "unwarn",
                plugin.messageHandler.getMessage("unwarn", "usage"),
                UnWarnCommand(plugin)
            )
            commands.register(
                "mute",
                plugin.messageHandler.getMessage("mute", "usage"),
                MuteCommand(plugin)
            )
            commands.register(
                "unmute", plugin.messageHandler.getMessage("unmute", "usage"),
                UnMuteCommand(plugin)
            )
            commands.register(
                "setjail",
                plugin.messageHandler.getMessage("setjail", "usage"),
                SetjailCommand(plugin)
            )
            commands.register(
                "jail",
                plugin.messageHandler.getMessage("jail", "usage"),
                JailCommand(plugin)
            )
            commands.register(
                "unjail",
                plugin.messageHandler.getMessage("unjail", "usage"),
                UnjailCommand(plugin)
            )
            commands.register(
                "ban",
                plugin.messageHandler.getMessage("ban", "usage"),
                BanCommand(plugin)
            )
            commands.register(
                "banip",
                plugin.messageHandler.getMessage("banip", "usage"),
                BanIpCommand(plugin)
            )
            commands.register(
                "unban",
                plugin.messageHandler.getMessage("unban", "usage"),
                UnBanCommand(plugin)
            )
            commands.register(
                "cache",
                plugin.messageHandler.getMessage("cache", "usage"),
                CacheCommand(plugin)
            )
            commands.register(
                "change-reason",
                plugin.messageHandler.getMessage("change-reason", "usage"),
                ChangeReasonCommand(plugin)
            )
            commands.register("clearall", plugin.messageHandler.getMessage("clear", "usage"), ClearAllCommand(plugin))
            val aliases = plugin.config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { key ->
                val commandName = aliases.getString(key) ?: key
                when (key) {
                    "check" -> commands.register(
                        commandName,
                        "Checking player penalties" + plugin.messageHandler.getMessage("check", "usage"),
                        CheckCommand(plugin, plugin.playerIPManager)
                    )

                    "history" -> commands.register(
                        commandName,
                        "Checking player all penalties history" + plugin.messageHandler.getMessage("history", "usage"),
                        HistoryCommand(plugin, plugin.playerIPManager)
                    )

                    "kick" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("kick", "usage"),
                        KickCommand(plugin)
                    )

                    "warn" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("warn", "usage"),
                        WarnCommand(plugin)
                    )

                    "unwarn" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("unwarn", "usage"),
                        UnWarnCommand(plugin)
                    )

                    "mute" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("mute", "usage"),
                        MuteCommand(plugin)
                    )

                    "unmute" -> commands.register(
                        commandName, plugin.messageHandler.getMessage("unmute", "usage"),
                        UnMuteCommand(plugin)
                    )

                    "setjail" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("setjail", "usage"),
                        SetjailCommand(plugin)
                    )

                    "jail" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("jail", "usage"),
                        JailCommand(plugin)
                    )

                    "unjail" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("unjail", "usage"),
                        UnjailCommand(plugin)
                    )

                    "ban" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("ban", "usage"),
                        BanCommand(plugin)
                    )

                    "banip" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("banip", "usage"),
                        BanIpCommand(plugin)
                    )

                    "unban" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("unban", "usage"),
                        UnBanCommand(plugin)
                    )

                    "change-reason" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("change-reason", "usage"),
                        ChangeReasonCommand(plugin)
                    )

                    "clearall" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("clear", "usage"),
                        ClearAllCommand(plugin)
                    )
                }
            }
        }
    }
}