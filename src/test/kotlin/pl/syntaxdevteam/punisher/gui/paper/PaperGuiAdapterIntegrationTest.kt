package pl.syntaxdevteam.punisher.gui.paper

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PaperGuiAdapterIntegrationTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: JavaPlugin
    private lateinit var player: PlayerMock
    private lateinit var adapter: PaperGuiAdapter
    private lateinit var screen: DemoScreen

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin()
        player = server.addPlayer()
        adapter = PaperGuiAdapter(plugin)
        screen = DemoScreen()
        adapter.register(screen).registerListener()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `open renders demo screen`() {
        adapter.open(player, DemoScreen.ID)

        val top = player.openInventory.topInventory
        assertEquals(Component.text("Demo screen"), player.openInventory.title())
        assertNotNull(top.getItem(13))
        assertEquals(Material.EMERALD, top.getItem(13)?.type)
        assertEquals(1, screen.renderCount)
    }

    @Test
    fun `click is routed through adapter listener`() {
        adapter.open(player, DemoScreen.ID)

        val event = InventoryClickEvent(
            player.openInventory,
            InventoryType.SlotType.CONTAINER,
            13,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        )
        server.pluginManager.callEvent(event)

        assertTrue(event.isCancelled)
        assertEquals(1, screen.clickCount)
    }

    @Test
    fun `redraw re-renders current open inventory`() {
        adapter.open(player, DemoScreen.ID)
        screen.color = Material.DIAMOND

        adapter.redraw(player)

        val top = player.openInventory.topInventory
        assertEquals(Material.DIAMOND, top.getItem(13)?.type)
        assertEquals(2, screen.renderCount)
    }

    private class DemoScreen : PaperScreen {
        companion object {
            const val ID = "demo"
        }

        override val id: String = ID
        var renderCount = 0
        var clickCount = 0
        var color: Material = Material.EMERALD

        override fun title(viewer: org.bukkit.entity.Player): Component = Component.text("Demo screen")

        override fun render(viewer: org.bukkit.entity.Player, inventory: org.bukkit.inventory.Inventory) {
            renderCount++
            val item = org.bukkit.inventory.ItemStack(color)
            val meta = item.itemMeta
            meta.displayName(Component.text("Odśwież"))
            item.itemMeta = meta
            inventory.setItem(13, item)
            NavigationComponents.place(
                inventory = inventory,
                back = NavigationComponents.back(Component.text("Wróć")),
                redraw = NavigationComponents.redraw(Component.text("Odśwież"))
            )
        }

        override fun handleClick(event: InventoryClickEvent, adapter: PaperGuiAdapter) {
            event.isCancelled = true
            clickCount++
            if (event.rawSlot == NavigationComponents.REDRAW_SLOT) {
                adapter.redraw(event.whoClicked as org.bukkit.entity.Player)
            }
        }
    }
}

