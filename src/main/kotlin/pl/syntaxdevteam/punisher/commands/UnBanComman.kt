package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.minimessage.MiniMessage
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.Logger
import pl.syntaxdevteam.punisher.common.MessageHandler
import pl.syntaxdevteam.punisher.common.UUIDManager

@Suppress("UnstableApiUsage")
class UnBanCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {
    private var config = plugin.config
    private var debugMode = config.getBoolean("debug")
    private val logger = Logger(pluginMetas, debugMode)
    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.unban")) {
                val playerOrIpOrUUID = args[0]
                val broadcastMessage = MiniMessage.miniMessage().deserialize(messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                if (playerOrIpOrUUID.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                    val punishments = plugin.databaseHandler.getPunishmentsByIP(playerOrIpOrUUID)
                    if (punishments.isNotEmpty()) {
                        punishments.forEach { punishment ->
                            if (punishment.type == "BANIP") {
                                plugin.databaseHandler.removePunishment(playerOrIpOrUUID, punishment.type)
                            }
                        }
                        stack.sender.sendRichMessage(messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                        logger.info("IP $playerOrIpOrUUID has been unbanned")
                    } else {
                        stack.sender.sendRichMessage(messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
                    }
                } else {
                    val uuid = uuidManager.getUUID(playerOrIpOrUUID).toString()
                    logger.debug("UUID for player $playerOrIpOrUUID: [$uuid]")
                    val punishments = plugin.databaseHandler.getPunishments(uuid)
                    if (punishments.isNotEmpty()) {
                        punishments.forEach { punishment ->
                            if (punishment.type == "BAN") {
                                plugin.databaseHandler.removePunishment(uuid, punishment.type)
                                //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon $playerOrIpOrUUID")
                                logger.info("Player $playerOrIpOrUUID ($uuid) has been unbanned")
                            }
                        }
                        stack.sender.sendRichMessage(messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                    } else {
                        val ip = plugin.playerIPManager.getPlayerIPByName(playerOrIpOrUUID)
                        logger.debug("Assigned IP for player $playerOrIpOrUUID: [$ip]")
                        if (ip != null) {
                            val punishmentsByIP = plugin.databaseHandler.getPunishmentsByIP(ip)
                            if (punishmentsByIP.isNotEmpty()) {
                                punishmentsByIP.forEach { punishment ->
                                    if (punishment.type == "BANIP") {
                                        plugin.databaseHandler.removePunishment(ip, punishment.type)
                                        //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon-ip $ip")
                                        logger.info("Player $playerOrIpOrUUID ($uuid) has been unbanned")
                                    }
                                }
                                stack.sender.sendRichMessage(messageHandler.getMessage("unban", "unban", mapOf("player" to playerOrIpOrUUID)))
                            } else {
                                stack.sender.sendRichMessage(messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
                            }
                        } else {
                            stack.sender.sendRichMessage(messageHandler.getMessage("error", "player_not_found", mapOf("player" to playerOrIpOrUUID)))
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
                stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("ban", "usage_unban"))
        }
    }
}
