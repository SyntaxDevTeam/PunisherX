package pl.syntaxdevteam.punisher.players

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerProfile
import org.bukkit.profile.PlayerTextures
import org.mockito.Mockito.*
import java.net.URI
import java.util.UUID
import kotlin.test.Test

class LocalPlayerSkinsTest {
    @Test fun `unknown skin never creates a profile`() {
        val meta = mock(SkullMeta::class.java)
        mockStatic(Bukkit::class.java).use { bukkit ->
            LocalPlayerSkins().apply(UUID.randomUUID(), meta)
            bukkit.verifyNoInteractions()
            verifyNoInteractions(meta)
        }
    }

    @Test fun `locally supplied texture is copied without resolving a profile`() {
        val uuid = UUID.randomUUID()
        val url = URI("https://textures.minecraft.net/texture/test").toURL()
        val player = mock(Player::class.java)
        val loginProfile = mock(com.destroystokyo.paper.profile.PlayerProfile::class.java)
        val loginTextures = mock(PlayerTextures::class.java)
        `when`(player.uniqueId).thenReturn(uuid)
        `when`(player.playerProfile).thenReturn(loginProfile)
        `when`(loginProfile.textures).thenReturn(loginTextures)
        `when`(loginTextures.skin).thenReturn(url)
        `when`(loginTextures.skinModel).thenReturn(PlayerTextures.SkinModel.SLIM)
        val cache = LocalPlayerSkins()
        cache.capture(player)
        val profile = mock(PlayerProfile::class.java)
        val textures = mock(PlayerTextures::class.java)
        `when`(profile.textures).thenReturn(textures)
        val meta = mock(SkullMeta::class.java)
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<PlayerProfile> { Bukkit.createPlayerProfile(uuid) }.thenReturn(profile)
            cache.apply(uuid, meta)
            verify(textures).setSkin(url, PlayerTextures.SkinModel.SLIM)
            verify(profile).setTextures(textures)
            verify(meta).setOwnerProfile(profile)
            verify(profile, never()).update()
        }
        // A later login without textures must remove the previous skin.
        `when`(loginTextures.skin).thenReturn(null)
        cache.capture(player)
        val emptyMeta = mock(SkullMeta::class.java)
        cache.apply(uuid, emptyMeta)
        verifyNoInteractions(emptyMeta)
    }
    @Test fun `premium skins are delegated to SyntaxCore and applied with server UUID`() {
        val uuid = UUID.randomUUID()
        val manager = mock(pl.syntaxdevteam.core.tools.UUIDManager::class.java)
        val resolver = mock(pl.syntaxdevteam.core.tools.PlayerSkinResolver::class.java)
        val directory = java.io.File("unused-test-cache")
        val skin = pl.syntaxdevteam.core.tools.PlayerSkin("https://textures.minecraft.net/texture/premium", true)
        `when`(manager.createSkinResolver(directory)).thenReturn(resolver)
        `when`(resolver.resolve(uuid, "OfflineName")).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(skin))
        val cache = LocalPlayerSkins()
        cache.initialize(directory, manager)
        kotlin.test.assertTrue(cache.resolve(uuid, "OfflineName").join())
        val profile = mock(PlayerProfile::class.java)
        val textures = mock(PlayerTextures::class.java)
        val meta = mock(SkullMeta::class.java)
        `when`(profile.textures).thenReturn(textures)
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<PlayerProfile> { Bukkit.createPlayerProfile(uuid) }.thenReturn(profile)
            cache.apply(uuid, meta)
            verify(textures).setSkin(java.net.URI(skin.url).toURL(), PlayerTextures.SkinModel.SLIM)
            verify(meta).setOwnerProfile(profile)
            verify(profile, never()).update()
        }
        cache.close()
        verify(resolver).close()
    }

}
