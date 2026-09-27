package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.DialogSupport
import pl.syntaxdevteam.punisher.dialogs.PunishmentListDialogService
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.players.PlayerIPManager
import java.text.SimpleDateFormat
import java.util.*

class HistoryCommand(private val plugin: PunisherX, private val playerIPManager: PlayerIPManager) : BasicCommand {

    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("history", "usage"))
            return
        }

        val player = args[0]
        if (player.equals(stack.sender.name, ignoreCase = true) || PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.HISTORY)) {

            val page = (if (args.size > 1) args[1].toIntOrNull() ?: 1 else 1).coerceAtLeast(1)
            val limit = 10
            val offset = (page - 1) * limit

            val uuid = plugin.resolvePlayerUuid(player)
            val targetPlayer = when (Bukkit.getPlayer(player)?.name) {
                null -> Bukkit.getOfflinePlayer(uuid).name
                else -> Bukkit.getPlayer(player)?.name
            }

            if (DialogSupport.canUseListDialogs(plugin, stack.sender)) {
                openDialog(stack.sender as Player, targetPlayer ?: player, uuid.toString(), page)
                return
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

    private fun openDialog(viewer: Player, targetName: String, uuid: String, page: Int) {
        val pageSize = plugin.config.getInt("dialogs.list-page-size", 10).coerceIn(1, 20)
        val offset = (page - 1) * pageSize
        val punishments = plugin.databaseHandler.getPunishmentHistory(uuid, pageSize + 1, offset)
        PunishmentListDialogService(plugin).open(
            player = viewer,
            title = plugin.messageHandler.stringMessageToComponentNoPrefix("history", "title"),
            subtitle = Component.text("$targetName • $uuid • #$page"),
            entries = punishments.take(pageSize),
            page = page,
            hasNext = punishments.size > pageSize,
            loadPage = { requestedPage -> openDialog(viewer, targetName, uuid, requestedPage) }
        )
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.HISTORY)) {
            return emptyList()
        }
        val input = args.lastOrNull().orEmpty()
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(input, ignoreCase = true) }
            else -> emptyList()
        }
    }
}
