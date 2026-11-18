package pl.syntaxdevteam.punisher.players

import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerTextures
import java.net.URL
import java.net.URI
import java.util.UUID
import pl.syntaxdevteam.core.SyntaxCore
import pl.syntaxdevteam.core.tools.PlayerSkin
import pl.syntaxdevteam.core.tools.PlayerSkinResolver
import java.io.File
import java.time.Duration
import java.util.concurrent.CompletableFuture
import pl.syntaxdevteam.core.tools.UUIDManager

/** Applies complete local or cached premium textures without invoking Bukkit profile resolution. */
class LocalPlayerSkins : AutoCloseable {
    private var resolver: PlayerSkinResolver? = null
    fun initialize(directory: File, manager: UUIDManager = SyntaxCore.uuidManager) {
        resolver?.close()
        resolver = manager.createSkinResolver(directory)
    }
    fun resolve(uuid: UUID, name: String): CompletableFuture<Boolean> {
        if (skins.getIfPresent(uuid) != null) return CompletableFuture.completedFuture(true)
        return (resolver?.resolve(uuid, name) ?: CompletableFuture.completedFuture(null)).thenApply { skin ->
            if (skin != null) skins.asMap().putIfAbsent(uuid, Skin(URI.create(skin.url).toURL(),
                if (skin.slim) PlayerTextures.SkinModel.SLIM else PlayerTextures.SkinModel.CLASSIC))
            skin != null
        }
    }
    override fun close() { resolver?.close(); resolver = null }

    private data class Skin(val url: URL, val model: PlayerTextures.SkinModel)
    private val skins = Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofDays(7)).build<UUID, Skin>()

    fun capture(player: Player) {
        val textures = player.playerProfile.textures
        val url = textures.skin
        resolver?.capture(player.uniqueId, url?.let { PlayerSkin(it.toString(), textures.skinModel == PlayerTextures.SkinModel.SLIM) })
        if (url == null) skins.invalidate(player.uniqueId)
        else skins.put(player.uniqueId, Skin(url, textures.skinModel))
    }

    fun apply(uuid: UUID, meta: SkullMeta) {
        val skin = skins.getIfPresent(uuid) ?: return
        val profile = Bukkit.createProfile(uuid)
        val textures = profile.textures
        textures.setSkin(skin.url, skin.model)
        profile.setTextures(textures)
        meta.playerProfile = profile
    }
}
