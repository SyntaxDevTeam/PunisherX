package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class UnMuteCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.unmute")) {
                val player = args[0]
                val uuid = plugin.uuidManager.getUUID(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type)
                        }
                    }
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unmute", "unmute", mapOf("player" to player)))
                    val targetPlayer = Bukkit.getPlayer(player)
                    val muteMessage = plugin.messageHandler.getMessage("unmute", "unmute_message")
                    val formattedMessage = MiniMessage.miniMessage().deserialize(muteMessage)
                    targetPlayer?.sendMessage(formattedMessage)
                    plugin.logger.info("Player $player ($uuid) has been unmuted")
                } else {
                    stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unmute", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
