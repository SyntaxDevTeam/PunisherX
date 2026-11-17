package pl.syntaxdevteam.punisher.players

import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.Mockito.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.identity.IdentityNotResolvedException
import java.util.UUID
import kotlin.test.*

class OfflineIdentityTest {
    @Test fun `unconfirmed offline targets fail without cache profile or punishment-history lookup`() {
        val server = mock(Server::class.java)
        `when`(server.onlineMode).thenReturn(false)
        val plugin = mock(PunisherX::class.java, CALLS_REAL_METHODS)
        doReturn(server).`when`(plugin).server
        doReturn(YamlConfiguration().apply { set("identity.realm", "offline-test") }).`when`(plugin).config
        plugin.resetPlayerIdentities()
        mockStatic(Bukkit::class.java).use { bukkit ->
            for (name in listOf("KnownPlayer", "NewPlayer", "newplayer")) {
                assertFailsWith<IdentityNotResolvedException> { plugin.resolvePlayerUuid(name) }
            }
            val uuid = UUID.randomUUID()
            assertEquals(uuid, plugin.resolvePlayerUuid(uuid.toString()))
            plugin.playerIdentities.joined(uuid, "KnownPlayer")
            assertEquals(uuid, plugin.resolvePlayerUuid("KnownPlayer"))
            plugin.playerIdentities.left(uuid)
            assertFailsWith<IdentityNotResolvedException> { plugin.resolvePlayerUuid("KnownPlayer") }
            bukkit.verifyNoInteractions()
        }
    }
}
