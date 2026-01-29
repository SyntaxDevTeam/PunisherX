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
                    val (gtime, reason) = PunishmentCommandUtils.parseTimeAndReason(plugin, args, 1)

                    val punishmentType = "WARN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    val punishmentId = plugin.databaseHandler.addPunishment(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
                    if (punishmentId == null) {
                        stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                        return
                    }
                    plugin.databaseHandler.addPunishmentHistory(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)

                    val warnCount = plugin.databaseHandler.getActiveWarnCount(uuid.toString())
                    val formattedTime = plugin.timeHandler.formatTime(gtime)
                    val placeholders = PunishmentCommandUtils.buildPlaceholders(
                        player = player,
                        operator = stack.sender.name,
                        reason = reason,
                        time = formattedTime,
                        type = punishmentType,
                        extra = mapOf(
                            "warn_no" to warnCount.toString(),
                            "id" to punishmentId.toString()
                        )
                    )

                    PunishmentCommandUtils.sendSenderMessages(plugin, stack, "warn", "warn", placeholders)
                    PunishmentCommandUtils.sendTargetMessages(plugin, targetPlayer, "warn", "warn_message", placeholders)
                    PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_WARN, "warn", "broadcast", placeholders)
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
            0, 1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            3 -> plugin.messageHandler.getMessageStringList("warn", "reasons")
            else -> emptyList()
        }
    }

}
