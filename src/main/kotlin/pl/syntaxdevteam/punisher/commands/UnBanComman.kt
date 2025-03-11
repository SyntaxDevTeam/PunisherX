package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class UnBanCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (!stack.sender.hasPermission("punisherx.unban")) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("unban", "usage"))
            return
        }

        val playerOrIpOrUUID = args[0]

        // Jeśli podano adres IP
        if (playerOrIpOrUUID.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
            unbanIP(stack, playerOrIpOrUUID)
            return
        }

        // Pobranie UUID gracza
        val uuid = plugin.uuidManager.getUUID(playerOrIpOrUUID).toString()

        plugin.logger.debug("UUID for player $playerOrIpOrUUID: [$uuid]")

        // Próba odbanowania użytkownika
        if (unbanPlayer(stack, playerOrIpOrUUID, uuid)) {
            return
        }

        // Próba odbanowania po IP (jeśli istnieje)
        val ip = plugin.playerIPManager.getPlayerIPByName(playerOrIpOrUUID)
        if (ip == null) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
            return
        }

        plugin.logger.debug("Assigned IP for player $playerOrIpOrUUID: [$ip]")

        if (!unbanIP(stack, ip)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
        }
    }

    /**
     * Odbanowanie użytkownika po UUID
     */
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
            stack.sender.sendMessage(plugin.messageHandler.getMessage("unban", "unban", mapOf("player" to playerName)))
            broadcastUnban(playerName)
        }

        return unbanned
    }

    /**
     * Odbanowanie adresu IP
     */
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
            stack.sender.sendMessage(plugin.messageHandler.getMessage("unban", "unban", mapOf("player" to ip)))
            broadcastUnban(ip)
        }

        return unbanned
    }

    /**
     * Wysyła wiadomość o odbanowaniu do wszystkich uprawnionych graczy
     */
    private fun broadcastUnban(playerOrIp: String) {
        val message = plugin.messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIp))
        val permission = "punisherx.see.unban"
        plugin.server.onlinePlayers
            .filter { it.hasPermission(permission) }
            .forEach { it.sendMessage(message) }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return if (stack.sender.hasPermission("punisherx.unban") && args.size == 1) {
            plugin.server.onlinePlayers.map { it.name }
        } else {
            emptyList()
        }
    }
}
