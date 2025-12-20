package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

internal object PunishmentCommandUtils {
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
        placeholders: Map<String, String>
    ) {
        plugin.messageHandler.getSmartMessage(section, key, placeholders).forEach { stack.sender.sendMessage(it) }
    }

    fun sendTargetMessages(
        plugin: PunisherX,
        targetPlayer: Player?,
        section: String,
        key: String,
        placeholders: Map<String, String>
    ) {
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
        val kickMessageBuilder = Component.text()
        kickMessages.forEach { line ->
            kickMessageBuilder.append(line).append(Component.newline())
        }
        targetPlayer.kick(kickMessageBuilder.build())
    }

    fun sendBroadcast(
        plugin: PunisherX,
        permissionKey: PermissionChecker.PermissionKey,
        section: String,
        key: String,
        placeholders: Map<String, String>
    ) {
        val broadcastMessages = plugin.messageHandler.getSmartMessage(section, key, placeholders)
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (PermissionChecker.hasWithSee(onlinePlayer, permissionKey)) {
                broadcastMessages.forEach { onlinePlayer.sendMessage(it) }
            }
        }
    }
}
