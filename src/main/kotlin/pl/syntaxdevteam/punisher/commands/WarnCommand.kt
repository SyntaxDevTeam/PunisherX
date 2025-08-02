package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class WarnCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.WARN)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("warn", "usage"))
                } else {
                    val player = args[0]
                    val uuid = plugin.uuidManager.getUUID(player)
                    val targetPlayer = Bukkit.getPlayer(uuid)
                    if (targetPlayer != null && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_WARN)) {
                        stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "bypass", mapOf("player" to player)))
                        return
                    }
                    var gtime: String?
                    var reason: String
                    try {
                        gtime = args[1]
                        plugin.timeHandler.parseTime(gtime)
                        reason = args.slice(2 until args.size).joinToString(" ")
                    } catch (e: NumberFormatException) {
                        gtime = null
                        reason = args.slice(1 until args.size).joinToString(" ")
                    }

                    val punishmentType = "WARN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    plugin.databaseHandler.addPunishment(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
                    plugin.databaseHandler.addPunishmentHistory(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)

                    val warnCount = plugin.databaseHandler.getActiveWarnCount(uuid.toString())
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("warn", "warn", mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime), "warn_no" to warnCount.toString())))
                    val warnMessage = plugin.messageHandler.getMessage("warn", "warn_message", mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime), "warn_no" to warnCount.toString()))
                    targetPlayer?.sendMessage(warnMessage)

                    val broadcastMessage = plugin.messageHandler.getMessage("warn", "broadcast", mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime), "warn_no" to warnCount.toString()))
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_WARN)) {
                            onlinePlayer.sendMessage(broadcastMessage)
                        }
                    }
                    executeWarnAction(player, warnCount)
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("warn", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.WARN)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getReasons("warn", "reasons")
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
        val warnActions = plugin.config.getConfigurationSection("warn.actions")?.getKeys(false)
        warnActions?.forEach { key ->
            val warnThreshold = key.toIntOrNull()
            if (warnThreshold != null && warnCount == warnThreshold) {
                val command = plugin.config.getString("warn.actions.$key")
                if (command != null) {
                    val formattedCommand = command.replace("{player}", player).replace("{warn_no}", warnCount.toString())
                    plugin.server.dispatchCommand(plugin.server.consoleSender, formattedCommand)
                    plugin.logger.debug("Executed command for $player: $formattedCommand")
                }
            }
        }
    }
}