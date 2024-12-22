package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import net.kyori.adventure.text.minimessage.MiniMessage
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX

@Suppress("UnstableApiUsage")
class UnjailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isEmpty()) {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("unjail", "usage"))
            return
        }

        if (!stack.sender.hasPermission("punisherx.unjail")) {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        val playerName = args[0]
        val uuid = plugin.uuidManager.getUUID(playerName)

        Bukkit.getPlayer(playerName)?.apply {
            gameMode = GameMode.SURVIVAL
            teleport(plugin.server.worlds[0].spawnLocation)
            sendRichMessage(plugin.messageHandler.getMessage("unjail", "unjail_message"))
        }

        plugin.cache.removePunishment(uuid)
        plugin.databaseHandler.removePunishment(uuid.toString(), "JAIL")

        val broadcastMessage = MiniMessage.miniMessage().deserialize(
            plugin.messageHandler.getMessage("unjail", "broadcast", mapOf("player" to playerName))
        )
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (onlinePlayer.hasPermission("punisherx.see.unjail")) {
                onlinePlayer.sendMessage(broadcastMessage)
            }
        }

        stack.sender.sendRichMessage(
            plugin.messageHandler.getMessage("unjail", "success", mapOf("player" to playerName))
        )
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            else -> emptyList()
        }
    }
}
