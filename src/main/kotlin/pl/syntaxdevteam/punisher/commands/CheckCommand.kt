package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.players.PlayerIPManager

@Suppress("UnstableApiUsage")
class CheckCommand(private val plugin: PunisherX, private val playerIPManager: PlayerIPManager) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if(stack.sender.hasPermission("punisherx.check")){
            if (args.isNotEmpty()) {
                val player = args[0]
                val senderName = stack.sender.name

                if (player.equals(senderName, ignoreCase = true)) {
                    if (args.size < 2) {
                        stack.sender.sendRichMessage(plugin.messageHandler.getMessage("check", "usage"))
                    } else {
                        val type = args[1]
                        val uuid = plugin.uuidManager.getUUID(player)
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
                                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("check", "invalid_type"))
                                return
                            }
                        }

                        if (filteredPunishments.isEmpty()) {
                            stack.sender.sendRichMessage(
                                plugin.messageHandler.getMessage(
                                    "check",
                                    "no_punishments",
                                    mapOf("player" to player)
                                )
                            )
                        } else {
                            val id = plugin.messageHandler.getLogMessage("check", "id")
                            val types = plugin.messageHandler.getLogMessage("check", "type")
                            val reasons = plugin.messageHandler.getLogMessage("check", "reason")
                            val times = plugin.messageHandler.getLogMessage("check", "time")
                            val title = plugin.messageHandler.getLogMessage("check", "title")
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
                            val tableHeader =
                                miniMessage.deserialize("<blue>|   $id  |  $types  |  $reasons  |  $times</blue>")
                            val br = miniMessage.deserialize("<blue> </blue>")
                            val hr = miniMessage.deserialize("<blue>|</blue>")
                            stack.sender.sendMessage(br)
                            stack.sender.sendMessage(header)
                            stack.sender.sendMessage(topHeader)
                            stack.sender.sendMessage(tableHeader)
                            stack.sender.sendMessage(hr)

                            filteredPunishments.forEach { punishment ->
                                val endTime = punishment.end
                                val remainingTime = (endTime - System.currentTimeMillis()) / 1000
                                val duration =
                                    if (endTime == -1L) "permanent" else plugin.timeHandler.formatTime(remainingTime.toString())
                                val reason = punishment.reason
                                val row =
                                    miniMessage.deserialize("<blue>|   <white>#${punishment.id}</white> <blue>|</blue> <white>${punishment.type}</white> <blue>|</blue> <white>$reason</white> <blue>|</blue> <white>$duration</white>")
                                stack.sender.sendMessage(row)
                            }
                        }
                    }
                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("check", "usage"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!stack.sender.hasPermission("punisherx.check")) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> listOf("all", "warn", "mute", "jail", "ban")
            else -> emptyList()
        }
    }
}
