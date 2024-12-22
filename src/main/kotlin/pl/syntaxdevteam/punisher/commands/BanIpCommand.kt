package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class BanIpCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.banip")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("banip", "usage"))
                } else {
                    val playerOrIpOrUUID = args[0]
                    val playerIP = when {
                        playerOrIpOrUUID.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) -> playerOrIpOrUUID
                        playerOrIpOrUUID.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) -> plugin.playerIPManager.getPlayerIPByUUID(playerOrIpOrUUID)
                        else -> plugin.playerIPManager.getPlayerIPByName(playerOrIpOrUUID.lowercase())
                    }

                    if (playerIP == null) {
                        stack.sender.sendRichMessage(plugin.messageHandler.getMessage("banip", "not_found"))
                        return
                    }
                    val targetPlayer = Bukkit.getPlayer(playerOrIpOrUUID)
                    val isForce = args.contains("--force")
                    if (targetPlayer != null) {
                        if (!isForce && targetPlayer.hasPermission("punisherx.bypass.banip")) {
                            stack.sender.sendRichMessage(
                                plugin.messageHandler.getMessage(
                                    "error",
                                    "bypass",
                                    mapOf("player" to playerOrIpOrUUID)
                                )
                            )
                            return
                        }
                    }

                    var gtime: String?
                    var reason: String
                    try {
                        gtime = args[1]
                        plugin.timeHandler.parseTime(gtime)
                        reason = args.slice(2 until args.size).filterNot { it == "--force" }.joinToString(" ")
                    } catch (e: NumberFormatException) {
                        gtime = null
                        reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
                    }

                    val punishmentType = "BANIP"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    val success = plugin.databaseHandler.addPunishment(playerOrIpOrUUID, playerIP, reason, stack.sender.name, punishmentType, start, end ?: -1)
                    if (!success) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban-ip $playerOrIpOrUUID")
                    }
                    plugin.databaseHandler.addPunishmentHistory(playerOrIpOrUUID, playerIP, reason, stack.sender.name, punishmentType, start, end ?: -1)

                    if (targetPlayer != null) {
                        val kickMessages = plugin.messageHandler.getComplexMessage("banip", "kick_message", mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime)))
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        targetPlayer.kick(kickMessage.build())
                    }

                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("banip", "ban", mapOf("player" to playerOrIpOrUUID, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))))
                    val permission = "punisherx.see.banip"
                    val broadcastMessage = MiniMessage.miniMessage().deserialize(plugin.messageHandler.getMessage("banip", "ban", mapOf("player" to playerOrIpOrUUID, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))))
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (onlinePlayer.hasPermission(permission)) {
                            onlinePlayer.sendMessage(broadcastMessage)
                        }
                    }
                    if (isForce) {
                        plugin.logger.warning("Force-banned by ${stack.sender.name} on $playerOrIpOrUUID")
                    }
                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("banip", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
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
