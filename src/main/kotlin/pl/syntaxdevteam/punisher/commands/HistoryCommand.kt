package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.MessageHandler
import pl.syntaxdevteam.punisher.common.UUIDManager
import pl.syntaxdevteam.punisher.players.PlayerIPManager
import java.text.SimpleDateFormat
import java.util.*

@Suppress("UnstableApiUsage")
class HistoryCommand(private val plugin: PunisherX, pluginMetas: PluginMeta, private val playerIPManager: PlayerIPManager) :
    BasicCommand {

    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)
    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            val player = args[0]
            val page = if (args.size > 1) args[1].toIntOrNull() ?: 1 else 1
            val senderName = stack.sender.name
            val limit = 10
            val offset = (page - 1) * limit

            if (player.equals(senderName, ignoreCase = true) || stack.sender.hasPermission("punisherx.history")) {
                val uuid = uuidManager.getUUID(player)
                val targetPlayer = when (Bukkit.getPlayer(player)?.name) {
                    null -> Bukkit.getOfflinePlayer(uuid).name
                    else -> Bukkit.getPlayer(player)?.name
                }
                val punishments = plugin.databaseHandler.getPunishmentHistory(uuid.toString(), limit, offset)

                if (punishments.isEmpty()) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("history", "no_punishments", mapOf("player" to player)))
                } else {
                    val id = messageHandler.getLogMessage("history", "id")
                    val types = messageHandler.getLogMessage("history", "type")
                    val reasons = messageHandler.getLogMessage("history", "reason")
                    val times = messageHandler.getLogMessage("history", "time")
                    val title = messageHandler.getLogMessage("history", "title")
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
                    val topHeader = miniMessage.deserialize("<blue>--------------------------------------------------</blue>")
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
                        val punishmentMessage = miniMessage.deserialize("<blue>|   <white>#${punishment.id}</white> <blue>|</blue> <white>${punishment.type}</white> <blue>|</blue> <white>${punishment.reason}</white> <blue>|</blue> <white>$formattedDate</blue>")
                        stack.sender.sendMessage(punishmentMessage)
                    }
                    stack.sender.sendMessage(hr)

                    val nextPage = page + 1
                    val prevPage = if (page > 1) page - 1 else 1
                    val navigation = miniMessage.deserialize("<blue>| <click:run_command:'/history $player $prevPage'>[Previous]</click>   <click:run_command:'/history $player $nextPage'>[Next]</click> </blue>")
                    stack.sender.sendMessage(navigation)
                }
            } else {
                stack.sender.sendRichMessage(messageHandler.getMessage("history", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("history", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
