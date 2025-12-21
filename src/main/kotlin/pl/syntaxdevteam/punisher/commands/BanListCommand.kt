package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.text.SimpleDateFormat
import java.util.*

class BanListCommand(private val plugin: PunisherX) : BrigadierCommand {

    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")
    private val mh = plugin.messageHandler

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BAN_LIST)) {
            stack.sender.sendMessage(mh.stringMessageToComponent("error", "no_permission"))
            return
        }

        var page = 1
        var historyMode = false

        for (arg in args) {
            when {
                arg.equals("--h", ignoreCase = true) -> historyMode = true
                arg.toIntOrNull() != null -> page = arg.toInt()
            }
        }

        val limit = 10
        val offset = (page - 1) * limit

        val punishments = if (historyMode) {
            plugin.databaseHandler.getHistoryBannedPlayers(limit, offset)
        } else {
            plugin.databaseHandler.getBannedPlayers(limit, offset)
        }

        if (punishments.isEmpty()) {
            stack.sender.sendMessage(mh.stringMessageToComponent("banlist", "no_punishments"))
            plugin.logger.success(mh.stringMessageToString("banlist", "no_punishments"))
            return
        }

        val title = mh.stringMessageToComponentNoPrefix("banlist", "title")
        val topHeader = mh.stringMessageToComponentNoPrefix("banlist", "top_header")
        val tableHeader = mh.stringMessageToComponentNoPrefix("banlist", "table_header")
        val br = mh.miniMessageFormat("<blue> </blue>")
        val hr = mh.miniMessageFormat("<blue>|</blue>")

        stack.sender.sendMessage(br)
        stack.sender.sendMessage(title)
        stack.sender.sendMessage(topHeader)
        stack.sender.sendMessage(tableHeader)
        stack.sender.sendMessage(hr)

        punishments.forEach { punishment ->
            val formattedDate = dateFormat.format(Date(punishment.start))

            val punishmentMessage = mh.stringMessageToComponentNoPrefix("banlist", "ban_list", mapOf(
                "uuid" to punishment.uuid,
                "id" to punishment.id.toString(),
                "player" to punishment.name,
                "type" to punishment.type,
                "reason" to punishment.reason,
                "time" to formattedDate,
                "operator" to punishment.operator))
            stack.sender.sendMessage(punishmentMessage)
        }

        stack.sender.sendMessage(hr)
        stack.sender.sendMessage(hr)

        val nextPage = page + 1
        val prevPage = if (page > 1) page - 1 else 1
        val navigation = mh.miniMessageFormat(
            "<blue>| <click:run_command:'/banlist $prevPage'>[Previous]</click> " +
                    "<click:run_command:'/banlist $nextPage'>[Next]</click> </blue>"
        )
        stack.sender.sendMessage(navigation)
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BAN_LIST)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> listOf("--h") + (1..5).map { it.toString() } // Sugestie dla historii i numerów stron
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val history = Commands.literal("--h")
            .executes { context ->
                execute(context.source, listOf("--h"))
                1
            }
            .then(
                Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes { context ->
                        val page = IntegerArgumentType.getInteger(context, "page")
                        execute(context.source, listOf("--h", page.toString()))
                        1
                    }
            )

        val pageArg = Commands.argument("page", IntegerArgumentType.integer(1))
            .executes { context ->
                val page = IntegerArgumentType.getInteger(context, "page")
                execute(context.source, listOf(page.toString()))
                1
            }
            .then(history)

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.BAN_LIST))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(history)
            .then(pageArg)
            .build()
    }
}
