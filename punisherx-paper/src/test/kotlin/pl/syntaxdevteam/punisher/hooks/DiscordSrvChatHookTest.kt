package pl.syntaxdevteam.punisher.hooks

import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent
import org.bukkit.entity.Player
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.never
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordSrvChatHookTest {
    private val uuid = UUID.randomUUID()
    private val player = mock<Player>().also { whenever(it.uniqueId).thenReturn(uuid) }
    private val event = mock<GameChatMessagePreProcessEvent>().also {
        whenever(it.player).thenReturn(player)
    }

    @Test
    fun `restricted player cannot relay chat`() {
        DiscordSrvChatHook { id -> id == uuid }.onGameChat(event)
        verify(event).setCancelled(true)
    }

    @Test
    fun `unrestricted player can relay chat`() {
        DiscordSrvChatHook { false }.onGameChat(event)
        verify(event, never()).setCancelled(true)
    }

    @Test
    fun `previous cancellation remains intact`() {
        whenever(event.isCancelled).thenReturn(true)
        var lookups = 0
        DiscordSrvChatHook { lookups++; false }.onGameChat(event)
        assertEquals(0, lookups)
        verify(event, never()).setCancelled(false)
    }
}
