package pl.syntaxdevteam.punisher.identity

import com.google.common.net.InetAddresses
import java.util.UUID

sealed interface ModerationTarget {
    data class PlayerTarget(val uuid: UUID) : ModerationTarget
    data class IpTarget(val address: String) : ModerationTarget
}

/** Literal address parsing only: never DNS, never a nickname-to-address fallback. */
fun canonicalIp(value: String): String? =
    if (InetAddresses.isInetAddress(value)) InetAddresses.toAddrString(InetAddresses.forString(value)) else null
