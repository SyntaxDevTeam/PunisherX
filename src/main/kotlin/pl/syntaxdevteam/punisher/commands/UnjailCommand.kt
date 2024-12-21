package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import net.kyori.adventure.text.minimessage.MiniMessage
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.configuration.PluginMeta
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.common.MessageHandler
import pl.syntaxdevteam.punisher.common.UUIDManager

@Suppress("UnstableApiUsage")
class UnjailCommand(private val plugin: PunisherX, pluginMetas: PluginMeta) : BasicCommand {

    private val uuidManager = UUIDManager(plugin)
    private val messageHandler = MessageHandler(plugin, pluginMetas)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isEmpty()) {
            stack.sender.sendRichMessage(messageHandler.getMessage("unjail", "usage"))
            return
        }

        if (!stack.sender.hasPermission("punisherx.unjail")) {
            stack.sender.sendRichMessage(messageHandler.getMessage("error", "no_permission"))
            return
        }

        val playerName = args[0]
        val uuid = uuidManager.getUUID(playerName)

        Bukkit.getPlayer(playerName)?.apply {
            gameMode = GameMode.SURVIVAL
            teleport(plugin.server.worlds[0].spawnLocation)
            sendRichMessage(messageHandler.getMessage("unjail", "unjail_message"))
        }

        plugin.cache.removePunishment(uuid)
        plugin.databaseHandler.removePunishment(uuid.toString(), "JAIL")

        val broadcastMessage = MiniMessage.miniMessage().deserialize(
            messageHandler.getMessage("unjail", "broadcast", mapOf("player" to playerName))
        )
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (onlinePlayer.hasPermission("punisherx.see.unjail")) {
                onlinePlayer.sendMessage(broadcastMessage)
            }
        }

        stack.sender.sendRichMessage(
            messageHandler.getMessage("unjail", "success", mapOf("player" to playerName))
        )
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
