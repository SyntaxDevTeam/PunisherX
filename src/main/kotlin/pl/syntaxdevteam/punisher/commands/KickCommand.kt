package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.permissions.PermissionChecker

class KickCommand(private val plugin: PunisherX) : BasicCommand {

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val history: Boolean = plugin.config.getBoolean("kick.history", false)
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("error", "no_permission"))
            return
        }

        if (args.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.getMessage("kick", "usage"))
            return
        }

        val targetArg = args[0]
        val isForce = args.contains("--force")
        val reason = args.slice(1 until args.size).filterNot { it == "--force" }.joinToString(" ")
        val punishmentType = "KICK"
        val start = System.currentTimeMillis()

        if (targetArg.equals("all", ignoreCase = true)) {
            Bukkit.getOnlinePlayers().forEach { target ->
                if (target.name == stack.sender.name) return@forEach

                val uuid = target.uniqueId
                if (!isForce && PermissionChecker.hasWithBypass(target, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                    stack.sender.sendMessage(
                        plugin.messageHandler.getMessage(
                            "error", "bypass", mapOf("player" to target.name)
                        )
                    )
                    return@forEach
                }

                if (PermissionChecker.isAuthor(uuid)) {
                    // Skip plugin author
                    return@forEach
                }

                if(history) {
                    plugin.databaseHandler.addPunishmentHistory(
                        target.name,
                        uuid.toString(),
                        reason,
                        stack.sender.name,
                        punishmentType,
                        start,
                        start
                    )
                }

                // Kick with message
                val kickMessages = plugin.messageHandler.getComplexMessage("kick", "kick_message", mapOf("reason" to reason))
                val kickMessageBuilder = Component.text()
                kickMessages.forEach { line ->
                    kickMessageBuilder.append(line).append(Component.newline())
                }
                target.kick(kickMessageBuilder.build())

                stack.sender.sendMessage(
                    plugin.messageHandler.getMessage(
                        "kick", "kick", mapOf("player" to target.name, "reason" to reason)
                    )
                )
            }

            val broadcastMessage = plugin.messageHandler.getMessage("kick", "broadcast", mapOf("player" to "all", "reason" to reason))
            plugin.server.onlinePlayers.forEach { onlinePlayer ->
                if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_KICK)) {
                    onlinePlayer.sendMessage(broadcastMessage)
                }
            }
            return
        }

        val uuid = plugin.uuidManager.getUUID(targetArg)
        val targetPlayer = Bukkit.getPlayer(uuid)

        if (targetPlayer != null) {
            if (!isForce && PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_KICK)) {
                stack.sender.sendMessage(
                    plugin.messageHandler.getMessage(
                        "error", "bypass", mapOf("player" to targetArg)
                    )
                )
                return
            }
        }

        if (PermissionChecker.isAuthor(uuid)) {
            stack.sender.sendMessage(
                plugin.messageHandler.formatMixedTextToMiniMessage("<red>You can't punish the plugin author</red>")
            )
            return
        }

        if(history) {
            plugin.databaseHandler.addPunishmentHistory(
                targetArg,
                uuid.toString(),
                reason,
                stack.sender.name,
                punishmentType,
                start,
                start
            )
        }

        if (targetPlayer != null) {
            val kickMessages = plugin.messageHandler.getComplexMessage("kick", "kick_message", mapOf("reason" to reason))
            val kickMessageBuilder = Component.text()
            kickMessages.forEach { line ->
                kickMessageBuilder.append(line).append(Component.newline())
            }
            targetPlayer.kick(kickMessageBuilder.build())
        }

        stack.sender.sendMessage(
            plugin.messageHandler.getMessage("kick", "kick", mapOf("player" to targetArg, "reason" to reason))
        )

        val broadcastMessage = plugin.messageHandler.getMessage("kick", "broadcast", mapOf("player" to targetArg, "reason" to reason))
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_KICK)) {
                onlinePlayer.sendMessage(broadcastMessage)
            }
        }
    }

    override fun suggest(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.KICK)) {
            return emptyList()
        }
        return when (args.size) {
            1 -> listOf("all") + plugin.server.onlinePlayers.map { it.name }
            2 -> plugin.messageHandler.getReasons("kick", "reasons")
            else -> emptyList()
        }
    }
}
