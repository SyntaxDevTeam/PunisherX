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

        val player = Bukkit.getPlayer(playerName)
        if (player != null) {
            val spawnLocation = plugin.server.worlds[0].spawnLocation

            if (plugin.server.name.contains("Folia")) {
                Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                    try {
                        player.teleportAsync(spawnLocation).thenAccept { success ->
                            if (success) {
                                player.gameMode = GameMode.SURVIVAL
                                player.sendRichMessage(plugin.messageHandler.getMessage("unjail", "unjail_message"))
                                plugin.logger.debug("<green>Player $playerName successfully unjailed (Folia).</green>")
                            } else {
                                plugin.logger.debug("<red>Failed to teleport player $playerName during unjail (Folia).</red>")
                            }
                        }.exceptionally { throwable ->
                            plugin.logger.debug("<red>Error while teleporting $playerName during unjail: ${throwable.message}</red>")
                            null
                        }
                    } catch (e: Exception) {
                        plugin.logger.debug("<red>Error during Folia unjail for $playerName: ${e.message}</red>")
                    }
                }
            } else {
                player.teleport(spawnLocation)
                player.gameMode = GameMode.SURVIVAL
                player.sendRichMessage(plugin.messageHandler.getMessage("unjail", "unjail_message"))
            }
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
