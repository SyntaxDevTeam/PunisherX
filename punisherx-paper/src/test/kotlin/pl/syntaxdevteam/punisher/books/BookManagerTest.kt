package pl.syntaxdevteam.punisher.books

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookManagerTest {
    private fun player(): Player = mock<Player>().also { whenever(it.uniqueId).thenReturn(UUID.randomUUID()) }
    private fun button(manager: BookManager, player: Player, action: () -> Unit): String {
        var command = ""
        manager.open(player, Component.text("Reports"), Component.text("PunisherX")) { menu ->
            val button = menu.button(Component.text("Send"), action)
            command = button.clickEvent()!!.value()
            listOf(button)
        }
        return command
    }

    @Test fun `actions are bound to player and can only execute once`() {
        val manager = BookManager()
        val owner = player()
        val other = player()
        var calls = 0
        val command = button(manager, owner) { calls++ }
        val foreignEvent = PlayerCommandPreprocessEvent(other, command, emptySet())
        manager.onCommand(foreignEvent)
        assertTrue(foreignEvent.isCancelled)
        assertEquals(0, calls)
        manager.onCommand(PlayerCommandPreprocessEvent(owner, command, emptySet()))
        manager.onCommand(PlayerCommandPreprocessEvent(owner, command, emptySet()))
        assertEquals(1, calls)
    }

    @Test fun `new menu invalidates old buttons without losing the new session`() {
        val manager = BookManager()
        val player = player()
        var calls = 0
        val old = button(manager, player) { calls += 100 }
        val current = button(manager, player) { calls++ }
        manager.onCommand(PlayerCommandPreprocessEvent(player, old, emptySet()))
        manager.onCommand(PlayerCommandPreprocessEvent(player, current, emptySet()))
        assertEquals(1, calls)
    }

    @Test fun `expired and disconnected sessions cannot execute`() {
        var now = 0L
        val manager = BookManager { now }
        val player = player()
        var calls = 0
        val expired = button(manager, player) { calls++ }
        now = 300_001
        manager.onCommand(PlayerCommandPreprocessEvent(player, expired, emptySet()))
        val disconnected = button(manager, player) { calls++ }
        manager.onQuit(PlayerQuitEvent(player, Component.empty()))
        manager.onCommand(PlayerCommandPreprocessEvent(player, disconnected, emptySet()))
        assertEquals(0, calls)
    }
}
