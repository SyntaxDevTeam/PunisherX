package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.players.PlayerIPManager
import java.text.SimpleDateFormat
import java.util.*

class HistoryCommand(private val plugin: PunisherX, private val playerIPManager: PlayerIPManager) : BasicCommand {

    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("history", "usage"))
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
                    plugin.messageHandler.getMessage(
                        "history",
                        "no_punishments",
                        mapOf("player" to player)
                    )
                )
            } else {
                val id = plugin.messageHandler.getCleanMessage("history", "id")
                val types = plugin.messageHandler.getCleanMessage("history", "type")
                val reasons = plugin.messageHandler.getCleanMessage("history", "reason")
                val times = plugin.messageHandler.getCleanMessage("history", "time")
                val title = plugin.messageHandler.getCleanMessage("history", "title")
                val playerIP = playerIPManager.getPlayerIPByName(player)
                plugin.logger.debug("Player IP: $playerIP")
                val geoLocation = playerIP?.let { ip ->
                    val country = playerIPManager.geoIPHandler.getCountry(ip)
                    val city = playerIPManager.geoIPHandler.getCity(ip)
                    plugin.logger.debug("Country: $country, City: $city")
                    "$city, $country"
                } ?: "Unknown location"
                plugin.logger.debug("GeoLocation: $geoLocation")
                val fullGeoLocation = when (stack.sender.hasPermission("punisherx.view_ip")) {
                    true -> "$playerIP ($geoLocation)"
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
            stack.sender.sendMessage(plugin.messageHandler.getMessage("history", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.HISTORY)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
