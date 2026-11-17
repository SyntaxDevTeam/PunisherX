package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin
import pl.syntaxdevteam.punisher.PunisherX

class CommandManager(private val plugin: PunisherX) {

    private fun registerCommand(commands: Commands, name: String, description: String?, command: io.papermc.paper.command.brigadier.BasicCommand) {
        if (command is BrigadierCommand) {
            val node = command.build(name)
            BrigadierCommandUtils.addTrailingFlags(node, command)
            commands.register(node, description, emptyList())
        } else commands.register(name, description, command)
    }

    fun registerCommands() {
        val manager: LifecycleEventManager<Plugin> = plugin.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands: Commands = event.registrar()
            registerCommand(commands, "punisherx",
                "PunisherX plugin command. Type /punisherx help to check available commands",
                PunisherRootCommand(plugin)
            )
            registerCommand(commands, "prx",
                "PunisherX plugin command. Type /prx help to check available commands",
                PunisherRootCommand(plugin)
            )
            registerCommand(commands, "check",
                "Checking player penalties" + plugin.messageHandler.stringMessageToString("check", "usage"),
                CheckCommand(plugin, plugin.playerIPManager)
            )
            registerCommand(commands, "history",
                "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("history", "usage"),
                HistoryCommand(plugin, plugin.playerIPManager)
            )
            registerCommand(commands, "banlist",
                "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("banlist", "usage"),
                BanListCommand(plugin)
            )
            registerCommand(commands, "kick",
                plugin.messageHandler.stringMessageToString("kick", "usage"),
                KickCommand(plugin)
            )
            registerCommand(commands, "warn",
                plugin.messageHandler.stringMessageToString("warn", "usage"),
                WarnCommand(plugin)
            )
            registerCommand(commands, "unmark", "/unmark <player|all> [mark] | pause/resume <mark>", UnmarkCommand(plugin))
            val unpunishCmd = UnpunishCommand(plugin)
            val unpunishNode = Commands.literal("unpunish")
                .requires { src -> pl.syntaxdevteam.punisher.permissions.PermissionChecker
                    .hasWithLegacy(src.sender, pl.syntaxdevteam.punisher.permissions.PermissionChecker.PermissionKey.UNPUNISH) }
                .executes { ctx -> unpunishCmd.execute(ctx.source, emptyArray()); 1 }
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests { _, builder ->
                        plugin.server.onlinePlayers.forEach { builder.suggest(it.name) }
                        builder.buildFuture()
                    }
                    .executes { ctx ->
                        unpunishCmd.execute(ctx.source, arrayOf(StringArgumentType.getString(ctx, "player"))); 1
                    }
                    .then(Commands.argument("template", StringArgumentType.string())
                        .suggests { _, builder ->
                            plugin.punishTemplateManager.getTemplateNames().forEach {
                                builder.suggest(StringArgumentType.escapeIfRequired(it))
                            }
                            builder.buildFuture()
                        }
                        .executes { ctx ->
                            unpunishCmd.execute(ctx.source, arrayOf(
                                StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "template")))
                            1
                        }
                        .then(Commands.argument("flags", StringArgumentType.greedyString())
                            .suggests { _, builder ->
                                if ("--silent".startsWith(builder.remaining)) builder.suggest("--silent")
                                builder.buildFuture()
                            }
                            .executes { ctx ->
                                val flags = StringArgumentType.getString(ctx, "flags").trim().split(Regex("\\s+"))
                                unpunishCmd.execute(ctx.source, arrayOf(
                                    StringArgumentType.getString(ctx, "player"),
                                    StringArgumentType.getString(ctx, "template"), *flags.toTypedArray()))
                                1
                            })))
                .build()
            commands.register(unpunishNode, "/unpunish <player> <template>", emptyList())
            val punishNode = createPunishNode()
            commands.register(punishNode, plugin.messageHandler.stringMessageToString("punish", "usage"), emptyList())
            registerCommand(commands, "unwarn",
                plugin.messageHandler.stringMessageToString("unwarn", "usage"),
                UnWarnCommand(plugin)
            )
            registerCommand(commands, "mute",
                plugin.messageHandler.stringMessageToString("mute", "usage"),
                MuteCommand(plugin)
            )
            registerCommand(commands, "unmute", plugin.messageHandler.stringMessageToString("unmute", "usage"),
                UnMuteCommand(plugin)
            )
            registerCommand(commands, "setjail",
                plugin.messageHandler.stringMessageToString("setjail", "usage"),
                SetjailCommand(plugin)
            )
            registerCommand(commands, "setunjail",
                plugin.messageHandler.stringMessageToString("setunjail", "usage"),
                SetSpawnCommand(plugin)
            )
            registerCommand(commands, "jail",
                plugin.messageHandler.stringMessageToString("jail", "usage"),
                JailCommand(plugin)
            )
            registerCommand(commands, "unjail",
                plugin.messageHandler.stringMessageToString("unjail", "usage"),
                UnjailCommand(plugin)
            )
            registerCommand(commands, "ban",
                plugin.messageHandler.stringMessageToString("ban", "usage"),
                BanCommand(plugin)
            )
            registerCommand(commands, "banip",
                plugin.messageHandler.stringMessageToString("banip", "usage"),
                BanIpCommand(plugin)
            )
            registerCommand(commands, "unban",
                plugin.messageHandler.stringMessageToString("unban", "usage"),
                UnBanCommand(plugin)
            )
            registerCommand(commands, "cache",
                "debugging command",
                CacheCommand(plugin)
            )
            registerCommand(commands, "change-reason",
                plugin.messageHandler.stringMessageToString("change-reason", "usage"),
                ChangeReasonCommand(plugin)
            )
            registerCommand(commands, "clearall",
                plugin.messageHandler.stringMessageToString("clear", "usage"),
                ClearAllCommand(plugin)
            )
            registerCommand(commands, "panel",
                "Opens the PunisherX GUI with lots of useful information and commands.",
                PanelCommand(plugin)
            )
            registerCommand(commands, "langfix",
                "Converts legacy translation placeholders from {} to <>",
                PlaceholderFixCommand(plugin)
            )
            registerCommand(commands, "report",
                "Report a player for breaking rules",
                ReportCommand(plugin)
            )
            registerCommand(commands, "reports",
                "Review and manage player reports",
                ReportsCommand(plugin)
            )
            registerCommand(commands, "note", "Manage private staff notes", NoteCommand(plugin))

            val aliases = plugin.config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { key ->
                val commandName = aliases.getString(key) ?: key
                when (key) {
                    "check" -> registerCommand(commands, commandName,
                        "Checking player penalties" + plugin.messageHandler.stringMessageToString("check", "usage"),
                        CheckCommand(plugin, plugin.playerIPManager)
                    )

                    "history" -> registerCommand(commands, commandName,
                        "Checking player all penalties history" + plugin.messageHandler.stringMessageToString("history", "usage"),
                        HistoryCommand(plugin, plugin.playerIPManager)
                    )

                    "kick" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("kick", "usage"),
                        KickCommand(plugin)
                    )

                    "warn" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("warn", "usage"),
                        WarnCommand(plugin)
                    )
