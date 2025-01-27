package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class ClearAllCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (stack.sender.hasPermission("punisherx.clearall")) {
            if (args.isNotEmpty()) {
                val player = args[0]
                val uuid = plugin.uuidManager.getUUID(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE" || punishment.type == "BAN" || punishment.type == "WARN") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                        }
                    }
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("clear", "clearall", mapOf("player" to player)))
                    val targetPlayer = Bukkit.getPlayer(player)
                    val getMessage = plugin.messageHandler.getMessage("clear", "clear_message")
                    targetPlayer?.sendRichMessage(getMessage)
                    plugin.logger.success("Player $player ($uuid) has been cleared of all punishments")
                } else {
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("clear", "usage"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!stack.sender.hasPermission("punisherx.clearall")) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
