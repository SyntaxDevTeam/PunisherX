package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.players.PlayerIPManager

class CheckCommand(private val plugin: PunisherX, private val playerIPManager: PlayerIPManager) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("check", "usage"))
            return
        }
        val player = args[0]

        if (player.equals(stack.sender.name, ignoreCase = true) || PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHECK)) {
            if (args.size < 2) {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("check", "usage"))
            } else {
                val type = args[1]
                val uuid = plugin.resolvePlayerUuid(player)
                val targetPlayer = when (Bukkit.getPlayer(player)?.name) {
                    null -> Bukkit.getOfflinePlayer(uuid).name
                    else -> Bukkit.getPlayer(player)?.name
                }
                val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
                val filteredPunishments = when (type.lowercase()) {
                    "all" -> punishments
                    "ban" -> punishments.filter { it.type == "BAN" || it.type == "BANIP" }
                    "jail" -> punishments.filter { it.type == "JAIL" }
                    "mute" -> punishments.filter { it.type == "MUTE" }
                    "warn" -> punishments.filter { it.type == "WARN" }
                    else -> {
                        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("check", "invalid_type"))
                        return
                    }
                }

                if (filteredPunishments.isEmpty()) {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent("check", "no_punishments", mapOf("player" to player))
                    )
                } else {
                    val id = plugin.messageHandler.stringMessageToStringNoPrefix("check", "id")
                    val types = plugin.messageHandler.stringMessageToStringNoPrefix("check", "type")
                    val reasons = plugin.messageHandler.stringMessageToStringNoPrefix("check", "reason")
                    val times = plugin.messageHandler.stringMessageToStringNoPrefix("check", "time")
                    val title = plugin.messageHandler.stringMessageToStringNoPrefix("check", "title")
                    val playerInfo = playerIPManager.getPlayerInfoByName(player)
                    plugin.logger.debug("Player info: $playerInfo")
                    val playerIP = playerInfo?.playerIP
                    val geoLocation = playerInfo?.geoLocation?.takeIf { it.isNotBlank() } ?: "Unknown location"
                    plugin.logger.debug("GeoLocation: $geoLocation")
                    val fullGeoLocation = when (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.VIEW_IP)) {
                        true -> playerIP?.let { "$it ($geoLocation)" } ?: geoLocation
                        else -> geoLocation
                    }
                    val gamer = if (stack.sender.name == "CONSOLE") {
                        "<gold>$targetPlayer <gray>[$uuid, $fullGeoLocation]</gray>:</gold>"
                    } else {
                        "<gold><hover:show_text:'[<white>$uuid, $fullGeoLocation</white>]'>$targetPlayer:</gold>"
                    }
                    val mh = plugin.messageHandler
                    val topHeader = mh.miniMessageFormat("<blue>--------------------------------------------------</blue>")
                    val header = mh.miniMessageFormat("<blue>|    $title $gamer</blue>")
                    val tableHeader = mh.miniMessageFormat("<blue>|   $id  |  $types  |  $reasons  |  $times</blue>")
                    val br = mh.miniMessageFormat("<blue> </blue>")
                    val hr = mh.miniMessageFormat("<blue>|</blue>")
                    stack.sender.sendMessage(br)
                    stack.sender.sendMessage(header)
                    stack.sender.sendMessage(topHeader)
                    stack.sender.sendMessage(tableHeader)
                    stack.sender.sendMessage(hr)

                    filteredPunishments.forEach { punishment ->
                        val endTime = punishment.end
                        val remainingTime = (endTime - System.currentTimeMillis()) / 1000
                        val duration = if (endTime == -1L) "permanent" else plugin.timeHandler.formatTime(remainingTime.toString())
                        val reason = punishment.reason
                        val row = mh.miniMessageFormat(
                            "<blue>|   <white>#${punishment.id}</white> <blue>|</blue> <white>${punishment.type}</white> " +
                                    "<blue>|</blue> <white>$reason</white> <blue>|</blue> <white>$duration</white>"
                        )
                        stack.sender.sendMessage(row)
                    }
                }
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> listOf("all", "warn", "mute", "jail", "ban")
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
                Commands.argument("type", StringArgumentType.word())
                    .suggests(BrigadierCommandUtils.suggestions(this) { context ->
                        listOf(StringArgumentType.getString(context, "player"), "")
                    })
                    .executes { context ->
                        val player = StringArgumentType.getString(context, "player")
                        val type = StringArgumentType.getString(context, "type")
                        execute(context.source, listOf(player, type))
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
