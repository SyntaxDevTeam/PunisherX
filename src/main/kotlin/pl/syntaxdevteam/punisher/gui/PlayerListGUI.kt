package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import pl.syntaxdevteam.punisher.PunisherX

/**
 * GUI displaying currently online players.
 */
class PlayerListGUI(private val plugin: PunisherX) : GUI {

    override fun open(player: Player) {
        val online = plugin.server.onlinePlayers.toList()
        val size = ((online.size - 1) / 9 + 1) * 9
        val inventory = Bukkit.createInventory(null, if (size == 0) 9 else size, getTitle())

        online.forEachIndexed { index, target ->
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = target
            meta.displayName(plugin.messageHandler.formatMixedTextToMiniMessage("<yellow>${target.name}</yellow>", TagResolver.empty()))
            meta.lore(
                listOf(
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Czas online: <green>5m</green>", TagResolver.empty()),
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Czas łączny: <green>1h</green>", TagResolver.empty()),
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Kara: <red>Brak</red>", TagResolver.empty()),
                    plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Łącznie kar: <yellow>0</yellow>", TagResolver.empty())
                )
            )
            head.itemMeta = meta
            inventory.setItem(index, head)
        }

        player.openInventory(inventory)
    }

    override fun handleClick(event: InventoryClickEvent) {
        event.isCancelled = true
    }

    override fun getTitle(): Component {
        return plugin.messageHandler.formatMixedTextToMiniMessage("<gray>Gracze online</gray>", TagResolver.empty())
    }
}