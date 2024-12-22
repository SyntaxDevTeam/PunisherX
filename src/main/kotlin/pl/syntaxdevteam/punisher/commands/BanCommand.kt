package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.ban.BanListType
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.ban.ProfileBanList
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import java.util.*

@Suppress("UnstableApiUsage")
class BanCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.ban")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("ban", "usage"))
                } else {
                    val player = args[0]
                    val targetPlayer = Bukkit.getPlayer(player)
                    val isForce = args.contains("--force")
                    if (targetPlayer != null) {
                        if (!isForce && targetPlayer.hasPermission("punisherx.bypass.ban")) {
                            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "bypass", mapOf("player" to player)))
                            return
                        }
                    }
                    val uuid = plugin.uuidManager.getUUID(player).toString()
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

                    val punishmentType = "BAN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    val success = plugin.databaseHandler.addPunishment(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)
                    if (!success) {
                        val playerProfile = Bukkit.createProfile(UUID.fromString(uuid), player)
                        val banList: ProfileBanList = Bukkit.getBanList(BanListType.PROFILE)
                        val banEndDate = if (gtime != null) Date(System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null
                        banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
                    }
                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)

                    if (targetPlayer != null) {
                        val kickMessages = plugin.messageHandler.getComplexMessage("ban", "kick_message", mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime)))
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        targetPlayer.kick(kickMessage.build())
                    }
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("ban", "ban", mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))))
                    val permission = "punisherx.see.ban"
                    val broadcastMessage = MiniMessage.miniMessage().deserialize(plugin.messageHandler.getMessage("ban", "broadcast", mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))))
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (onlinePlayer.hasPermission(permission)) {
                            onlinePlayer.sendMessage(broadcastMessage)
                        }
                    }
                    if (isForce) {
                        plugin.logger.warning("Force-banned by ${stack.sender.name} on $player")
                    }

                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("ban", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getReasons("ban", "reasons")
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
