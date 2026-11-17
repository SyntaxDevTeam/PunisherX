package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.commands.CommandTree.Argument
import pl.syntaxdevteam.punisher.commands.CommandTree.Kind
import pl.syntaxdevteam.punisher.commands.CommandTree.Literal
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.permissions.PermissionChecker.PermissionKey

class CommandManager(private val plugin: PunisherX) {

    fun registerCommands() {
        val manager: LifecycleEventManager<Plugin> = plugin.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands: Commands = event.registrar()
            val tree = CommandTree(plugin)
            commandKeys.forEach { key -> commands.register(node(tree, key, key)!!, description(key), emptyList()) }
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

            val aliases = plugin.config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { key ->
                val commandName = aliases.getString(key) ?: key
                node(tree, key, commandName)?.let { commands.register(it, description(key), emptyList()) }
            }
        }
    }

    private val commandKeys = listOf(
        "punisherx", "prx", "check", "history", "banlist", "kick", "warn", "unmark", "unwarn", "mute", "unmute",
        "setjail", "setunjail", "jail", "unjail", "ban", "banip", "unban", "cache", "change-reason", "clearall",
        "panel", "langfix", "report", "reports", "note"
    )

    private fun usage(key: String) = plugin.messageHandler.stringMessageToString(key, "usage")

    private fun description(key: String): String = when (key) {
        "punisherx", "prx" -> "PunisherX plugin command. Type /$key help to check available commands"
        "check" -> "Checking player penalties" + usage("check")
        "history" -> "Checking player all penalties history" + usage("history")
        "banlist" -> "Checking player all penalties history" + usage("banlist")
        "unmark" -> "/unmark <player|all> [mark] | pause/resume <mark>"
        "cache" -> "debugging command"
        "clearall" -> usage("clear")
        "panel" -> "Opens the PunisherX GUI with lots of useful information and commands."
        "langfix" -> "Converts legacy translation placeholders from {} to <>"
        "report" -> "Report a player for breaking rules"
        "reports" -> "Review and manage player reports"
        "note" -> "Manage private staff notes"
        else -> usage(key)
    }

    // The forms each command accepts. Null for keys that are not commands, e.g. an unknown alias.
    private fun node(tree: CommandTree, key: String, name: String): LiteralCommandNode<CommandSourceStack>? {
        fun legacy(permission: PermissionKey): (CommandSender) -> Boolean = { PermissionChecker.hasWithLegacy(it, permission) }
        fun manage(permission: PermissionKey): (CommandSender) -> Boolean = { PermissionChecker.hasWithManage(it, permission) }
        val anyone: (CommandSender) -> Boolean = { true }
        val player = Argument("player", values = tree::players)
        val silent = Argument("flags", Kind.GREEDY, listOf("--silent"))
        val page = Argument("page")

        return when (key) {
            "ban" -> tree.punishment(name, "ban", PermissionKey.BAN, BanCommand(plugin))
            "banip" -> tree.punishment(name, "banip", PermissionKey.BANIP, BanIpCommand(plugin), targetArgument = "target")
            "mute" -> tree.punishment(name, "mute", PermissionKey.MUTE, MuteCommand(plugin))
            "warn" -> tree.punishment(name, "warn", PermissionKey.WARN, WarnCommand(plugin))
            "jail" -> tree.punishment(name, "jail", PermissionKey.JAIL, JailCommand(plugin))
            "kick" -> tree.punishment(name, "kick", PermissionKey.KICK, KickCommand(plugin), timed = false)

            "unban" -> {
                val target = Argument("target", values = tree::players)
                tree.create(name, legacy(PermissionKey.UNBAN), UnBanCommand(plugin), listOf(target), listOf(target, silent))
            }
            "unmute" -> tree.create(name, legacy(PermissionKey.UNMUTE), UnMuteCommand(plugin), listOf(player), listOf(player, silent))
            "unwarn" -> tree.create(name, legacy(PermissionKey.UNWARN), UnWarnCommand(plugin), listOf(player), listOf(player, silent))
            "unjail" -> tree.create(name, legacy(PermissionKey.UNJAIL), UnjailCommand(plugin), listOf(player), listOf(player, silent))
            "clearall" -> tree.create(name, legacy(PermissionKey.CLEAR_ALL), ClearAllCommand(plugin), listOf(player), listOf(player, silent))

            // A player may look at their own record without the permission.
            "check" -> {
                val type = Argument("type", values = { listOf("all", "ban", "jail", "mute", "warn") })
                tree.create(name, anyone, CheckCommand(plugin, plugin.playerIPManager), listOf(player), listOf(player, type))
            }
            "history" -> tree.create(name, anyone, HistoryCommand(plugin, plugin.playerIPManager), listOf(player), listOf(player, page))
            "banlist" -> {
                val history = Literal("--h")
                tree.create(name, legacy(PermissionKey.BAN_LIST), BanListCommand(plugin), listOf(page), listOf(history), listOf(history, page))
            }
            "change-reason" -> {
                val id = Argument("id")
                tree.create(name, legacy(PermissionKey.CHANGE_REASON), ChangeReasonCommand(plugin), listOf(id), listOf(id, Argument("reason", Kind.GREEDY)))
            }
            "unmark" -> {
                val who = Argument("player", values = { listOf("all") + tree.players() })
                val mark = Argument("mark")
                tree.create(
                    name, legacy(PermissionKey.UNMARK), UnmarkCommand(plugin),
                    listOf(Literal("pause"), mark), listOf(Literal("resume"), mark),
                    listOf(who), listOf(who, mark), listOf(who, mark, silent)
                )
            }

            "setjail" -> {
                val radius = Argument("radius", values = { listOf("5", "10", "20", "50") })
                tree.create(name, manage(PermissionKey.MANAGE_SET_JAIL), SetjailCommand(plugin), listOf(radius))
            }
            "setunjail" -> tree.create(name, legacy(PermissionKey.MANAGE_SET_SPAWN), SetSpawnCommand(plugin))
            "panel" -> tree.create(name, manage(PermissionKey.PANELS_COMMAND), PanelCommand(plugin))
            "langfix" -> tree.create(name, legacy(PermissionKey.PUNISHERX_COMMAND), PlaceholderFixCommand(plugin))
            "cache" -> tree.create(name, anyone, CacheCommand(plugin))

            "report" -> tree.create(name, anyone, ReportCommand(plugin), listOf(player), listOf(player, Argument("reason", Kind.GREEDY)))
            "reports" -> {
                val canManage = manage(PermissionKey.MANAGE_REPORTS)
                val canRead: (CommandSender) -> Boolean = { PermissionChecker.hasWithSee(it, PermissionKey.SEE_REPORTS) || canManage(it) }
                val id = Argument("id")
                val note = Argument("note", Kind.GREEDY)
                val forms = listOf("gui", "book", "list", "history").flatMap { listOf(listOf(Literal(it)), listOf(Literal(it), page)) } +
                    listOf(listOf(Literal("view"), id)) +
                    listOf("resolve", "reject").map { listOf(Literal(it, canManage), id, note) }
                tree.create(name, canRead, ReportsCommand(plugin), *forms.toTypedArray())
            }
            "note" -> {
                val canManage = manage(PermissionKey.MANAGE_NOTES)
                val canRead: (CommandSender) -> Boolean = { canManage(it) || PermissionChecker.hasWithSee(it, PermissionKey.SEE_NOTES) }
                tree.create(
                    name, canRead, NoteCommand(plugin),
                    listOf(Literal("list"), player), listOf(Literal("list"), player, page),
                    listOf(Literal("add", canManage), player, Argument("text", Kind.GREEDY)),
                    listOf(Literal("remove", canManage), player, Argument("id"))
                )
            }

            // help is open to everyone, the rest is checked again inside the command.
            "punisherx", "prx" -> {
                val admin = legacy(PermissionKey.PUNISHERX_COMMAND)
                val types = { listOf("mysql", "mariadb", "postgresql", "mssql", "sqlite") }
                val from = Argument("from", values = types)
                val to = Argument("to", values = types)
                val force = Literal("--force", admin)
                val migrate = Literal("migrate", admin)
                val simple = listOf("version", "reload", "export", "import", "diag", "diagnostics", "panel", "validate")
                    .map { listOf(Literal(it, admin)) }
                tree.create(
                    name, anyone, PunisherRootCommand(plugin),
                    listOf(Literal("help")), listOf(Literal("help"), page), *simple.toTypedArray(),
                    listOf(migrate, from), listOf(migrate, from, to), listOf(migrate, from, to, force),
                    listOf(migrate, from, force), listOf(migrate, from, force, to)
                )
            }
            else -> null
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
