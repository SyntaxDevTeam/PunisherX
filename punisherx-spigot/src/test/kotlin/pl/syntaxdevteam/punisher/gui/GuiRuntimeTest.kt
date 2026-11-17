package pl.syntaxdevteam.punisher.gui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.platform.SchedulerAdapter
import kotlin.test.Test
import kotlin.test.assertEquals

class GuiRuntimeTest {
    @Test fun `reload invalidates sessions and closes only previously owned panels on player thread`() {
        val plugin = mock<PunisherX>()
        val player = mock<Player>()
        val other = mock<Player>()
        val scheduler = mock<SchedulerAdapter>()
        val owned = mock<Inventory>()
        val foreign = mock<Inventory>()
        val view = mock<InventoryView>()
        val otherView = mock<InventoryView>()
        whenever(plugin.server).thenReturn(mock())
        whenever(plugin.server.onlinePlayers).thenReturn(listOf(player, other))
        whenever(plugin.schedulerAdapter).thenReturn(scheduler)
        whenever(player.openInventory).thenReturn(view)
        whenever(other.openInventory).thenReturn(otherView)
        whenever(view.topInventory).thenReturn(owned)
        whenever(otherView.topInventory).thenReturn(foreign)
        val tasks = mutableListOf<Runnable>()
        doAnswer { tasks += it.getArgument<Runnable>(1); null }.whenever(scheduler).runForPlayer(any(), any())
        val runtime = GuiRuntime(plugin)
        runtime.track(owned)
        runtime.reset()
        assertEquals(1L, runtime.generation)
        verify(player, never()).closeInventory()
        tasks.forEach { it.run() }
        verify(player).closeInventory()
        verify(other, never()).closeInventory()
        tasks.clear()
        runtime.reset()
        assertEquals(2L, runtime.generation)
        assertEquals(0, tasks.size, "Previous sessions must be cleared after reload")
    }
}
