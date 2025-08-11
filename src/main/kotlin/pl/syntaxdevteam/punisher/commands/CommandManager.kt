package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin
import pl.syntaxdevteam.punisher.PunisherX

class CommandManager(private val plugin: PunisherX) {
    private data class CommandInfo(val description: String, val executor: BasicCommand)

    private val messageCache = mutableMapOf<Pair<String, String>, String>()

    private fun simpleMessage(command: String, @Suppress("SameParameterValue") key: String) =
        messageCache.getOrPut(command to key) {
            plugin.messageHandler.getSimpleMessage(command, key)
        }


    fun registerCommands() {
        val manager: LifecycleEventManager<Plugin> = plugin.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val registrar: Commands = event.registrar()
            val commandMap = buildCommandMap()

            commandMap.forEach { (name, info) ->
                register(registrar, name, info)
            }

            val aliases = plugin.config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { alias ->
                val base = aliases.getString(alias) ?: alias
                registerAlias(registrar, alias, base, commandMap)
            }
        }
    }

    private fun buildCommandMap(): Map<String, CommandInfo> =
        mapOf(
            "punisherx" to CommandInfo(
                "PunisherX plugin command. Type /punisherx help to check available commands",
                PunishesXCommands(plugin)
            ),
            "prx" to CommandInfo(
                "PunisherX plugin command. Type /prx help to check available commands",
                PunishesXCommands(plugin)
            ),
            "check" to CommandInfo(
                "Checking player penalties" + simpleMessage("check", "usage"),
                CheckCommand(plugin, plugin.playerIPManager)
            ),
            "history" to CommandInfo(
                "Checking player all penalties history" + simpleMessage("history", "usage"),
                HistoryCommand(plugin, plugin.playerIPManager)
            ),
            "banlist" to CommandInfo(
                "Checking player all penalties history" + simpleMessage("banlist", "usage"),
                BanListCommand(plugin)
            ),
            "kick" to CommandInfo(
                simpleMessage("kick", "usage"),
                KickCommand(plugin)
            ),
            "warn" to CommandInfo(
                simpleMessage("warn", "usage"),
                WarnCommand(plugin)
            ),
            "unwarn" to CommandInfo(
                simpleMessage("unwarn", "usage"),
                UnWarnCommand(plugin)
            ),
            "mute" to CommandInfo(
                simpleMessage("mute", "usage"),
                MuteCommand(plugin)
            ),
            "unmute" to CommandInfo(
                simpleMessage("unmute", "usage"),
                UnMuteCommand(plugin)
            ),
            "setjail" to CommandInfo(
                simpleMessage("setjail", "usage"),
                SetjailCommand(plugin)
            ),
            "setspawn" to CommandInfo(
                simpleMessage("setspawn", "usage"),
                SetSpawnCommand(plugin)
            ),
            "jail" to CommandInfo(
                simpleMessage("jail", "usage"),
                JailCommand(plugin)
            ),
            "unjail" to CommandInfo(
                simpleMessage("unjail", "usage"),
                UnjailCommand(plugin)
            ),
            "ban" to CommandInfo(
                simpleMessage("ban", "usage"),
                BanCommand(plugin)
            ),
            "banip" to CommandInfo(
                simpleMessage("banip", "usage"),
                BanIpCommand(plugin)
            ),
            "unban" to CommandInfo(
                simpleMessage("unban", "usage"),
                UnBanCommand(plugin)
            ),
            "cache" to CommandInfo(
                "debugging command",
                CacheCommand(plugin)
            ),
            "change-reason" to CommandInfo(
                simpleMessage("change-reason", "usage"),
                ChangeReasonCommand(plugin)
            ),
            "clearall" to CommandInfo(
                simpleMessage("clear", "usage"),
                ClearAllCommand(plugin)
            )
        )
    private fun register(commands: Commands, name: String, info: CommandInfo) {
        commands.register(name, info.description, info.executor)
    }
    private fun registerAlias(
        commands: Commands,
        alias: String,
        base: String,
        commandMap: Map<String, CommandInfo>
    ) {
        commandMap[base]?.let { register(commands, alias, it) }
    }
}