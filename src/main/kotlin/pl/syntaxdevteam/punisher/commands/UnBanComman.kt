package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnBanCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNBAN)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("unban", "usage"))
            return
        }

        val playerOrIpOrUUID = args[0]

        if (playerOrIpOrUUID.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
            unbanIP(stack, playerOrIpOrUUID)
            return
        }

        val uuid = plugin.resolvePlayerUuid(playerOrIpOrUUID).toString()

        plugin.logger.debug("UUID for player $playerOrIpOrUUID: [$uuid]")

        if (unbanPlayer(stack, playerOrIpOrUUID, uuid)) {
            return
        }

        val ips = plugin.playerIPManager.getPlayerIPsByName(playerOrIpOrUUID)
        if (ips.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
            return
        }

        plugin.logger.debug("Assigned IPs for player $playerOrIpOrUUID: $ips")

        var anyUnbanned = false
        ips.forEach { ip ->
            if (unbanIP(stack, ip)) anyUnbanned = true
        }
        if (!anyUnbanned) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
        }
    }

    private fun unbanPlayer(stack: CommandSourceStack, playerName: String, uuid: String): Boolean {
        val punishments = plugin.databaseHandler.getPunishments(uuid)

        if (punishments.isEmpty()) {
            plugin.logger.debug("Player $playerName ($uuid) has no ban")
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_punished", mapOf("player" to playerName)))
            return false
        }

        var unbanned = false
        punishments.forEach { punishment ->
            if (punishment.type == "BAN") {
                plugin.commandLoggerPlugin.logCommand(stack.sender.name, "UNBAN", playerName, "")
                plugin.databaseHandler.removePunishment(uuid, punishment.type, removeAll = false)
                plugin.logger.info("Player $playerName ($uuid) has been unbanned")
                unbanned = true
            }
        }

        if (unbanned) {
            plugin.messageHandler.getSmartMessage(
                "unban",
                "unban",
                mapOf("player" to playerName)
            ).forEach { stack.sender.sendMessage(it) }
            broadcastUnban(playerName)
        }

        return unbanned
    }

    private fun unbanIP(stack: CommandSourceStack, ip: String): Boolean {
        val punishments = plugin.databaseHandler.getPunishmentsByIP(ip)

        if (punishments.isEmpty()) {
            plugin.logger.debug("No punishments found for IP $ip")
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "ip_not_found", mapOf("ip" to ip)))
            return false
        }

        var unbanned = false
        punishments.forEach { punishment ->
            if (punishment.type == "BANIP") {
                plugin.commandLoggerPlugin.logCommand(stack.sender.name, "UNBAN (IP)", ip, "")
                plugin.databaseHandler.removePunishment(ip, punishment.type)
                plugin.logger.info("IP $ip has been unbanned")
                unbanned = true
            }
        }

        if (unbanned) {
            plugin.messageHandler.getSmartMessage(
                "unban",
                "unban",
                mapOf("player" to ip)
            ).forEach { stack.sender.sendMessage(it) }
            broadcastUnban(ip)
        }

        return unbanned
    }

    private fun broadcastUnban(playerOrIp: String) {
        val messages = plugin.messageHandler.getSmartMessage("unban", "unban", mapOf("player" to playerOrIp))

        plugin.server.onlinePlayers
            .filter { PermissionChecker.hasWithSee(it, PermissionChecker.PermissionKey.SEE_UNBAN) }
            .forEach { player -> messages.forEach { player.sendMessage(it) } }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNBAN) && args.size == 1) {
            plugin.server.onlinePlayers.map { it.name }
        } else {
            emptyList()
        }
    }
}
