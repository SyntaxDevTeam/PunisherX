package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnjailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val options = PunishmentCommandOptions.parse(args)
        val arguments = options.arguments
        val silent = options.silent

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNJAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (arguments.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unjail", "usage"))
            return
        }

        val playerName = arguments[0]
        val uuid = plugin.resolvePlayerUuid(playerName)

        if (!plugin.cache.isPlayerInCache(uuid)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "error",
                    "player_not_punished",
                    mapOf("player" to playerName, "operator" to stack.sender.name)
                )
            )
            return
        }

        val player = Bukkit.getPlayer(uuid)
        val releaseLocation = plugin.cache.getReleaseLocation(uuid)

        if (player != null && releaseLocation == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("setunjail", "set_error"))
            return
        }

        fun completeUnjail() {
            plugin.cache.removePunishment(uuid, teleportPlayer = false, notify = false)

            val placeholders = mapOf("player" to playerName, "operator" to stack.sender.name)
            PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_UNJAIL, "unjail", "broadcast", placeholders, silent = silent)
            PunishmentCommandUtils.sendSenderMessages(plugin, stack, "unjail", "success", placeholders, silent = silent)
        }

        if (player != null && releaseLocation != null) {
            plugin.safeTeleportService.teleportSafely(player, releaseLocation) { success ->
                if (success) {
                    player.gameMode = GameMode.SURVIVAL
                    PunishmentCommandUtils.sendTargetMessages(
                        plugin, player, "unjail", "unjail_message",
                        mapOf("player" to playerName, "operator" to stack.sender.name), silent = silent
                    )
                    plugin.logger.debug("<green>Player $playerName successfully unjailed.</green>")
                    completeUnjail()
                } else {
                    val failureMessage = plugin.messageHandler.stringMessageToComponent(
                        "unjail",
                        "teleport_failed",
                        mapOf("player" to playerName, "operator" to stack.sender.name)
                    )
                    stack.sender.sendMessage(failureMessage)
                    if (!silent) player.sendMessage(failureMessage)
                    plugin.logger.debug("<red>Failed to teleport player $playerName during unjail.</red>")
                }
            }
        } else {
            completeUnjail()
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNJAIL)) {
            return emptyList()
        }
        val input = args.lastOrNull().orEmpty()
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(input, ignoreCase = true) }
            else -> emptyList()
        }
    }
}
