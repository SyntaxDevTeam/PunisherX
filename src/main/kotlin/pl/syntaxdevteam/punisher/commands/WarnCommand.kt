package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.TimeHandler
import pl.syntaxdevteam.punisher.common.MessageHandler
import pl.syntaxdevteam.punisher.common.UUIDManager

@Suppress("UnstableApiUsage")
class WarnCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)
    private val timeHandler = TimeHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.warn")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("warn", "usage"))
                } else {
                    val player = args[0]
                    val targetPlayer = Bukkit.getPlayer(player)
                    if (targetPlayer != null && targetPlayer.hasPermission("punisherx.bypass.warn")) {
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

                    val punishmentType = "WARN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + timeHandler.parseTime(gtime) * 1000) else null

                    plugin.databaseHandler.addPunishment(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)
                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)

                    val warnCount = plugin.databaseHandler.getActiveWarnCount(uuid)
                    stack.sender.sendRichMessage(messageHandler.getMessage("warn", "warn", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime), "warn_no" to warnCount.toString())))
                    val warnMessage = messageHandler.getMessage("warn", "warn_message", mapOf("reason" to reason, "time" to timeHandler.formatTime(gtime), "warn_no" to warnCount.toString()))
                    val formattedMessage = MiniMessage.miniMessage().deserialize(warnMessage)
                    targetPlayer?.sendMessage(formattedMessage)
                    val permission = "punisherx.see.warn"
                    val broadcastMessage = MiniMessage.miniMessage().deserialize(messageHandler.getMessage("warn", "broadcast", mapOf("player" to player, "reason" to reason, "time" to timeHandler.formatTime(gtime), "warn_no" to warnCount.toString())))
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (onlinePlayer.hasPermission(permission)) {
                            onlinePlayer.sendMessage(broadcastMessage)
                        }
                    }
                    executeWarnAction(player, warnCount)
                }
            } else {
                stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("warn", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> messageHandler.getReasons("warn", "reasons")
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

    private fun executeWarnAction(player: String, warnCount: Int) {
        val warnActions = plugin.config.getConfigurationSection("WarnActions")?.getKeys(false)
        warnActions?.forEach { key ->
            val warnThreshold = key.toIntOrNull()
            if (warnThreshold != null && warnCount == warnThreshold) {
                val command = plugin.config.getString("WarnActions.$key")
                if (command != null) {
                    val formattedCommand = command.replace("{player}", player).replace("{warn_no}", warnCount.toString())
                    plugin.server.dispatchCommand(plugin.server.consoleSender, formattedCommand)
                    plugin.logger.debug("Executed command for $player: $formattedCommand")
                }
            }
        }
    }
}