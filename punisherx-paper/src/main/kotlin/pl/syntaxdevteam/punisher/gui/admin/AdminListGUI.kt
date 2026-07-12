package pl.syntaxdevteam.punisher.gui.admin

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.gui.PunisherMain
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import pl.syntaxdevteam.punisher.gui.stats.PlayerStatsService
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

/**
 * GUI displaying currently online administrators.
 */
class AdminListGUI(plugin: PunisherX) : BaseGUI(plugin) {

    override fun open(player: Player) {
        open(player, 0)
    }

    /**
     * Opens the admin list GUI for the given page.
     */
    private fun open(player: Player, page: Int) {
        val online = plugin.server.onlinePlayers.filter {
            PermissionChecker.hasPermissionStartingWith(it, "punisherx")
        }
        val playersPerPage = 27 // 3 rows of heads
        val totalPages = if (online.isEmpty()) 1 else (online.size - 1) / playersPerPage + 1
        val currentPage = page.coerceIn(0, totalPages - 1)

        val startIndex = currentPage * playersPerPage
        val playersPage = online.drop(startIndex).take(playersPerPage)

        val gui = createGui(5)

        playersPage.forEachIndexed { index, target ->
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = target
            meta.displayName(
                mH.formatMixedTextToMiniMessage("<yellow>${target.name}</yellow>", TagResolver.empty())
            )
            val loadMsg = mH.stringMessageToStringNoPrefix("GUI", "PlayerList.loading")
            meta.lore(
                listOf(
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.uuid", mapOf("uuid" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.playerIP", mapOf("playerip" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.onlineStr", mapOf("onlinestr" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.totalStr", mapOf("totalstr" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.lastActive", mapOf("lastactive" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishments", mapOf("punishments" to loadMsg)),
                    mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishStr", mapOf("punishstr" to loadMsg)),
                )
            )
            head.itemMeta = meta
            gui.setItem(index, createGuiItem(head))

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val uuid = target.uniqueId
                val onlineStr  = PlayerStatsService.getCurrentOnlineString(uuid) ?: "Brak danych"
                val totalStr   = PlayerStatsService.getTotalPlaytimeString(uuid) ?: "Brak danych"
                val punishStr  = plugin.databaseHandler.countPlayerAllPunishmentHistory(uuid).toString()
                val playerIP   = plugin.playerIPManager.getPlayerIPByName(target.name) ?: "Brak danych"
                val punishments = plugin.databaseHandler.getActivePunishmentsString(uuid) ?: mH.stringMessageToStringNoPrefix("error", "no_data")
                val lastActive = PlayerStatsService.getLastActiveString(uuid) ?: mH.stringMessageToStringNoPrefix("error", "no_data")

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!gui.inventory.viewers.contains(player)) return@Runnable
                    val item = gui.inventory.getItem(index) ?: return@Runnable
                    val im = item.itemMeta as SkullMeta
                    im.lore(
                        listOf(
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.uuid", mapOf("uuid" to target.uniqueId.toString())),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.playerIP", mapOf("playerip" to playerIP)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.onlineStr", mapOf("onlinestr" to onlineStr)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.totalStr", mapOf("totalstr" to totalStr)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.lastActive", mapOf("lastactive" to lastActive)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishments", mapOf("punishments" to punishments)),
                            mH.stringMessageToComponentNoPrefix("GUI", "PlayerList.hover.punishStr", mapOf("punishstr" to punishStr)),
                        )
                    )
                    item.itemMeta = im
                    gui.updateItem(index, item)
                })
            })
        }

        if (currentPage > 0) {
            gui.setItem(36, createNavGuiItem(Material.PAPER, "<yellow>Poprzednia strona</yellow>") { clicker ->
                open(clicker, currentPage - 1)
            })
        }

        gui.setItem(40, createNavGuiItem(Material.BARRIER, "<yellow>Powrót</yellow>") { clicker ->
            PunisherMain(plugin).open(clicker)
        })

        if (currentPage < totalPages - 1) {
            gui.setItem(44, createNavGuiItem(Material.BOOK, "<yellow>Następna strona</yellow>") { clicker ->
                open(clicker, currentPage + 1)
            })
        }

        gui.open(player)
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "PunisherMain.adminOnline.title")
    }
}
