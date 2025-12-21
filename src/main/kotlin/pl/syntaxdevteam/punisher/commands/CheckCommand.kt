package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentCheckType
import pl.syntaxdevteam.punisher.commands.arguments.PunishmentCheckTypeArgumentType
import pl.syntaxdevteam.punisher.databases.PunishmentData
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
                val resolvedType = PunishmentCheckType.entries
                    .firstOrNull { it.name.equals(type, ignoreCase = true) }
                    ?: run {
                        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("check", "invalid_type"))
                        return
                    }
                executeCheck(stack, player, resolvedType)
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
        val playerArg = Commands.argument("player", ArgumentTypes.playerProfiles())
            .executes { context ->
                BrigadierCommandUtils.resolvePlayerProfileNames(context, "player").forEach { player ->
                    execute(context.source, listOf(player))
                }
                1
            }
            .then(
                Commands.argument("type", PunishmentCheckTypeArgumentType.checkType())
                    .executes { context ->
                        val type = PunishmentCheckTypeArgumentType.getCheckType(context, "type")
                        BrigadierCommandUtils.resolvePlayerProfileNames(context, "player").forEach { player ->
                            executeCheck(context.source, player, type)
                        }
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

    private fun executeCheck(stack: CommandSourceStack, player: String, type: PunishmentCheckType) {
        if (player.equals(stack.sender.name, ignoreCase = true) || PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHECK)) {
            val uuid = plugin.resolvePlayerUuid(player)
            val targetPlayer = when (Bukkit.getPlayer(player)?.name) {
                null -> Bukkit.getOfflinePlayer(uuid).name
                else -> Bukkit.getPlayer(player)?.name
            }
            val punishments = plugin.databaseHandler.getPunishments(uuid.toString())
            val filteredPunishments = filterPunishments(punishments, type)
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
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    private fun filterPunishments(
        punishments: List<PunishmentData>,
        type: PunishmentCheckType
    ): List<PunishmentData> {
        return when (type) {
            PunishmentCheckType.ALL -> punishments
            PunishmentCheckType.BAN -> punishments.filter { it.type == "BAN" || it.type == "BANIP" }
            PunishmentCheckType.JAIL -> punishments.filter { it.type == "JAIL" }
            PunishmentCheckType.MUTE -> punishments.filter { it.type == "MUTE" }
            PunishmentCheckType.WARN -> punishments.filter { it.type == "WARN" }
        }
    }
}
