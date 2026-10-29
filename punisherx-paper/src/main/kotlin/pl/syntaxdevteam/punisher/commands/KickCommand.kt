package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class KickCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val options = PunishmentCommandOptions.parse(args)
        val arguments = options.arguments
        val silent = options.silent
        val history: Boolean = plugin.config.getBoolean("kick.history", false)
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (!options.allowMark(plugin, stack)) return

        if (arguments.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("kick", "usage"))
            return
        }

        val targetArg = arguments[0]
        val isForce = options.force
        val reason = arguments.drop(1).joinToString(" ")
        val punishmentType = "KICK"
        val start = System.currentTimeMillis()
        val formattedTime = plugin.timeHandler.formatTime(null)

        if (targetArg.equals("all", ignoreCase = true)) {
            Bukkit.getOnlinePlayers().forEach { target ->
                if (target.name == stack.sender.name) return@forEach

                val uuid = target.uniqueId
                if (!isForce && PermissionChecker.hasWithBypass(target, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent(
                            "error", "bypass", mapOf("player" to target.name)
                        )
                    )
                    return@forEach
                }
                if (options.mark != null && plugin.databaseHandler.addMarkedPunishment(
                    target.name, uuid.toString(), reason, stack.sender.name, punishmentType, start, start, options.mark) == null) {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                    return@forEach
                }
                if(history) {
                    if (options.mark == null) plugin.databaseHandler.addPunishmentHistory(
                        target.name,
                        uuid.toString(),
                        reason,
                        stack.sender.name,
                        punishmentType,
                        start,
                        start
                    )
                }

                val placeholders = PunishmentCommandUtils.buildPlaceholders(
                    player = target.name,
                    operator = stack.sender.name,
                    reason = reason,
                    time = formattedTime,
                    type = punishmentType
                )
                PunishmentCommandUtils.sendKickMessage(plugin, target, "kick", "kick_message", placeholders)
                PunishmentCommandUtils.sendSenderMessages(plugin, stack, "kick", "kick", placeholders, silent = silent)
                plugin.actionExecutor.executeAction("kicked", target.name, placeholders, silent = silent, mark = options.mark)
            }

            val allPlaceholders = PunishmentCommandUtils.buildPlaceholders(
                player = "all",
                operator = stack.sender.name,
                reason = reason,
                time = formattedTime,
                type = punishmentType
            )
            PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_KICK, "kick", "broadcast", allPlaceholders, silent = silent)
            return
        }

        val uuid = plugin.resolvePlayerUuid(targetArg)
        val targetPlayer = Bukkit.getPlayer(uuid)

        if (targetPlayer != null) {
            if (!isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                stack.sender.sendMessage(
                    plugin.messageHandler.stringMessageToComponent(
                        "error", "bypass", mapOf("player" to targetArg)
                    )
                )
                return
            }
        }
        if (options.mark != null && plugin.databaseHandler.addMarkedPunishment(
            targetArg, uuid.toString(), reason, stack.sender.name, punishmentType, start, start, options.mark) == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            return
        }
        if(history) {
            if (options.mark == null) plugin.databaseHandler.addPunishmentHistory(
                targetArg,
                uuid.toString(),
                reason,
                stack.sender.name,
                punishmentType,
                start,
                start
            )
        }

        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetArg,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType
        )
        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "kick", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "kick", "kick", placeholders, silent = silent)
        plugin.actionExecutor.executeAction("kicked", targetArg, placeholders, silent = silent, mark = options.mark)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_KICK, "kick", "broadcast", placeholders, silent = silent)
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            return emptyList()
        }
        val input = args.lastOrNull().orEmpty()
        if (input.startsWith("--")) return listOf("--force", "--silent", "--mark", "--mark=")
            .filter { it.startsWith(input) && it !in args.dropLast(1) }

        return when (args.size) {
            0, 1 -> (listOf("all") + plugin.server.onlinePlayers.map { it.name }).filter { it.startsWith(input, ignoreCase = true) }
            2 -> plugin.messageHandler.getMessageStringList("kick", "reasons").filter { it.startsWith(input, ignoreCase = true) }
            else -> emptyList()
        }
    }
}
