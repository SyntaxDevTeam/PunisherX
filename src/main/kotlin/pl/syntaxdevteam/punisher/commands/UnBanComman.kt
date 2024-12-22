package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class UnBanCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.unban")) {
                val playerOrIpOrUUID = args[0]
                val broadcastMessage = MiniMessage.miniMessage().deserialize(plugin.messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                if (playerOrIpOrUUID.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                    val punishments = plugin.databaseHandler.getPunishmentsByIP(playerOrIpOrUUID)
                    if (punishments.isNotEmpty()) {
                        punishments.forEach { punishment ->
                            if (punishment.type == "BANIP") {
                                plugin.databaseHandler.removePunishment(playerOrIpOrUUID, punishment.type)
                            }
                        }
                        stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                        plugin.logger.info("IP $playerOrIpOrUUID has been unbanned")
                    } else {
                        stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
                    }
                } else {
                    val uuid = plugin.uuidManager.getUUID(playerOrIpOrUUID).toString()
                    plugin.logger.debug("UUID for player $playerOrIpOrUUID: [$uuid]")
                    val punishments = plugin.databaseHandler.getPunishments(uuid)
                    if (punishments.isNotEmpty()) {
                        punishments.forEach { punishment ->
                            if (punishment.type == "BAN") {
                                plugin.databaseHandler.removePunishment(uuid, punishment.type)
                                //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon $playerOrIpOrUUID")
                                plugin.logger.info("Player $playerOrIpOrUUID ($uuid) has been unbanned")
                            }
                        }
                        stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                    } else {
                        val ip = plugin.playerIPManager.getPlayerIPByName(playerOrIpOrUUID)
                        plugin.logger.debug("Assigned IP for player $playerOrIpOrUUID: [$ip]")
                        if (ip != null) {
                            val punishmentsByIP = plugin.databaseHandler.getPunishmentsByIP(ip)
                            if (punishmentsByIP.isNotEmpty()) {
                                punishmentsByIP.forEach { punishment ->
                                    if (punishment.type == "BANIP") {
                                        plugin.databaseHandler.removePunishment(ip, punishment.type)
                                        //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon-ip $ip")
                                        plugin.logger.info("Player $playerOrIpOrUUID ($uuid) has been unbanned")
                                    }
                                }
                                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                            } else {
                                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
                            }
                        } else {
                            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
                        }
                    }
                    val permission = "punisherx.see.unban"
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (onlinePlayer.hasPermission(permission)) {
                            onlinePlayer.sendMessage(broadcastMessage)
                        }
                    }
                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("ban", "usage_unban"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
