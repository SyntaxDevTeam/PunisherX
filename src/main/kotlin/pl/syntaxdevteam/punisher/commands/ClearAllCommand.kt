package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.Logger
import pl.syntaxdevteam.punisher.common.MessageHandler
import pl.syntaxdevteam.punisher.common.UUIDManager

@Suppress("UnstableApiUsage")
class ClearAllCommand(private val plugin: PunisherX) : BasicCommand {
    private var config = plugin.config
    private var debugMode = config.getBoolean("debug")
    var pluginMetas: PluginMeta = plugin.pluginMetas
    private val logger = Logger(pluginMetas, debugMode)
    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.clearall")) {
                val player = args[0]
                val uuid = uuidManager.getUUID(player).toString()
                val punishments = plugin.databaseHandler.getPunishments(uuid)
                if (punishments.isNotEmpty()) {
                    punishments.forEach { punishment ->
                        if (punishment.type == "MUTE" || punishment.type == "BAN" || punishment.type == "WARN") {
                            plugin.databaseHandler.removePunishment(uuid, punishment.type, true)
                        }
                    }
                    stack.sender.sendRichMessage(messageHandler.getMessage("clear", "clearall", mapOf("player" to player)))
                    val targetPlayer = Bukkit.getPlayer(player)
                    val getMessage = messageHandler.getMessage("clear", "clear_message")
                    targetPlayer?.sendRichMessage(getMessage)
                    logger.success("Player $player ($uuid) has been cleared of all punishments")
                } else {
                    stack.sender.sendRichMessage(messageHandler.getMessage("error", "player_not_found", mapOf("player" to player)))
                }
            } else {
                stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("clear", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
