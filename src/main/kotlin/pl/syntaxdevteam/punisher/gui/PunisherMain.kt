package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX

class PunisherMain(private val plugin: PunisherX) : GUI {

    private val sortTitleMaterialName = plugin.config.getString("GUI.sort.title", "Title1")!!
    private val sortMaterialName = plugin.config.getString("GUI.sort.material", sortTitleMaterialName)!!
    private val sortMaterial = Material.matchMaterial(sortMaterialName) ?: Material.PURPLE_WOOL
    private val sortIndex = plugin.config.getInt("GUI.sort.index", 11)

    private val jailTitleMaterialName = plugin.config.getString("GUI.jail.title", "§a§lTitle2")!!
    private val jailMaterialName = plugin.config.getString("GUI.jail.material", jailTitleMaterialName)!!
    private val jailMaterial = Material.matchMaterial(jailMaterialName) ?: Material.PAPER
    private val jailIndex = plugin.config.getInt("GUI.jail.index", 15)

    override fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 27, getTitle())

        val sortItem = createItem(sortMaterial, sortTitleMaterialName)
        val jailItem = createItem(jailMaterial, jailTitleMaterialName)

        inventory.setItem(sortIndex, sortItem)
        inventory.setItem(jailIndex, jailItem)

        player.openInventory(inventory)
    }
    //Tej sekcji nie zmieniamy pod żadnym pozorem poza uzupełnieniem kodu w "when (slot)"
    override fun handleClick(event: InventoryClickEvent) {
        val view = event.view
        val topSize = view.topInventory.size
        val slot = event.rawSlot

        if (slot !in 0..<topSize) return

        if (slot != sortIndex && slot != jailIndex) return

        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return

        when (slot) {
            sortIndex -> {
                player.sendMessage(plugin.messageHandler.getMessage("GUI", "opening_sort"))
                //Przyszła metoda wykonująca zadanie np. otwarcie kolejnego GUI np. PunisherSortGUI(plugin).open(player)
            }
            jailIndex -> {
                player.sendMessage(plugin.messageHandler.getMessage("GUI", "opening_jail"))
                //Jak wyżej...
            }
        }

    }

    override fun getTitle(): Component {
        return plugin.messageHandler.getLogMessage("GUI", "PunisherMain")
    }

    private fun createItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(plugin.messageHandler.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }
}