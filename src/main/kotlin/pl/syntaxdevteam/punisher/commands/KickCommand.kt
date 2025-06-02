package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

@Suppress("UnstableApiUsage")
class KickCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        if (PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            if (args.isNotEmpty()) {
                if (args.size < 2) {
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("kick", "usage"))
                } else {
                    val player = args[0]
                    val uuid = plugin.uuidManager.getUUID(player)
                    val targetPlayer = Bukkit.getPlayer(uuid)
                    val isForce = args.contains("--force")
                    if (targetPlayer != null) {
                        if (!isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                            stack.sender.sendMessage(
                                plugin.messageHandler.getMessage(
                                    "error",
                                    "bypass",
                                    mapOf("player" to player)
                                )
                            )
                            return
                        }

                    }
                    if(PermissionChecker.isAuthor(uuid)){
                        stack.sender.sendMessage(plugin.messageHandler.formatMixedTextToMiniMessage("<red>You can't punish the plugin author</red>"))
                        return
                    }
                    val reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
                    val punishmentType = "KICK"
                    val start = System.currentTimeMillis()

                    plugin.databaseHandler.addPunishmentHistory(player, uuid.toString(), reason, stack.sender.name, punishmentType, start, start)

                    if (targetPlayer != null) {
                        val kickMessages = plugin.messageHandler.getComplexMessage("kick", "kick_message", mapOf("reason" to reason))
                        val kickMessage = Component.text()
                        kickMessages.forEach { line ->
                            kickMessage.append(line)
                            kickMessage.append(Component.newline())
                        }
                        targetPlayer.kick(kickMessage.build())
                    }
                    stack.sender.sendMessage(plugin.messageHandler.getMessage("kick", "kick", mapOf("player" to player, "reason" to reason)))

                    val broadcastMessage = plugin.messageHandler.getMessage("kick", "broadcast", mapOf("player" to player, "reason" to reason))
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_KICK)) {
                            onlinePlayer.sendMessage(broadcastMessage)
                        }
                    }
                }
            } else {
                stack.sender.sendMessage(plugin.messageHandler.getMessage("kick", "usage"))
            }
        } else {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> plugin.server.onlinePlayers.map { it.name }
            2 -> plugin.messageHandler.getReasons("kick", "reasons")
            else -> emptyList()
        }
    }
}
