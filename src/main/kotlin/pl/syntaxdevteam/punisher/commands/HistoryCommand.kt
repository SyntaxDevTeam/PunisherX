package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.players.PlayerIPManager
import java.text.SimpleDateFormat
import java.util.*

class HistoryCommand(private val plugin: PunisherX, private val playerIPManager: PlayerIPManager) : BrigadierCommand {

    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")

    override fun execute(stack: CommandSourceStack, args: List<String>) {

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("history", "usage"))
            return
        }

        val player = args[0]
        if (player.equals(stack.sender.name, ignoreCase = true) || PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.HISTORY)) {

            val page = if (args.size > 1) args[1].toIntOrNull() ?: 1 else 1
            val limit = 10
            val offset = (page - 1) * limit

            val uuid = plugin.resolvePlayerUuid(player)
            val targetPlayer = when (Bukkit.getPlayer(player)?.name) {
                null -> Bukkit.getOfflinePlayer(uuid).name
                else -> Bukkit.getPlayer(player)?.name
            }
            val punishments = plugin.databaseHandler.getPunishmentHistory(uuid.toString(), limit, offset)

            if (punishments.isEmpty()) {
                stack.sender.sendMessage(
                    plugin.messageHandler.stringMessageToComponent(
                        "history",
                        "no_punishments",
                        mapOf("player" to player)
                    )
                )
            } else {
                val id = plugin.messageHandler.stringMessageToStringNoPrefix("history", "id")
                val types = plugin.messageHandler.stringMessageToStringNoPrefix("history", "type")
                val reasons = plugin.messageHandler.stringMessageToStringNoPrefix("history", "reason")
                val times = plugin.messageHandler.stringMessageToStringNoPrefix("history", "time")
                val title = plugin.messageHandler.stringMessageToStringNoPrefix("history", "title")
                val playerInfo = playerIPManager.getPlayerInfoByName(player)
                plugin.logger.debug("Player info: $playerInfo")
                val playerIP = playerInfo?.playerIP
                val geoLocation = playerInfo?.geoLocation?.takeIf { it.isNotBlank() } ?: "Unknown location"
                plugin.logger.debug("GeoLocation: $geoLocation")
                val fullGeoLocation = when (stack.sender.hasPermission("punisherx.view_ip")) {
                    true -> playerIP?.let { "$it ($geoLocation)" } ?: geoLocation
                    else -> geoLocation
                }
                val gamer = if (stack.sender.name == "CONSOLE") {
                    "<gold>$targetPlayer <gray>[$uuid, $fullGeoLocation]</gray>:</gold>"
                } else {
                    "<gold><hover:show_text:'[<white>$uuid, $fullGeoLocation</white>]'>$targetPlayer:</gold>"
                }
                val mh = plugin.messageHandler
                val topHeader =
                    mh.miniMessageFormat("<blue>--------------------------------------------------</blue>")
                val header = mh.miniMessageFormat("<blue>|    $title $gamer</blue>")
                val tableHeader = mh.miniMessageFormat("<blue>|   $id  |  $types  |  $reasons  |  $times</blue>")
                val br = mh.miniMessageFormat("<blue> </blue>")
                val hr = mh.miniMessageFormat("<blue>|</blue>")
                stack.sender.sendMessage(br)
                stack.sender.sendMessage(header)
                stack.sender.sendMessage(topHeader)
                stack.sender.sendMessage(tableHeader)
                stack.sender.sendMessage(hr)

                punishments.forEach { punishment ->
                    val formattedDate = dateFormat.format(Date(punishment.start))
                    val punishmentMessage =
                        mh.miniMessageFormat("<blue>|   <white>#${punishment.id}</white> <blue>|</blue> <white>${punishment.type}</white> <blue>|</blue> <white>${punishment.reason}</white> <blue>|</blue> <white>$formattedDate</blue>")
                    stack.sender.sendMessage(punishmentMessage)
                }
                stack.sender.sendMessage(hr)

                val nextPage = page + 1
                val prevPage = if (page > 1) page - 1 else 1
                val navigation =
                    mh.miniMessageFormat("<blue>| <click:run_command:'/history $player $prevPage'>[Previous]</click>   <click:run_command:'/history $player $nextPage'>[Next]</click> </blue>")
                stack.sender.sendMessage(navigation)
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("history", "no_permission"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.HISTORY)) {
            return emptyList()
        }
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val playerArg = Commands.argument("player", StringArgumentType.word())
            .suggests(BrigadierCommandUtils.suggestions(this) { emptyList() })
            .executes { context ->
                val player = StringArgumentType.getString(context, "player")
                execute(context.source, listOf(player))
                1
            }
            .then(
                Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes { context ->
                        val player = StringArgumentType.getString(context, "player")
                        val page = IntegerArgumentType.getInteger(context, "page")
                        execute(context.source, listOf(player, page.toString()))
                        1
                    }
            )

        return Commands.literal(name)
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(playerArg)
            .build()
    }
}
