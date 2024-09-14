package pl.syntaxdevteam.commands

import io.papermc.paper.ban.BanListType
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.ban.ProfileBanList
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.PunisherX
import pl.syntaxdevteam.helpers.MessageHandler
import pl.syntaxdevteam.helpers.TimeHandler
import pl.syntaxdevteam.helpers.UUIDManager
import java.util.*

@Suppress("UnstableApiUsage")
class BanCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)
    private val timeHandler = TimeHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.ban")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("ban", "usage"))
                } else {
                    val player = args[0]
                    val targetPlayer = Bukkit.getPlayer(player)
                    if (targetPlayer != null && targetPlayer.hasPermission("punisherx.bypass.ban")) {
                        stack.sender.sendRichMessage(messageHandler.getMessage("error", "bypass", mapOf("player" to player)))
                        return
                    }
                    val uuid = uuidManager.getUUID(player).toString()
                    var gtime: String?
                    var reason: String
                    try {
                        gtime = args[1]
                        timeHandler.parseTime(gtime)
                        reason = args.slice(2 until args.size).joinToString(" ")
                    } catch (e: NumberFormatException) {
                        gtime = null
                        reason = args.slice(1 until args.size).joinToString(" ")
                    }

                    val punishmentType = "BAN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + timeHandler.parseTime(gtime) * 1000) else null

                    val success = plugin.databaseHandler.addPunishment(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)
                    if (!success) {
                        val playerProfile = Bukkit.createProfile(UUID.fromString(uuid), player)
                        val banList: ProfileBanList = Bukkit.getBanList(BanListType.PROFILE)
                        val banEndDate = if (gtime != null) Date(System.currentTimeMillis() + timeHandler.parseTime(gtime) * 1000) else null
                        banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
                    }
                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)

                    if (targetPlayer != null) {
                        val kickMessages = messageHandler.getComplexMessage("ban", "kick_message", mapOf("reason" to reason, "time" to timeHandler.formatTime(gtime)))
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        targetPlayer.kick(kickMessage.build())
                    }
                    stack.sender.sendRichMessage(messageHandler.getMessage("ban", "ban", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime))))
                    val permission = "punisherx.see.ban"
                    val broadcastMessage = MiniMessage.miniMessage().deserialize(messageHandler.getMessage("ban", "broadcast", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime))))
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
            stack.sender.sendRichMessage(messageHandler.getMessage("ban", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> messageHandler.getReasons("ban", "reasons")
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
