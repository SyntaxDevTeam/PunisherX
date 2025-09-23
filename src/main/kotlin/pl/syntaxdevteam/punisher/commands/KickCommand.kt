package pl.syntaxdevteam.punisher.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
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

                val kickMessages = plugin.messageHandler.getSmartMessage(
                    "kick",
                    "kick_message",
                    mapOf("reason" to reason)
                )
                val kickMessageBuilder = Component.text()
                kickMessages.forEach { line ->
                    kickMessageBuilder.append(line).append(Component.newline())
                }
                target.kick(kickMessageBuilder.build())

                plugin.messageHandler.getSmartMessage(
                    "kick",
                    "kick",
                    mapOf("player" to target.name, "reason" to reason)
                ).forEach { stack.sender.sendMessage(it) }
            }

            val broadcastMessages = plugin.messageHandler.getSmartMessage(
                "kick",
                "broadcast",
                mapOf("player" to "all", "reason" to reason)
            )

            plugin.server.onlinePlayers.forEach { onlinePlayer ->
                if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_KICK)) {
                    broadcastMessages.forEach { onlinePlayer.sendMessage(it) }
                }
            }
            return
        }

        val uuid = plugin.resolvePlayerUuid(targetArg)
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
      val prefix = plugin.messageHandler.getPrefix()
        if (PermissionChecker.isAuthor(uuid)) {
            stack.sender.sendMessage(
                plugin.messageHandler.formatMixedTextToMiniMessage("$prefix <red>You can't punish the plugin author</red>",
                    TagResolver.empty())
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
            val kickMessages = plugin.messageHandler.getSmartMessage(
                "kick",
                "kick_message",
                mapOf("reason" to reason)
            )
            val kickMessageBuilder = Component.text()
            kickMessages.forEach { line ->
                kickMessageBuilder.append(line).append(Component.newline())
            }
            targetPlayer.kick(kickMessageBuilder.build())
        }

        plugin.messageHandler.getSmartMessage(
            "kick",
            "kick",
            mapOf("player" to targetArg, "reason" to reason)
        ).forEach { stack.sender.sendMessage(it) }

        val broadcastMessages = plugin.messageHandler.getSmartMessage(
            "kick",
            "broadcast",
            mapOf("player" to targetArg, "reason" to reason)
        )
        plugin.server.onlinePlayers.forEach { onlinePlayer ->
            if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_KICK)) {
                broadcastMessages.forEach { onlinePlayer.sendMessage(it) }
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
