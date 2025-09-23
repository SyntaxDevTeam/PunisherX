package pl.syntaxdevteam.punisher.commands

import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.GameMode
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.common.TeleportUtils

class JailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }
        if (args.isEmpty() || args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("jail", "usage"))
            return
        }

        val playerName = args[0]
        val uuid = plugin.resolvePlayerUuid(playerName)
        val targetPlayer = Bukkit.getPlayer(uuid)
        val isForce = args.contains("--force")

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_JAIL)) {
            stack.sender.sendMessage(
                plugin.messageHandler.getMessage("error", "bypass", mapOf("player" to playerName))
            )
            return
        }
        val prefix = plugin.messageHandler.getPrefix()
        if(PermissionChecker.isAuthor(uuid)){
            stack.sender.sendMessage(plugin.messageHandler.formatMixedTextToMiniMessage("$prefix <red>You can't punish the plugin author</red>",
                TagResolver.empty()))
            return
        }

        var gtime: String?
        var reason: String
        try {
            gtime = args[1]
            plugin.timeHandler.parseTime(gtime)
            reason = args.slice(2 until args.size).filterNot { it == "--force" }.joinToString(" ")
        } catch (_: NumberFormatException) {
            gtime = null
            reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
        }

        val punishmentType = "JAIL"
        val start = System.currentTimeMillis()
        val end: Long? = if (gtime != null) start + plugin.timeHandler.parseTime(gtime) * 1000 else null

        val jailLocation = JailUtils.getJailLocation(plugin.config)
        if (jailLocation == null) {
            plugin.logger.debug("<red>No jail location found! Teleportation aborted.</red>")
            return
        }
        plugin.logger.debug("<yellow>Jail location: ${jailLocation}</yellow>")

        targetPlayer?.apply {
            TeleportUtils.teleportSafely(plugin, this, jailLocation) { success ->
                if (success) {
                    plugin.logger.debug("<green>Player successfully teleported to jail.</green>")
                } else {
                    plugin.logger.debug("<red>Failed to teleport player to jail.</red>")
                }
            }
            gameMode = GameMode.ADVENTURE

            plugin.logger.debug("Changing gamemode ($gameMode) and teleporting $name to $jailLocation")

        }

        plugin.databaseHandler.addPunishment(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
        plugin.databaseHandler.addPunishmentHistory(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
        plugin.cache.addOrUpdatePunishment(uuid, end ?: -1)

        targetPlayer?.sendMessage(
            plugin.messageHandler.getMessage(
                "jail", "jail_message",
                mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
            )
        )
        val broadcastMessages = plugin.messageHandler.getSmartMessage(
            "jail", "broadcast",
            mapOf("player" to playerName, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
        )
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_JAIL)) {
                broadcastMessages.forEach { message ->
                    onlinePlayer.sendMessage(message)
                }
            }
        }

        plugin.messageHandler.getSmartMessage(
            "jail",
            "jail",
            mapOf("player" to playerName, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
        ).forEach { stack.sender.sendMessage(it) }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            return emptyList()
        }
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
