package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

class BanIpCommand(private val plugin: PunisherX) : BasicCommand {
    companion object {
        private val IP_REGEX = Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")
        private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }

    private val clp = plugin.commandLoggerPlugin

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BANIP)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }
        if (args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("banip", "usage"))
            return
        }

        val rawTarget = args[0]
        val isForce = args.contains("--force")
        val playerIPs: List<String> = when {
            IP_REGEX.matches(rawTarget) -> listOf(rawTarget)
            UUID_REGEX.matches(rawTarget) -> plugin.playerIPManager.getPlayerIPsByUUID(rawTarget)
            else -> plugin.playerIPManager.getPlayerIPsByName(rawTarget.lowercase())
        }

        if (playerIPs.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("banip", "not_found"))
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
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "bypass", mapOf("player" to rawTarget)))
            return
        }

        val filtered = args.filterNot { it == "--force" }
        val timeArg = filtered.getOrNull(1)
        val durationSec = timeArg?.let {
            try { plugin.timeHandler.parseTime(it) } catch (_: Exception) { null }
        }
        val reason = if (durationSec != null) filtered.drop(2).joinToString(" ") else filtered.drop(1).joinToString(" ")

        val start = System.currentTimeMillis()
        val end = durationSec?.let { start + it * 1000 }

        var dbError = false
        playerIPs.forEach { ip ->
            val success = plugin.databaseHandler.addPunishment(rawTarget, ip, reason, stack.sender.name,
                "BANIP", start, end ?: -1)
            if (!success) {
                plugin.logger.err("DB error ban-ip $ip")
                dbError = true
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban-ip $ip")
            }
            plugin.databaseHandler.addPunishmentHistory(rawTarget, ip, reason, stack.sender.name,
                "BANIP", start, end ?: -1)
        }
        clp.logCommand(stack.sender.name, "BANIP", rawTarget, reason)
        if (dbError) stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "db_error"))

        if (targetPlayer != null) {
            val lines = plugin.messageHandler.getSmartMessage(
                "banip",
                "kick_message",
                mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(timeArg))
            )
            val builder = Component.text()
            lines.forEach { builder.append(it).append(Component.newline()) }
            targetPlayer.kick(builder.build())
        }

        val msgLines = plugin.messageHandler.getSmartMessage(
            "banip",
            "ban",
            mapOf("player" to rawTarget, "reason" to reason, "time" to plugin.timeHandler.formatTime(timeArg))
        )
        msgLines.forEach { stack.sender.sendMessage(it) }

        plugin.server.onlinePlayers.filter { PermissionChecker.hasWithSee(it, PermissionChecker.PermissionKey.SEE_BANIP) }
            .forEach { player -> msgLines.forEach { player.sendMessage(it) } }

        if (isForce) plugin.logger.warning("Force by ${stack.sender.name} on $rawTarget")
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender as Player, PermissionChecker.PermissionKey.BANIP)) return emptyList()
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getReasons("banip", "reasons")
            else -> emptyList()
        }
    }

    private fun generateTimeSuggestions(): List<String> {
        val units = listOf("s", "m", "h", "d")
        val suggestions = mutableListOf<String>()
        for (i in 1..999) {
            for (unit in units) {
                suggestions.add("$i$unit")
            }
        }
        return suggestions
    }
}
