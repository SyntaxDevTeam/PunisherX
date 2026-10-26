package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.multilineComponent
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

class BanIpCommand(private val plugin: PunisherX) : BasicCommand {
    companion object {
        private val IP_REGEX = Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")
        private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }

    private val clp = plugin.commandLoggerPlugin

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val options = PunishmentCommandOptions.parse(args)
        val arguments = options.arguments
        val silent = options.silent
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (arguments.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("banip", "usage"))
            return
        }

        val rawTarget = arguments[0]
        val isForce = options.force
        val playerIPs: List<String> = when {
            IP_REGEX.matches(rawTarget) -> listOf(rawTarget)
            UUID_REGEX.matches(rawTarget) -> plugin.playerIPManager.getPlayerIPsByUUID(rawTarget)
            else -> plugin.playerIPManager.getPlayerIPsByName(rawTarget.lowercase())
        }

        if (playerIPs.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("banip", "not_found"))
            return
        }

        val targetUUID: UUID? = when {
            IP_REGEX.matches(rawTarget) -> plugin.playerIPManager.getAllDecryptedRecords()
                .find { it.playerIP == rawTarget }
                ?.let { UUID.fromString(it.playerUUID) }
            else -> plugin.resolvePlayerUuid(rawTarget)
        }
        val targetPlayer: Player? = targetUUID?.let { Bukkit.getPlayer(it) }

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithLegacy(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to rawTarget)))
            return
        }

        val filtered = arguments.toList()
        val timeArg = filtered.getOrNull(1)
        val durationSec = timeArg?.let {
            try { plugin.timeHandler.parseTime(it) } catch (_: Exception) { null }
        }
        val reason = if (durationSec != null) filtered.drop(2).joinToString(" ") else filtered.drop(1).joinToString(" ")

        val start = System.currentTimeMillis()
        val end = durationSec?.let { start + it * 1000 }
        val formattedTime = plugin.timeHandler.formatTime(timeArg)
        val failedIPs = mutableListOf<String>()
        val bannedIPs = mutableListOf<String>()
        val normalizedEnd = end ?: -1
        val punishmentIds = mutableListOf<Long>()
        playerIPs.distinct().forEach { ip ->
            val punishmentId = plugin.databaseHandler.addPunishment(rawTarget, ip, reason, stack.sender.name,
                "BANIP", start, normalizedEnd)
            if (punishmentId == null) {
                plugin.logger.err("IP ban not applied: failed to persist punishment for $ip")
                failedIPs.add(ip)
                return@forEach
            }
            punishmentIds.add(punishmentId)
            bannedIPs.add(ip)
            plugin.databaseHandler.addPunishmentHistory(rawTarget, ip, reason, stack.sender.name,
                "BANIP", start, normalizedEnd)
            plugin.proxyBridgeMessenger.notifyIpBan(ip, reason, normalizedEnd)
        }
        if (failedIPs.isNotEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent(
                "error", "banip_db_error", mapOf(
                    "failed_ips" to failedIPs.joinToString(", "),
                    "banned_ips" to bannedIPs.joinToString(", ").ifEmpty { "-" }
                )
            ))
            // Some addresses may already have been persisted. Do not report full success.
            return
        }
        val placeholders = mapOf(
            "player" to rawTarget,
            "operator" to stack.sender.name,
            "reason" to reason,
            "time" to formattedTime,
            "type" to "BANIP",
            "id" to when {
                punishmentIds.isEmpty() -> "?"
                punishmentIds.size == 1 -> punishmentIds.first().toString()
                else -> punishmentIds.joinToString(",")
            }
        )
        clp.logCommand(stack.sender.name, "BANIP", rawTarget, reason)

        if (targetPlayer != null) {
            val lines = plugin.messageHandler.getSmartMessage(
                "banip",
                "kick_message",
                placeholders
            )
            targetPlayer.kick(multilineComponent(lines))
        }

        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "banip", "ban", placeholders, silent = silent)
        plugin.actionExecutor.executeAction("ip_banned", rawTarget, placeholders, silent = silent)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_BANIP, "banip", "ban", placeholders, silent = silent)

        if (isForce) plugin.logger.warning("Force by ${stack.sender.name} on $rawTarget")
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender as Player, PermissionChecker.PermissionKey.BANIP)) return emptyList()
        val input = args.lastOrNull().orEmpty()
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(input, ignoreCase = true) }
            2 -> TimeSuggestionProvider.generateTimeSuggestions().filter { it.startsWith(input, ignoreCase = true) }
            3 -> plugin.messageHandler.getMessageStringList("banip", "reasons").filter { it.startsWith(input, ignoreCase = true) }
            else -> emptyList()
        }
    }
}
