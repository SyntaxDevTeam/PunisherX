package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnWarnCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNWARN)) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val uuid = plugin.resolvePlayerUuid(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                val warnPunishments = punishments.filter { it.type == "WARN" }
                if (warnPunishments.isNotEmpty()) {
                    plugin.databaseHandler.removePunishment(uuid, "WARN")
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("unwarn", "unwarn", mapOf("player" to player)))
                    plugin.logger.info("Player $player ($uuid) has been unwarned")
                } else {
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("unwarn", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNWARN)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
