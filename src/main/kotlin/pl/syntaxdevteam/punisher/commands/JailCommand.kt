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

class JailCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {

        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (args.isEmpty() || args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("jail", "usage"))
            return
        }

        val playerName = args[0]
        val uuid = plugin.resolvePlayerUuid(playerName)
        val targetPlayer = Bukkit.getPlayer(uuid)
        val isForce = args.contains("--force")

        if (targetPlayer != null && !isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_JAIL)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to playerName))
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
        val previousLocation = targetPlayer?.location?.clone()

        val jailLocation = JailUtils.getJailLocation(plugin.config)
        if (jailLocation == null) {
            plugin.logger.debug("<red>No jail location found! Teleportation aborted.</red>")
            return
        }
        plugin.logger.debug("<yellow>Jail location: ${jailLocation}</yellow>")

        val formattedTime = plugin.timeHandler.formatTime(gtime)
        val placeholders = mapOf(
            "player" to playerName,
            "operator" to stack.sender.name,
            "reason" to reason,
            "time" to formattedTime,
            "type" to punishmentType
        )

        fun finalizePunishment() {
            val success = plugin.databaseHandler.addPunishment(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            if (!success) {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                return
            }
            plugin.databaseHandler.addPunishmentHistory(playerName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
            plugin.cache.addOrUpdatePunishment(uuid, end ?: -1, previousLocation)

            targetPlayer?.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "jail", "jail_message",
                    placeholders
                )
            )

            val broadcastMessages = plugin.messageHandler.getSmartMessage(
                "jail", "broadcast",
                placeholders
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
                placeholders
            ).forEach { stack.sender.sendMessage(it) }
            plugin.actionExecutor.executeAction("jailed", playerName, placeholders)
        }

        if (targetPlayer != null) {
            plugin.safeTeleportService.teleportSafely(targetPlayer, jailLocation) { success ->
                if (success) {
                    targetPlayer.gameMode = GameMode.ADVENTURE
                    plugin.logger.debug("<green>Player successfully teleported to jail.</green>")
                    plugin.logger.debug("Changing gamemode (${targetPlayer.gameMode}) and teleporting ${targetPlayer.name} to $jailLocation")
                    finalizePunishment()
                } else {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent(
                            "jail",
                            "teleport_failed",
                            mapOf("player" to playerName)
                        )
                    )
                    plugin.logger.debug("<red>Failed to teleport player to jail. Command aborted.</red>")
                }
            }
        } else {
            finalizePunishment()
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.JAIL)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> TimeSuggestionProvider.generateTimeSuggestions()
            3 -> plugin.messageHandler.getMessageStringList("jail", "reasons")
            else -> emptyList()
        }
    }
}
