package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.ban.BanListType
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.ban.ProfileBanList
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.*

class BanCommand(private var plugin: PunisherX) : BasicCommand {
    private val clp = plugin.commandLoggerPlugin

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
    if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BAN)) {  
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("ban", "usage"))
                } else {
                    val player = args[0]
                    val uuid = plugin.resolvePlayerUuid(player)
                    val targetPlayer = Bukkit.getPlayer(uuid)
                    val isForce = args.contains("--force")
                    if (targetPlayer != null) {
                        if (!isForce && PermissionChecker.hasWithLegacy(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BAN)) {
                            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "bypass", mapOf("player" to player)))
                            return
                        }
                    }
                    if(PermissionChecker.isAuthor(uuid)){
                        stack.sender.sendMessage(plugin.messageHandler.formatMixedTextToMiniMessage("<red>You can't punish the plugin author</red>",
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

                    val punishmentType = "BAN"
                    val start = System.currentTimeMillis()
                    val end: Long? = if (gtime != null) (System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null

                    val success = plugin.databaseHandler.addPunishment(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)
                    if (!success) {
                        plugin.logger.err("Failed to add ban to database for player $player. Using fallback method.")
                        stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "db_error"))
                        val playerProfile = Bukkit.createProfile(uuid, player)
                        val banList: ProfileBanList = Bukkit.getBanList(BanListType.PROFILE)
                        val banEndDate = if (gtime != null) Date(System.currentTimeMillis() + plugin.timeHandler.parseTime(gtime) * 1000) else null
                        banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
                    }
                    clp.logCommand(stack.sender.name, punishmentType, player, reason)
                    plugin.databaseHandler.addPunishmentHistory(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, end ?: -1)

                    if (targetPlayer != null) {
                        val kickMessages = plugin.messageHandler.getSmartMessage(
                            "ban",
                            "kick_message",
                            mapOf("reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                        )
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        targetPlayer.kick(kickMessage.build())
                    }

                    plugin.messageHandler.getSmartMessage(
                        "ban",
                        "ban",
                        mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                    ).forEach { stack.sender.sendMessage(it) }

                    val broadcastMessages = plugin.messageHandler.getSmartMessage(
                        "ban",
                        "broadcast",
                        mapOf("player" to player, "reason" to reason, "time" to plugin.timeHandler.formatTime(gtime))
                    )

                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if(PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_BAN)) {
                            broadcastMessages.forEach { onlinePlayer.sendMessage(it) }
                        }
                    }
                    if (isForce) {
                        plugin.logger.warning("Force-banned by ${stack.sender.name} on $player")
                    }

                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("ban", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.BAN)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> generateTimeSuggestions()
            3 -> plugin.messageHandler.getReasons("ban", "reasons")
            else -> emptyList()
        }
    }

    private fun generateTimeSuggestions(): List<String> {
        val units = listOf("s", "m", "h", "d")
        val suggestions = mutableListOf<String>()
        for (i in 1..999) {
            for (unit in units) {
                suggestions.add("$i$unit")
            }
        }
        return suggestions
    }
}
