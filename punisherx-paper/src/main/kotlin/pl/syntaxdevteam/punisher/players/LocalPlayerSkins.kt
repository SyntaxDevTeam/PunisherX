package pl.syntaxdevteam.punisher.players

import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerTextures
import java.net.URL
import java.util.UUID

/** Only textures supplied by the server at login are trusted; this class never resolves profiles. */
class LocalPlayerSkins {
    private data class Skin(val url: URL, val model: PlayerTextures.SkinModel)
    private val skins = Caffeine.newBuilder().maximumSize(10_000).build<UUID, Skin>()

    fun capture(player: Player) {
        val textures = player.playerProfile.textures
        val url = textures.skin
        if (url == null) skins.invalidate(player.uniqueId)
        else skins.put(player.uniqueId, Skin(url, textures.skinModel))
    }

    fun apply(uuid: UUID, meta: SkullMeta) {
        val skin = skins.getIfPresent(uuid) ?: return
        val profile = Bukkit.createPlayerProfile(uuid)
        val textures = profile.textures
        textures.setSkin(skin.url, skin.model)
        profile.setTextures(textures)
        meta.ownerProfile = profile
    }
}
