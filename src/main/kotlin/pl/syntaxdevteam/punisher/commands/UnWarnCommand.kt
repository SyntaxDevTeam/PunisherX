package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class UnWarnCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.unwarn")) {
                val player = args[0]
                val uuid = plugin.uuidManager.getUUID(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                val warnPunishments = punishments.filter { it.type == "WARN" }
                if (warnPunishments.isNotEmpty()) {
                    plugin.databaseHandler.removePunishment(uuid, "WARN")
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unwarn", "unwarn", mapOf("player" to player)))
                    plugin.logger.info("Player $player ($uuid) has been unwarned")
                } else {
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unwarn", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
