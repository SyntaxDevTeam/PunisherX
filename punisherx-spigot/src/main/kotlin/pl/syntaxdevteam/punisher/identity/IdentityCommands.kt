package pl.syntaxdevteam.punisher.identity

import pl.syntaxdevteam.punisher.compatibility.*
import org.bukkit.command.CommandSender
import pl.syntaxdevteam.punisher.PunisherX
import java.util.UUID

/** Preserve the UUID API for integrations, but make failure explicit before command side effects. */
fun PunisherX.resolvePlayerUuidOrNotify(identifier: String, sender: CommandSender): UUID? = try {
    resolvePlayerUuid(identifier).also { auditIdentityChoice(sender, it, identifier) }
} catch (error: IdentityNotResolvedException) {
    val key = when (error.resolution) {
        IdentityResolution.Ambiguous -> "identity_ambiguous"
        IdentityResolution.Unavailable -> "identity_unavailable"
        else -> "identity_unconfirmed"
    }
    sender.sendMessage(messageHandler.stringMessageToComponent("error", key))
    null
}

fun PunisherX.findConfirmedPlayerUuid(identifier: String): UUID? = try {
    resolvePlayerUuid(identifier)
} catch (_: IdentityNotResolvedException) { null }

fun PunisherX.resolveModerationTarget(identifier: String, sender: CommandSender): ModerationTarget? {
    canonicalIp(identifier)?.let { return ModerationTarget.IpTarget(it) }
    return resolvePlayerUuidOrNotify(identifier, sender)?.let { ModerationTarget.PlayerTarget(it) }
}
