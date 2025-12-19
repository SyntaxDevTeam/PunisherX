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
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("warn", "usage"))
                } else {
                    val player = args[0]
                    val uuid = plugin.resolvePlayerUuid(player)
                    val targetPlayer = Bukkit.getPlayer(uuid)
                    val isForce = args.contains("--force")
                    if (!isForce && targetPlayer != null && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_WARN)) {
                        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to player)))
                        return
                    }
                    var gtime: String?
                    var reason: String
                    try {
                        gtime = args[1]
                        plugin.timeHandler.parseTime(gtime)
                        reason = args.slice(2 until args.size).filterNot { it == "--force" }.joinToString(" ")
                    } catch (_: NumberFormatException) {
                        gtime = null
                        reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
                    }

                    val punishmentType = "WARN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    val success = plugin.databaseHandler.addPunishment(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
                    if (!success) {
                        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                        return
                    }
                    plugin.databaseHandler.addPunishmentHistory(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)

                    val warnCount = plugin.databaseHandler.getActiveWarnCount(uuid.toString())
                    val formattedTime = plugin.timeHandler.formatTime(gtime)
                    val placeholders = mapOf(
                        "player" to player,
                        "operator" to stack.sender.name,
                        "reason" to reason,
                        "time" to formattedTime,
                        "type" to punishmentType,
                        "warn_no" to warnCount.toString()
                    )

                    plugin.messageHandler.getSmartMessage(
                        "warn",
                        "warn",
                        placeholders
                    ).forEach { stack.sender.sendMessage(it) }

                    val warnMessages = plugin.messageHandler.getSmartMessage(
                        "warn",
                        "warn_message",
                        placeholders
                    )
                    targetPlayer?.let { p -> warnMessages.forEach { p.sendMessage(it) } }

                    val broadcastMessages = plugin.messageHandler.getSmartMessage(
                        "warn",
                        "broadcast",
                        placeholders
                    )
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_WARN)) {
                            broadcastMessages.forEach { onlinePlayer.sendMessage(it) }
                        }
                    }
                    if (isForce) {
                        plugin.logger.warning("Force-warned by ${stack.sender.name} on $player")
                    }
                    plugin.actionExecutor.executeWarnCountActions(player, warnCount)
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("warn", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.WARN)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getMessageStringList("warn", "reasons")
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
