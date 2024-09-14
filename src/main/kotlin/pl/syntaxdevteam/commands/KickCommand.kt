package pl.syntaxdevteam.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.PunisherX
import pl.syntaxdevteam.helpers.MessageHandler
import pl.syntaxdevteam.helpers.UUIDManager

@Suppress("UnstableApiUsage")
class KickCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isNotEmpty()) {
            if (stack.sender.hasPermission("punisherx.kick")) {
                if (args.size < 2) {
                    stack.sender.sendRichMessage(messageHandler.getMessage("kick", "usage"))
                } else {
                    val player = args[0]
                    val targetPlayer = Bukkit.getPlayer(player)
                    if (targetPlayer != null && targetPlayer.hasPermission("punisherx.bypass.kick")) {
                        stack.sender.sendRichMessage(messageHandler.getMessage("error", "bypass", mapOf("player" to player)))
                        return
                    }
                    val uuid = uuidManager.getUUID(player).toString()
                    val reason = args.slice(1 until args.size).joinToString(" ")
                    val punishmentType = "KICK"
                    val start = System.currentTimeMillis()

                    plugin.databaseHandler.addPunishmentHistory(player, uuid, reason, stack.sender.name, punishmentType, start, start)

                    if (targetPlayer != null) {
                        val kickMessages = messageHandler.getComplexMessage("kick", "kick_message", mapOf("reason" to reason))
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        targetPlayer.kick(kickMessage.build())
                    }
                    stack.sender.sendRichMessage(messageHandler.getMessage("kick", "kick", mapOf("player" to player, "reason" to reason)))
                    val permission = "punisherx.see.kick"
                    val broadcastMessage = MiniMessage.miniMessage().deserialize(messageHandler.getMessage("kick", "broadcast", mapOf("player" to player, "reason" to reason)))
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (onlinePlayer.hasPermission(permission)) {
                            onlinePlayer.sendMessage(broadcastMessage)
                        }
                    }
                }
            } else {
                stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            }
        } else {
            stack.sender.sendRichMessage(messageHandler.getMessage("kick", "usage"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> messageHandler.getReasons("kick", "reasons")
            else -> emptyList()
        }
    }
}
