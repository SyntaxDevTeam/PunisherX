package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class MuteCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if(PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MUTE)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("mute", "usage"))
                } else {
                    val player = args[0]
                    val targetPlayer = Bukkit.getPlayer(player)
                    val isForce = args.contains("--force")
                    if (targetPlayer != null) {
                        if (!isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_MUTE)) {
                            stack.sender.sendMessage(
                                plugin.messageHandler.getMessage(
                                    "error",
                                    "bypass",
                                    mapOf("player" to player)
                                )
                            )
                            return
                        }
                    }
                    val uuid = plugin.resolvePlayerUuid(player).toString()

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

                    val punishmentType = "MUTE"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    plugin.databaseHandler.addPunishment(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)
                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, end ?: -1)

                    plugin.messageHandler.getSmartMessage(
                        "mute",
                        "mute",
                        mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                    ).forEach { stack.sender.sendMessage(it) }

                    val muteMessages = plugin.messageHandler.getSmartMessage(
                        "mute",
                        "mute_message",
                        mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                    )
                    targetPlayer?.let { p -> muteMessages.forEach { p.sendMessage(it) } }

                    val broadcastMessages = plugin.messageHandler.getSmartMessage(
                        "mute",
                        "broadcast",
                        mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                    )
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_MUTE)) {
                            broadcastMessages.forEach { onlinePlayer.sendMessage(it) }
                        }
                    }
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("mute", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.MUTE)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getReasons("mute", "reasons")
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
