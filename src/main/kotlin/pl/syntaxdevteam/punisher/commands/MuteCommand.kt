package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.MessageHandler
import pl.syntaxdevteam.punisher.basic.TimeHandler
import pl.syntaxdevteam.punisher.common.UUIDManager

@Suppress("UnstableApiUsage")
class MuteCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)
    private val timeHandler = TimeHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.mute")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("mute", "usage"))
                } else {
                    val player = args[0]
                    val targetPlayer = Bukkit.getPlayer(player)
                    val isForce = args.contains("--force")
                    if (targetPlayer != null) {
                        if (!isForce && targetPlayer.hasPermission("punisherx.bypass.mute")) {
                            stack.sender.sendRichMessage(
                                messageHandler.getMessage(
                                    "error",
                                    "bypass",
                                    mapOf("player" to player)
                                )
                            )
                            return
                        }
                    }
                    val uuid = uuidManager.getUUID(player).toString()

                    var gtime: String?
                    var reason: String
                    try {
                        gtime = args[1]
                        timeHandler.parseTime(gtime) // Sprawdzenie, czy gtime jest poprawnym czasem
                        reason = args.slice(2 until args.size).filterNot { it == "--force" }.joinToString(" ")
                    } catch (e: NumberFormatException) {
                        gtime = null
                        reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
                    }

                    val punishmentType = "MUTE"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + timeHandler.parseTime(gtime) * 1000) else null

                    plugin.databaseHandler.addPunishment(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)
                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)

                    stack.sender.sendRichMessage(messageHandler.getMessage("mute", "mute", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime))))
                    val muteMessage = messageHandler.getMessage("mute", "mute_message", mapOf("reason" to reason, "time" to timeHandler.formatTime(gtime)))
                    val formattedMessage = MiniMessage.miniMessage().deserialize(muteMessage)
                    targetPlayer?.sendMessage(formattedMessage)
                    val permission = "punisherx.see.mute"
                    val broadcastMessage = MiniMessage.miniMessage().deserialize(messageHandler.getMessage("mute", "broadcast", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime))))
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
            stack.sender.sendRichMessage(messageHandler.getMessage("mute", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> messageHandler.getReasons("mute", "reasons")
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
