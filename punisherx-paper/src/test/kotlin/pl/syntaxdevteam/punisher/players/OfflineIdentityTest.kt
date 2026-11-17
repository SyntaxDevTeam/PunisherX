package pl.syntaxdevteam.punisher.players

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.mockito.Mockito.*
import pl.syntaxdevteam.punisher.PunisherX
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OfflineIdentityTest {
    @Test fun `offline server uses its own known UUID instead of premium lookup`() {
        val uuid = UUID.randomUUID()
        val offline = mock(OfflinePlayer::class.java)
        `when`(offline.uniqueId).thenReturn(uuid)
        val plugin = mock(PunisherX::class.java, CALLS_REAL_METHODS)
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<OfflinePlayer> { Bukkit.getOfflinePlayerIfCached("KnownPlayer") }.thenReturn(offline)
            assertEquals(uuid, plugin.resolvePlayerUuid("KnownPlayer"))
            bukkit.verify({ Bukkit.getOfflinePlayer("KnownPlayer") }, never())
        }
    }

    @Test fun `unknown offline name has deterministic case sensitive UUID without lookup`() {
        val server = mock(Server::class.java)
        `when`(server.onlineMode).thenReturn(false)
        val plugin = mock(PunisherX::class.java, CALLS_REAL_METHODS)
        doReturn(server).`when`(plugin).server
        mockStatic(Bukkit::class.java).use { bukkit ->
            for (name in listOf("NewPlayer", "newplayer")) {
                assertEquals(UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(Charsets.UTF_8)),
                    plugin.resolvePlayerUuid(name))
                bukkit.verify({ Bukkit.getOfflinePlayer(name) }, never())
            }
        }
    }
}
