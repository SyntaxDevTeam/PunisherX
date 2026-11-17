package pl.syntaxdevteam.punisher.identity

import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Names and addresses are never fallback account identifiers. */
data class PlayerIdentity(val realm: String, val uuid: UUID, val canonicalName: String?, val authority: String)
sealed interface IdentityResolution {
    data class Resolved(val identity: PlayerIdentity) : IdentityResolution
    data object NotFound : IdentityResolution
    data object Ambiguous : IdentityResolution
    data object Unavailable : IdentityResolution
}

class IdentityNotResolvedException(val resolution: IdentityResolution) : IllegalArgumentException("Player identity is not confirmed; specify a canonical UUID")

data class IdentitySettings(val realm: String, val authority: String, val onlineDomain: Boolean) {
    companion object {
        fun create(realm: String?, authority: String?, onlineMode: Boolean): IdentitySettings {
            val source = (authority ?: "auto").lowercase(Locale.ROOT)
            require(source in setOf("auto", "online", "offline", "trusted-proxy")) { "Invalid identity.authority" }
            require(source != "online" || onlineMode) { "identity.authority=online requires online-mode" }
            require(source != "offline" || !onlineMode) { "identity.authority=offline requires offline-mode" }
            val online = source == "online" || source == "trusted-proxy" || (source == "auto" && onlineMode)
            require((online && source != "trusted-proxy") || !realm.isNullOrBlank()) {
                "Offline and trusted-proxy identity modes require an explicit identity.realm"
            }
            val domain = realm?.takeIf { it.isNotBlank() } ?: "online"
            require(domain.matches(Regex("[a-z0-9][a-z0-9_.-]{0,63}"))) { "identity.realm must be a lowercase domain identifier of up to 64 characters" }
            val effective = if (source == "auto") if (online) "online" else "offline" else source
            return IdentitySettings(domain, effective, online)
        }
    }
}

/** Only current local sessions resolve names. Historical/shared observations never authorize a lookup. */
class PlayerIdentityDirectory(val settings: IdentitySettings) {
    private val sessions = ConcurrentHashMap<UUID, PlayerIdentity>()

    fun joined(uuid: UUID, name: String) {
        sessions[uuid] = PlayerIdentity(settings.realm, uuid, name, settings.authority)
    }
    fun left(uuid: UUID) { sessions.remove(uuid) }

    fun resolve(identifier: String): IdentityResolution {
        val explicit = runCatching { UUID.fromString(identifier) }.getOrNull()
            ?.takeIf { it.toString().equals(identifier, ignoreCase = true) }
        if (explicit != null) return IdentityResolution.Resolved(PlayerIdentity(settings.realm, explicit, null, "explicit-uuid"))
        if (!identifier.matches(Regex("[A-Za-z0-9_]{3,16}"))) return IdentityResolution.NotFound
        val matches = sessions.values.filter { it.canonicalName.equals(identifier, ignoreCase = true) }
        return when (matches.size) {
            0 -> IdentityResolution.NotFound
            1 -> IdentityResolution.Resolved(matches.single())
            else -> IdentityResolution.Ambiguous
        }
    }
}
