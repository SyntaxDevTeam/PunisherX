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
                CheckCommand(plugin, plugin.pluginMetas, plugin.playerIPManager)
            )
            commands.register(
                "history",
                "Checking player all penalties history" + plugin.messageHandler.getMessage("history", "usage"),
                HistoryCommand(plugin, plugin.pluginMetas, plugin.playerIPManager)
            )
            commands.register(
                "kick",
                plugin.messageHandler.getMessage("kick", "usage"),
                KickCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "warn",
                plugin.messageHandler.getMessage("warn", "usage"),
                WarnCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "unwarn",
                plugin.messageHandler.getMessage("unwarn", "usage"),
                UnWarnCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "mute",
                plugin.messageHandler.getMessage("mute", "usage"),
                MuteCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "unmute", plugin.messageHandler.getMessage("mute", "usage"),
                UnMuteCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "jail",
                plugin.messageHandler.getMessage("jail", "usage"),
                JailCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "ban",
                plugin.messageHandler.getMessage("ban", "usage"),
                BanCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "banip",
                plugin.messageHandler.getMessage("banip", "usage"),
                BanIpCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "unban",
                plugin.messageHandler.getMessage("ban", "usage"),
                UnBanCommand(plugin, plugin.pluginMetas)
            )
            commands.register(
                "change-reason",
                plugin.messageHandler.getMessage("change-reason", "usage"),
                ChangeReasonCommand(plugin, plugin.pluginMetas)
            )
            commands.register("clearall", plugin.messageHandler.getMessage("clear", "usage"), ClearAllCommand(plugin))
            val aliases = plugin.config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { key ->
                val commandName = aliases.getString(key) ?: key
                when (key) {
                    "check" -> commands.register(
                        commandName,
                        "Checking player penalties" + plugin.messageHandler.getMessage("check", "usage"),
                        CheckCommand(plugin, plugin.pluginMetas, plugin.playerIPManager)
                    )

                    "history" -> commands.register(
                        commandName,
                        "Checking player all penalties history" + plugin.messageHandler.getMessage("history", "usage"),
                        HistoryCommand(plugin, plugin.pluginMetas, plugin.playerIPManager)
                    )

                    "kick" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("kick", "usage"),
                        KickCommand(plugin, plugin.pluginMetas)
                    )

                    "warn" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("warn", "usage"),
                        WarnCommand(plugin, plugin.pluginMetas)
                    )

                    "unwarn" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("unwarn", "usage"),
                        UnWarnCommand(plugin, plugin.pluginMetas)
                    )

                    "mute" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("mute", "usage"),
                        MuteCommand(plugin, plugin.pluginMetas)
                    )

                    "unmute" -> commands.register(
                        commandName, plugin.messageHandler.getMessage("mute", "usage"),
                        UnMuteCommand(plugin, plugin.pluginMetas)
                    )

                    "jail" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("jail", "usage"),
                        JailCommand(plugin, plugin.pluginMetas)
                    )

                    "ban" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("ban", "usage"),
                        BanCommand(plugin, plugin.pluginMetas)
                    )

                    "banip" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("banip", "usage"),
                        BanIpCommand(plugin, plugin.pluginMetas)
                    )

                    "unban" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("ban", "usage"),
                        UnBanCommand(plugin, plugin.pluginMetas)
                    )

                    "change-reason" -> commands.register(
                        commandName,
                        plugin.messageHandler.getMessage("change-reason", "usage"),
                        ChangeReasonCommand(plugin, plugin.pluginMetas)
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