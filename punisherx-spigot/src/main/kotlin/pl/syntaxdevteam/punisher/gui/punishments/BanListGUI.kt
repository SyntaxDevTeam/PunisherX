package pl.syntaxdevteam.punisher.gui.punishments
import pl.syntaxdevteam.punisher.compatibility.*

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.gui.interfaces.BaseGUI
import java.text.SimpleDateFormat
import java.util.Date

class BanListGUI(plugin: PunisherX) : BaseGUI(plugin) {
    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss")

    override fun open(player: Player) {
        open(player, 0)
    }

    private fun open(player: Player, page: Int) {
        val limit = 27
        val offset = page * limit
        val punishments = plugin.databaseHandler.getBannedPlayers(limit, offset)
        val hasNext = plugin.databaseHandler.getBannedPlayers(1, offset + limit).isNotEmpty()

        val gui = createGui(5)

        punishments.forEachIndexed { index, punishment ->
            gui.setItem(index, createGuiItem(createHead(punishment)))
        }

        if (page > 0) {
            gui.setItem(36, createNavGuiItem(Material.PAPER, mH.stringMessageToStringNoPrefix("GUI", "Nav.previous")) { clicker ->
                open(clicker, page - 1)
            })
        }
        gui.setItem(40, createNavGuiItem(Material.BARRIER, mH.stringMessageToStringNoPrefix("GUI", "Nav.back")) { clicker ->
            PunishedListGUI(plugin).open(clicker)
        })
        if (hasNext) {
            gui.setItem(44, createNavGuiItem(Material.BOOK, mH.stringMessageToStringNoPrefix("GUI", "Nav.next")) { clicker ->
                open(clicker, page + 1)
            })
        }

        gui.open(player)
    }

    private fun createHead(punishment: PunishmentData): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as SkullMeta
        val offline = Bukkit.getOfflinePlayer(plugin.uuidManager.getUUID(punishment.name))
        meta.owningPlayer = offline
        meta.displayName(mH.formatMixedTextToMiniMessage("<yellow>${punishment.name}</yellow>", TagResolver.empty()))
        val formattedDate = dateFormat.format(Date(punishment.start))
        val remaining = if (punishment.end == -1L) {
            mH.stringMessageToStringNoPrefix("GUI", "BanList.permanent")
        } else {
            plugin.timeHandler.formatTime(((punishment.end - System.currentTimeMillis()) / 1000).toString())
        }
        meta.lore(
            listOf(
                mH.stringMessageToComponentNoPrefix("GUI", "BanList.hover.id", mapOf("id" to punishment.id.toString())),
                mH.stringMessageToComponentNoPrefix("GUI", "BanList.hover.date", mapOf("date" to formattedDate)),
                mH.stringMessageToComponentNoPrefix("GUI", "BanList.hover.remaining", mapOf("time" to remaining)),
                mH.stringMessageToComponentNoPrefix("GUI", "BanList.hover.operator", mapOf("operator" to punishment.operator)),
                mH.stringMessageToComponentNoPrefix("GUI", "BanList.hover.reason", mapOf("reason" to punishment.reason))
            )
        )
        head.itemMeta = meta
        return head
    }

    override fun getTitle(): Component {
        return mH.stringMessageToComponentNoPrefix("GUI", "BanList.title")
    }
}
