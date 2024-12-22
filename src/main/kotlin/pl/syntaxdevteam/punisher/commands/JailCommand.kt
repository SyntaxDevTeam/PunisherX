package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import net.kyori.adventure.text.minimessage.MiniMessage
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils

@Suppress("UnstableApiUsage")
class JailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (args.isEmpty() || args.size < 2) {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("jail", "usage"))
            return
        }

        if (!stack.sender.hasPermission("punisherx.jail")) {
            stack.sender.sendRichMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        val playerName = args[0]
        val targetPlayer = Bukkit.getPlayer(playerName)
        val uuid = plugin.uuidManager.getUUID(playerName)
        val isForce = args.contains("--force")

        if (targetPlayer != null) {
            if (!isForce && targetPlayer.hasPermission("punisherx.bypass.jail")) {
                stack.sender.sendRichMessage(
                    plugin.messageHandler.getMessage("error", "bypass", mapOf("player" to playerName))
                )
                return
            }
        }

        var gtime: String?
        var reason: String
        try {
            gtime = args[1]
            plugin.timeHandler.parseTime(gtime)
            reason = args.slice(2 until args.size).filterNot { it == "--force" }.joinToString(" ")
        } catch (e: NumberFormatException) {
            gtime = null
            reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
        }

        val punishmentType = "JAIL"
        val start = System.currentTimeMillis()
        val end: Long? = if (gtime != null) start + plugin.timeHandler.parseTime(gtime) * 1000 else null

        val jailLocation = JailUtils.getJailLocation(plugin.config)
        if (jailLocation == null) {
            stack.sender.sendMessage(Component.text("Jail location is not set in the configuration!"))
            return
        }

        targetPlayer?.apply {
            gameMode = GameMode.ADVENTURE
            teleport(jailLocation)
            plugin.logger.info("Changing gamemode ($gameMode) and teleporting $name to $jailLocation")

            plugin.databaseHandler.addPunishment(name, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            plugin.databaseHandler.addPunishmentHistory(name, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)

            plugin.cache.addOrUpdatePunishment(uuid, end ?: -1)

            sendRichMessage(
                plugin.messageHandler.getMessage(
                    "jail", "jail_message",
                    mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                )
            )
        }

        val broadcastMessage = MiniMessage.miniMessage().deserialize(
            plugin.messageHandler.getMessage(
                "jail", "broadcast",
                mapOf("player" to playerName, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
            )
        )
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (onlinePlayer.hasPermission("punisherx.see.jail")) {
                onlinePlayer.sendMessage(broadcastMessage)
            }
        }

        stack.sender.sendRichMessage(
            plugin.messageHandler.getMessage(
                "jail", "jail",
                mapOf("player" to playerName, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
            )
        )
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getReasons("jail", "reasons")
            else -> emptyList()
        }
    }

    private fun generateTimeSuggestions(): List<String> {
        val units = listOf("s", "m", "h", "d")
        return (1..999).flatMap { i -> units.map { unit -> "$i$unit" } }
    }
}
