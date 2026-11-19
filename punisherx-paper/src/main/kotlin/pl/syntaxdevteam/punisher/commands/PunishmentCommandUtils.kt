package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.multilineComponent
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import org.bukkit.command.CommandSender
import pl.syntaxdevteam.punisher.permissions.IpVisibility

import pl.syntaxdevteam.punisher.hooks.DiscordSrvNotifications

internal object PunishmentCommandUtils {
    private fun privatePlaceholders(plugin: PunisherX, viewer: CommandSender,
                                    placeholders: Map<String, String>): Map<String, String> =
        placeholders.mapValues { (key, value) ->
            if (key == "player") IpVisibility.displayAddress(plugin, viewer, value) else value
        }

    // punisherx.max.<type>.<time>, e.g. punisherx.max.ban.7d, caps how long the sender may punish for and forbids permanent punishments.
    fun exceedsMaxTime(plugin: PunisherX, stack: CommandSourceStack, type: String, time: String?): Boolean {
        val prefix = "punisherx.max.$type."
        val max = stack.sender.effectivePermissions
            .asSequence()
            .filter { it.value && it.permission.startsWith(prefix) && stack.sender.hasPermission(it.permission) }
            .mapNotNull {
                val raw = it.permission.removePrefix(prefix)
                runCatching { raw to plugin.timeHandler.parseTime(raw) }.getOrNull()
            }
            .maxByOrNull { it.second } ?: return false
        val seconds = time?.takeUnless { it.isBlank() || it.equals("permanent", ignoreCase = true) }
            ?.let { plugin.timeHandler.parseTime(it) }
        if (seconds != null && seconds <= max.second) return false
        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "max_time", mapOf("time" to plugin.timeHandler.formatTime(max.first))))
        return true
    }

    fun parseTimeAndReason(
        plugin: PunisherX,
        args: Array<String>,
        timeIndex: Int,
        forceFlag: String = "--force"
    ): Pair<String?, String> {
        if (args.size <= timeIndex) {
            return null to args.drop(timeIndex).filterNot { it == forceFlag }.joinToString(" ")
        }
        return try {
            val time = args[timeIndex]
            plugin.timeHandler.parseTime(time)
            val reason = args.drop(timeIndex + 1).filterNot { it == forceFlag }.joinToString(" ")
            time to reason
        } catch (_: NumberFormatException) {
            val reason = args.drop(timeIndex).filterNot { it == forceFlag }.joinToString(" ")
            null to reason
        }
    }

    fun buildPlaceholders(
        player: String,
        operator: String,
        reason: String,
        time: String,
        type: String,
        extra: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return mapOf(
            "player" to player,
            "operator" to operator,
            "reason" to reason,
            "time" to time,
            "type" to type
        ) + extra
    }

    fun sendSenderMessages(
        plugin: PunisherX,
        stack: CommandSourceStack,
        section: String,
        key: String,
        placeholders: Map<String, String>,
        silent: Boolean = false
    ) {
        val messageKey = if (silent) "silent_success" else key
        plugin.messageHandler.getSmartMessage(section, messageKey, privatePlaceholders(plugin, stack.sender, placeholders)).forEach { stack.sender.sendMessage(it) }
    }

    fun sendTargetMessages(
        plugin: PunisherX,
        targetPlayer: Player?,
        section: String,
        key: String,
        placeholders: Map<String, String>,
        silent: Boolean = false
    ) {
        if (silent) return
        if (targetPlayer == null) return
        plugin.messageHandler.getSmartMessage(section, key, placeholders).forEach { targetPlayer.sendMessage(it) }
    }

    fun sendKickMessage(
        plugin: PunisherX,
        targetPlayer: Player?,
        section: String,
        key: String,
        placeholders: Map<String, String>
    ) {
        if (targetPlayer == null) return
        val kickMessages = plugin.messageHandler.getSmartMessage(section, key, placeholders)
        targetPlayer.kick(multilineComponent(kickMessages))
    }

    fun sendBroadcast(
        plugin: PunisherX,
        permissionKey: PermissionChecker.PermissionKey,
        section: String,
        key: String,
        placeholders: Map<String, String>,
        silent: Boolean = false,
        notifyDiscord: Boolean = true
    ) {
        if (silent) return
        if (notifyDiscord) DiscordSrvNotifications.publish(plugin, section, placeholders, silent)
        val operator = placeholders["operator"]
        plugin.server.onlinePlayers.filterNot { it.name == operator }.forEach { onlinePlayer ->
            if (PermissionChecker.hasWithSee(onlinePlayer, permissionKey)) {
                val broadcastMessages = plugin.messageHandler.getSmartMessage(section, key, privatePlaceholders(plugin, onlinePlayer, placeholders))
                broadcastMessages.forEach { onlinePlayer.sendMessage(it) }
            }
        }
    }
}
