package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.players.PlayerIPManager
import java.text.SimpleDateFormat
import java.util.*

@Suppress("UnstableApiUsage")
class HistoryCommand(private val plugin: PunisherX, private val playerIPManager: PlayerIPManager) : BasicCommand {

    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if(stack.sender.hasPermission("punisherx.history")) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val page = if (args.size > 1) args[1].toIntOrNull() ?: 1 else 1
                val senderName = stack.sender.name
                val limit = 10
                val offset = (page - 1) * limit

                if (player.equals(senderName, ignoreCase = true)) {
                    val uuid = plugin.uuidManager.getUUID(player)
                    val targetPlayer = when (Bukkit.getPlayer(player)?.name) {
                        null -> Bukkit.getOfflinePlayer(uuid).name
                        else -> Bukkit.getPlayer(player)?.name
                    }
                    val punishments = plugin.databaseHandler.getPunishmentHistory(uuid.toString(), limit, offset)

                    if (punishments.isEmpty()) {
                        stack.sender.sendRichMessage(
                            plugin.messageHandler.getMessage(
                                "history",
                                "no_punishments",
                                mapOf("player" to player)
                            )
                        )
                    } else {
                        val id = plugin.messageHandler.getLogMessage("history", "id")
                        val types = plugin.messageHandler.getLogMessage("history", "type")
                        val reasons = plugin.messageHandler.getLogMessage("history", "reason")
                        val times = plugin.messageHandler.getLogMessage("history", "time")
                        val title = plugin.messageHandler.getLogMessage("history", "title")
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
                        val miniMessage = MiniMessage.miniMessage()
                        val topHeader =
                            miniMessage.deserialize("<blue>--------------------------------------------------</blue>")
                        val header = miniMessage.deserialize("<blue>|    $title $gamer</blue>")
                        val tableHeader = miniMessage.deserialize("<blue>|   $id  |  $types  |  $reasons  |  $times</blue>")
                        val br = miniMessage.deserialize("<blue> </blue>")
                        val hr = miniMessage.deserialize("<blue>|</blue>")
                        stack.sender.sendMessage(br)
                        stack.sender.sendMessage(header)
                        stack.sender.sendMessage(topHeader)
                        stack.sender.sendMessage(tableHeader)
                        stack.sender.sendMessage(hr)

                        punishments.forEach { punishment ->
                            val formattedDate = dateFormat.format(Date(punishment.start))
                            val punishmentMessage =
                                miniMessage.deserialize("<blue>|   <white>#${punishment.id}</white> <blue>|</blue> <white>${punishment.type}</white> <blue>|</blue> <white>${punishment.reason}</white> <blue>|</blue> <white>$formattedDate</blue>")
                            stack.sender.sendMessage(punishmentMessage)
                        }
                        stack.sender.sendMessage(hr)

                        val nextPage = page + 1
                        val prevPage = if (page > 1) page - 1 else 1
                        val navigation =
                            miniMessage.deserialize("<blue>| <click:run_command:'/history $player $prevPage'>[Previous]</click>   <click:run_command:'/history $player $nextPage'>[Next]</click> </blue>")
                        stack.sender.sendMessage(navigation)
                    }
                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("history", "usage"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("history", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!stack.sender.hasPermission("punisherx.history")) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
