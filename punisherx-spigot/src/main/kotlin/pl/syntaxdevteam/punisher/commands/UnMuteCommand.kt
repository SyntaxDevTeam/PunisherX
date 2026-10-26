package pl.syntaxdevteam.punisher.commands
import pl.syntaxdevteam.punisher.compatibility.*

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class UnMuteCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val options = PunishmentCommandOptions.parse(args)
        val arguments = options.arguments
        val silent = options.silent
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMUTE)) {
            if (arguments.isNotEmpty()) {
                val player = arguments[0]
                val uuid = plugin.resolvePlayerUuid(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type)
                        }
                    }
                    val placeholders = mapOf("player" to player, "operator" to stack.sender.name)
                    PunishmentCommandUtils.sendSenderMessages(plugin, stack, "unmute", "unmute", placeholders, silent = silent)
                    PunishmentCommandUtils.sendTargetMessages(plugin, Bukkit.getPlayer(player), "unmute", "unmute_message", placeholders, silent = silent)
                    plugin.logger.info("Player $player ($uuid) has been unmuted")
                } else {
                    stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "player_not_found", mapOf("player" to player, "operator" to stack.sender.name)))
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("unmute", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.UNMUTE)) {
            return emptyList()
        }
        val input = args.lastOrNull().orEmpty()
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(input, ignoreCase = true) }
            else -> emptyList()
        }
    }
}
