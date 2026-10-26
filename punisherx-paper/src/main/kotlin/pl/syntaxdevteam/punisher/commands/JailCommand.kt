package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class JailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val options = PunishmentCommandOptions.parse(args)
        val arguments = options.arguments
        val silent = options.silent

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (arguments.isEmpty() || arguments.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("jail", "usage"))
            return
        }

        val playerName = arguments[0]
        val uuid = plugin.resolvePlayerUuid(playerName)
        val targetPlayer = Bukkit.getPlayer(uuid)
        val isForce = options.force

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_JAIL)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to playerName))
            )
            return
        }
        val (gtime, reason) = PunishmentCommandUtils.parseTimeAndReason(plugin, arguments, 1)

        val punishmentType = "JAIL"
        val start = System.currentTimeMillis()
        val end: Long? = if (gtime != null) start + plugin.timeHandler.parseTime(gtime) * 1000 else null
        val previousLocation = targetPlayer?.location?.clone()

        val jailLocation = JailUtils.getJailLocation(plugin.config)
        if (jailLocation == null) {
            plugin.logger.debug("<red>No jail location found! Teleportation aborted.</red>")
            return
        }
        plugin.logger.debug("<yellow>Jail location: ${jailLocation}</yellow>")

        val formattedTime = plugin.timeHandler.formatTime(gtime)
        val basePlaceholders = mapOf(
            "player" to playerName,
            "operator" to stack.sender.name,
            "reason" to reason,
            "time" to formattedTime,
            "type" to punishmentType
        )

        fun finalizePunishment() {
            val punishmentId = plugin.databaseHandler.addPunishment(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            if (punishmentId == null) {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                return
            }
            plugin.databaseHandler.addPunishmentHistory(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            plugin.cache.addOrUpdatePunishment(uuid, end ?: -1, previousLocation)
            val placeholders = basePlaceholders + ("id" to punishmentId.toString())

            PunishmentCommandUtils.sendTargetMessages(plugin, targetPlayer, "jail", "jail_message", placeholders, silent = silent)
            PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_JAIL, "jail", "broadcast", placeholders, silent = silent)
            PunishmentCommandUtils.sendSenderMessages(plugin, stack, "jail", "jail", placeholders, silent = silent)
            plugin.actionExecutor.executeAction("jailed", playerName, placeholders, silent = silent)
        }

        if (targetPlayer != null) {
            plugin.safeTeleportService.teleportSafely(targetPlayer, jailLocation) { success ->
                if (success) {
                    targetPlayer.gameMode = GameMode.ADVENTURE
                    plugin.logger.debug("<green>Player successfully teleported to jail.</green>")
                    plugin.logger.debug("Changing gamemode (${targetPlayer.gameMode}) and teleporting ${targetPlayer.name} to $jailLocation")
                    finalizePunishment()
                } else {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent(
                            "jail",
                            "teleport_failed",
                            mapOf("player" to playerName)
                        )
                    )
                    plugin.logger.debug("<red>Failed to teleport player to jail. Command aborted.</red>")
                }
            }
        } else {
            finalizePunishment()
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            return emptyList()
        }
        val input = args.lastOrNull().orEmpty()
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(input, ignoreCase = true) }
            2 -> TimeSuggestionProvider.generateTimeSuggestions().filter { it.startsWith(input, ignoreCase = true) }
            3 -> plugin.messageHandler.getMessageStringList("jail", "reasons").filter { it.startsWith(input, ignoreCase = true) }
            else -> emptyList()
        }
    }
}
