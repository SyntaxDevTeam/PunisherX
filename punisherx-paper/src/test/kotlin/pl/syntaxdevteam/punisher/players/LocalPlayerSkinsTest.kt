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
}
