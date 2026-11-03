package pl.syntaxdevteam.punisher.gui.interfaces

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import pl.syntaxdevteam.punisher.PunisherX

abstract class BaseGUI(protected val plugin: PunisherX) : GUI {
    protected val mH = plugin.messageHandler
    private val generation = plugin.guiRuntime.generation
    private data class HeadBinding(val slot: Int, val uuid: java.util.UUID, val name: String)
    private val heads = java.util.IdentityHashMap<Gui, MutableList<HeadBinding>>()

    /** The only player-head path for panels: never assign an incomplete owner/profile. */
    protected fun setPlayerHead(gui: Gui, slot: Int, item: ItemStack, uuid: java.util.UUID?, name: String,
                                onClick: ((Player) -> Unit)? = null) {
        // IP bans have no player UUID. This key is for appearance only, never for punishment actions.
        val skinKey = uuid ?: java.util.UUID.nameUUIDFromBytes("PunisherX:head:$name".toByteArray(Charsets.UTF_8))
        val meta = item.itemMeta as org.bukkit.inventory.meta.SkullMeta
        plugin.localPlayerSkins.apply(skinKey, meta)
        item.itemMeta = meta
        gui.setItem(slot, createGuiItem(item, onClick))
        heads.getOrPut(gui) { mutableListOf() }.add(HeadBinding(slot, skinKey, name))
    }

    protected fun openGui(gui: Gui, player: Player) {
        if (generation != plugin.guiRuntime.generation) return
        plugin.guiRuntime.track(gui.inventory)
        gui.open(player)
        heads.remove(gui)?.forEach { head ->
            plugin.localPlayerSkins.resolve(head.uuid, head.name).thenAccept { available ->
                if (available && generation == plugin.guiRuntime.generation) {
                    plugin.schedulerAdapter.runForPlayer(player, Runnable {
                        if (generation != plugin.guiRuntime.generation || !player.isOnline ||
                            player.openInventory.topInventory !== gui.inventory) return@Runnable
                        val item = gui.inventory.getItem(head.slot) ?: return@Runnable
                        val meta = item.itemMeta as? org.bukkit.inventory.meta.SkullMeta ?: return@Runnable
                        plugin.localPlayerSkins.apply(head.uuid, meta)
                        item.itemMeta = meta
                        gui.updateItem(head.slot, item)
                    })
                }
            }
        }
    }

    protected fun createGui(rows: Int, title: Component = getTitle()): Gui {
        return Gui.gui()
            .title(title)
            .rows(rows)
            .disableAllInteractions()
            .create()
            .also { gui -> gui.filler.fill(createGuiItem(createFillerItem())) }
    }

    protected fun createGuiItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList(),
        onClick: ((Player) -> Unit)? = null
    ): GuiItem {
        return createGuiItem(createItem(material, name, lore), onClick)
    }

    protected fun createNavGuiItem(
        material: Material,
        name: String,
        onClick: ((Player) -> Unit)? = null
    ): GuiItem {
        return createGuiItem(createNavItem(material, name), onClick)
    }

    protected fun createGuiItem(
        item: ItemStack,
        onClick: ((Player) -> Unit)? = null
    ): GuiItem {
        return ItemBuilder.from(item).asGuiItem { event ->
            event.isCancelled = true
            if (generation != plugin.guiRuntime.generation) return@asGuiItem
            val player = event.whoClicked as? Player ?: return@asGuiItem
            onClick?.invoke(player)
        }
    }

    protected fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        item.itemMeta = meta
        return item
    }

    protected fun createItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList()
    ): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mH.formatMixedTextToMiniMessage(name, TagResolver.empty()))
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { mH.formatMixedTextToMiniMessage(it, TagResolver.empty()) })
        }
        item.itemMeta = meta
        return item
    }

    protected fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(Component.text(" "))
        item.itemMeta = meta
        return item
    }

    protected fun Inventory.fillWithFiller() {
        for (slot in 0 until size) {
            setItem(slot, createFillerItem())
        }
    }
}
