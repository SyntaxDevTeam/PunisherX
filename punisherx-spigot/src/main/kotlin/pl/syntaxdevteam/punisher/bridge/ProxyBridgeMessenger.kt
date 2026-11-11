package pl.syntaxdevteam.punisher.bridge

import pl.syntaxdevteam.punisher.PunisherX
import java.util.UUID

/**
 * Compatibility adapter for old call sites. Enforcement is SQL-authoritative:
 * proxy bridges independently reconcile committed active punishments.
 * Do not expose punishment commands over client-accessible plugin channels
 * or enqueue global, replayable events.
 */
@Suppress("UNUSED_PARAMETER")
class ProxyBridgeMessenger(private val plugin: PunisherX) {
    companion object {
        const val CHANNEL = "punisherx:bridge"
    }

    fun registerChannel() = Unit

    fun unregisterChannel() = Unit

    fun notifyBan(target: UUID, reason: String, end: Long) = Unit

    fun notifyIpBan(ip: String, reason: String, end: Long) = Unit
}
