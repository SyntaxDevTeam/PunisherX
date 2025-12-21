package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.Bukkit
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnjailCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNJAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unjail", "usage"))
            return
        }

        val playerName = args[0]
        val uuid = plugin.resolvePlayerUuid(playerName)

        if (!plugin.cache.isPlayerInCache(uuid)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "error",
                    "player_not_punished",
                    mapOf("player" to playerName)
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

            val broadcastMessages =
                plugin.messageHandler.getSmartMessage("unjail", "broadcast", mapOf("player" to playerName))

            plugin.server.onlinePlayers.forEach { onlinePlayer ->
                if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_UNJAIL)) {
                    broadcastMessages.forEach { msg -> onlinePlayer.sendMessage(msg) }
                }
            }

            plugin.messageHandler.getSmartMessage(
                "unjail",
                "success",
                mapOf("player" to playerName)
            ).forEach { stack.sender.sendMessage(it) }
        }

        if (player != null && releaseLocation != null) {
            plugin.safeTeleportService.teleportSafely(player, releaseLocation) { success ->
                if (success) {
                    player.gameMode = GameMode.SURVIVAL
                    plugin.messageHandler.getSmartMessage(
                        "unjail",
                        "unjail_message"
                    ).forEach { msg -> player.sendMessage(msg) }
                    plugin.logger.debug("<green>Player $playerName successfully unjailed.</green>")
                    completeUnjail()
                } else {
                    val failureMessage = plugin.messageHandler.stringMessageToComponent(
                        "unjail",
                        "teleport_failed",
                        mapOf("player" to playerName)
                    )
                    stack.sender.sendMessage(failureMessage)
                    player.sendMessage(failureMessage)
                    plugin.logger.debug("<red>Failed to teleport player $playerName during unjail.</red>")
                }
            }
        } else {
            completeUnjail()
        }
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNJAIL)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val playerArg = Commands.argument("player", ArgumentTypes.playerProfiles())
            .executes { context ->
                BrigadierCommandUtils.resolvePlayerProfileNames(context, "player")
                    .forEach { player -> execute(context.source, listOf(player)) }
                1
            }

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.UNJAIL))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(playerArg)
            .build()
    }
}
