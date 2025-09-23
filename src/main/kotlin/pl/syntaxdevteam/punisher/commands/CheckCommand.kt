package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.players.PlayerIPManager

class CheckCommand(private val plugin: PunisherX, private val playerIPManager: PlayerIPManager) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("check", "usage"))
            return
        }
        val player = args[0]

        if (player.equals(stack.sender.name, ignoreCase = true) || PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.CHECK)) {
            if (args.size < 2) {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("check", "usage"))
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
                        stack.sender.sendMessage(plugin.messageHandler.getMessage("check", "invalid_type"))
                        return
                    }
                }

                if (filteredPunishments.isEmpty()) {
                    stack.sender.sendMessage(
                        plugin.messageHandler.getMessage("check", "no_punishments", mapOf("player" to player))
                    )
                } else {
                    val id = plugin.messageHandler.getCleanMessage("check", "id")
                    val types = plugin.messageHandler.getCleanMessage("check", "type")
                    val reasons = plugin.messageHandler.getCleanMessage("check", "reason")
                    val times = plugin.messageHandler.getCleanMessage("check", "time")
                    val title = plugin.messageHandler.getCleanMessage("check", "title")
                    val playerIP = playerIPManager.getPlayerIPByName(player)
                    plugin.logger.debug("Player IP: $playerIP")
                    val geoLocation = playerIP?.let { ip ->
                        val country = playerIPManager.geoIPHandler.getCountry(ip)
                        val city = playerIPManager.geoIPHandler.getCity(ip)
                        plugin.logger.debug("Country: $country, City: $city")
                        "$city, $country"
                    } ?: "Unknown location"
                    plugin.logger.debug("GeoLocation: $geoLocation")
                    val fullGeoLocation = when (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.VIEW_IP)) {
                        true -> "$playerIP ($geoLocation)"
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
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> listOf("all", "warn", "mute", "jail", "ban")
            else -> emptyList()
        }
    }
}