/*
                    "unpunish" -> registerCommand(commands, commandName, "/unpunish <player> <template>", UnpunishCommand(plugin)
                    )
                    "punish" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("punish", "usage"),
                        PunishCommand(plugin)
                    )*/

                    "unwarn" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("unwarn", "usage"),
                        UnWarnCommand(plugin)
                    )

                    "mute" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("mute", "usage"),
                        MuteCommand(plugin)
                    )

                    "unmute" -> registerCommand(commands, commandName, plugin.messageHandler.stringMessageToString("unmute", "usage"),
                        UnMuteCommand(plugin)
                    )

                    "setjail" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("setjail", "usage"),
                        SetjailCommand(plugin)
                    )

                    "setunjail" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("setunjail", "usage"),
                        SetSpawnCommand(plugin)
                    )

                    "jail" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("jail", "usage"),
                        JailCommand(plugin)
                    )

                    "unjail" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("unjail", "usage"),
                        UnjailCommand(plugin)
                    )

                    "ban" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("ban", "usage"),
                        BanCommand(plugin)
                    )

                    "banip" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("banip", "usage"),
                        BanIpCommand(plugin)
                    )

                    "unban" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("unban", "usage"),
                        UnBanCommand(plugin)
                    )

                    "change-reason" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("change-reason", "usage"),
                        ChangeReasonCommand(plugin)
                    )

                    "clearall" -> registerCommand(commands, commandName,
                        plugin.messageHandler.stringMessageToString("clear", "usage"),
                        ClearAllCommand(plugin)
                    )
                    "panel" -> registerCommand(commands, commandName,
                        "Opens the PunisherX GUI with lots of useful information and commands.",
                        PanelCommand(plugin)
                    )
                }
            }
        }
    }

    // Keep the registered tree available to dispatcher regression tests.
    internal fun createPunishNode(): com.mojang.brigadier.tree.LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> {
        val punishCmd = PunishCommand(plugin)
        return Commands.literal("punish")
            .requires { src -> pl.syntaxdevteam.punisher.permissions.PermissionChecker
                .hasWithLegacy(src.sender, pl.syntaxdevteam.punisher.permissions.PermissionChecker.PermissionKey.PUNISH) }
            .executes { ctx ->
                punishCmd.executeParsed(ctx.source, emptyArray())
                com.mojang.brigadier.Command.SINGLE_SUCCESS
            }
            .then(
                Commands.argument("player", StringArgumentType.word())
                    .executes { ctx ->
                        punishCmd.executeParsed(ctx.source, arrayOf(StringArgumentType.getString(ctx, "player")))
                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                    }
                    .suggests { _, builder ->
                        plugin.server.onlinePlayers.filter { it.name.startsWith(builder.remaining, ignoreCase = true) }
                            .forEach { builder.suggest(it.name) }
                        builder.buildFuture()
                    }
                    .then(
                        Commands.argument("template", StringArgumentType.string())
                            .suggests { _, builder ->
                                PunishCommandCompletion.templates(
                                    plugin.punishTemplateManager.getTemplateNames(), builder.remaining
                                ).forEach { builder.suggest(it) }
                                builder.buildFuture()
                            }
                            .executes { ctx ->
                                val player = StringArgumentType.getString(ctx, "player")
                                val template = StringArgumentType.getString(ctx, "template")
                                punishCmd.executeParsed(ctx.source, arrayOf(player, template))
                                com.mojang.brigadier.Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument("options", StringArgumentType.greedyString())
                                    .suggests { ctx, builder ->
                                        val tokens = builder.remaining.split(Regex("\\s+"))
                                        val input = tokens.last()
                                        val suggestions = builder.createOffset(builder.start + builder.remaining.length - input.length)
                                        val used = tokens.dropLast(1)
                                        if (used.none { it.toIntOrNull() != null }) {
                                            plugin.punishTemplateManager.getTemplate(StringArgumentType.getString(ctx, "template"))
                                                ?.levels?.keys?.sorted()?.forEach {
                                                    if (it.toString().startsWith(input)) suggestions.suggest(it)
                                                }
                                        }
                                        listOf("--force", "--silent", "--mark", "--mark=").filter { it !in used && it.startsWith(input) }
                                            .forEach { suggestions.suggest(it) }
                                        suggestions.buildFuture()
                                    }
                                    .executes { ctx ->
                                        val player = StringArgumentType.getString(ctx, "player")
                                        val template = StringArgumentType.getString(ctx, "template")
                                        val options = StringArgumentType.getString(ctx, "options").trim().split(Regex("\\s+"))
                                        punishCmd.executeParsed(ctx.source, arrayOf(player, template, *options.toTypedArray()))
                                        com.mojang.brigadier.Command.SINGLE_SUCCESS
                                    }
                            )
                    )
            )
            .build()
    }

}
