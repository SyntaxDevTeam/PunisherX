package pl.syntaxdevteam.punisher.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.ban.BanListType
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.ban.ProfileBanList
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.common.PunishmentCommandUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.templates.PunishTemplate
import pl.syntaxdevteam.punisher.templates.PunishTemplateLevel
import java.util.Date
import java.util.Locale
import java.util.UUID

class PunishCommand(private val plugin: PunisherX) : BrigadierCommand {

    override fun execute(stack: CommandSourceStack, args: List<String>) {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISH)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (args.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("punish", "usage"))
            return
        }

        val targetName = args[0]
        val templateName = args[1]
        val template = plugin.punishTemplateManager.getTemplate(templateName)
        if (template == null) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "punish",
                    "template_not_found",
                    mapOf("template" to templateName)
                )
            )
            return
        }

        val levelArg = args.getOrNull(2)
        val requestedLevel = levelArg?.toIntOrNull()
        if (levelArg != null && requestedLevel == null) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "punish",
                    "invalid_level",
                    mapOf("level" to levelArg)
                )
            )
            return
        }

        val uuid = plugin.resolvePlayerUuid(targetName)
        val templateReason = template.reason
        val computedLevel = if (requestedLevel != null) {
            requestedLevel
        } else {
            val historyCount = plugin.databaseHandler
                .getPunishmentHistory(uuid.toString())
                .count { it.reason == templateReason }
            historyCount + 1
        }

        val templateLevel = template.resolveLevel(computedLevel)
        if (templateLevel == null) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "punish",
                    "invalid_level",
                    mapOf("level" to computedLevel.toString())
                )
            )
            return
        }

        applyTemplatePunishment(stack, targetName, uuid, template, templateLevel)
    }

    override fun suggest(stack: CommandSourceStack, args: List<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISH)) {
            return emptyList()
        }
        val current = args.lastOrNull().orEmpty()
        return when (args.size) {
            0, 1 -> plugin.server.onlinePlayers
                .map { it.name }
                .filter { it.startsWith(current, ignoreCase = true) }
            2 -> plugin.punishTemplateManager.getTemplateNames()
                .filter { it.startsWith(current, ignoreCase = true) }
            3 -> {
                val template = plugin.punishTemplateManager.getTemplate(args[1]) ?: return emptyList()
                template.levels.keys.sorted()
                    .map { it.toString() }
                    .filter { it.startsWith(current, ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    override fun build(name: String): LiteralCommandNode<CommandSourceStack> {
        val levelArg = Commands.argument("level", IntegerArgumentType.integer(1))
            .suggests(BrigadierCommandUtils.suggestions(this) { context ->
                listOf(
                    BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").firstOrNull().orEmpty(),
                    StringArgumentType.getString(context, "template"),
                    ""
                )
            })
            .executes { context ->
                val template = StringArgumentType.getString(context, "template")
                val level = IntegerArgumentType.getInteger(context, "level")
                BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                    execute(context.source, listOf(target, template, level.toString()))
                }
                1
            }

        val templateArg = Commands.argument("template", StringArgumentType.word())
            .suggests(BrigadierCommandUtils.suggestions(this) { context ->
                val target = BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").firstOrNull().orEmpty()
                listOf(target, "")
            })
            .executes { context ->
                val template = StringArgumentType.getString(context, "template")
                BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                    execute(context.source, listOf(target, template))
                }
                1
            }
            .then(levelArg)

        val targetArg = Commands.argument("target", ArgumentTypes.playerProfiles())
            .executes { context ->
                BrigadierCommandUtils.resolvePlayerProfileNames(context, "target").forEach { target ->
                    execute(context.source, listOf(target))
                }
                1
            }
            .then(templateArg)

        return Commands.literal(name)
            .requires(BrigadierCommandUtils.requiresPermission(PermissionChecker.PermissionKey.PUNISH))
            .executes { context ->
                execute(context.source, emptyList())
                1
            }
            .then(targetArg)
            .build()
    }

    private fun applyTemplatePunishment(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel
    ) {
        val type = templateLevel.type.uppercase(Locale.getDefault())
        val targetPlayer = Bukkit.getPlayer(uuid)
        if (targetPlayer != null && isBypassed(targetPlayer, type)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "error",
                    "bypass",
                    mapOf("player" to targetName)
                )
            )
            return
        }
        if (PermissionChecker.isAuthor(uuid)) {
            val prefix = plugin.messageHandler.getPrefix()
            stack.sender.sendMessage(
                plugin.messageHandler.formatMixedTextToMiniMessage(
                    "$prefix <red>You can't punish the plugin author</red>",
                    TagResolver.empty()
                )
            )
            return
        }

        when (type) {
            "BAN" -> applyBan(stack, targetName, uuid, template.reason, templateLevel)
            "MUTE" -> applyMute(stack, targetName, uuid, template.reason, templateLevel)
            "WARN" -> applyWarn(stack, targetName, uuid, template.reason, templateLevel)
            "KICK" -> applyKick(stack, targetName, uuid, template.reason, templateLevel)
            "JAIL" -> applyJail(stack, targetName, uuid, template.reason, templateLevel)
            else -> stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "punish",
                    "invalid_type",
                    mapOf("type" to type)
                )
            )
        }
    }

    private fun isBypassed(targetPlayer: org.bukkit.entity.Player, type: String): Boolean {
        return when (type) {
            "BAN" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BAN)
            "MUTE" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_MUTE)
            "WARN" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_WARN)
            "KICK" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_KICK)
            "JAIL" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_JAIL)
            else -> false
        }
    }

    private fun parseTemplateTime(stack: CommandSourceStack, time: String?): Long? {
        val trimmed = time?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            plugin.timeHandler.parseTime(trimmed)
        } catch (_: NumberFormatException) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "punish",
                    "invalid_time",
                    mapOf("time" to trimmed)
                )
            )
            null
        }
    }

    private fun applyBan(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        reason: String,
        templateLevel: PunishTemplateLevel
    ) {
        val parsedSeconds = parseTemplateTime(stack, templateLevel.time) ?: run {
            if (!templateLevel.time.isNullOrBlank()) return
            null
        }
        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "BAN"

        val punishmentId = plugin.databaseHandler.addPunishment(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
        if (punishmentId == null) {
            plugin.logger.err("Failed to add ban to database for player $targetName. Using fallback method.")
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            val playerProfile = Bukkit.createProfile(uuid, targetName)
            val banList: ProfileBanList = Bukkit.getBanList(BanListType.PROFILE)
            val banEndDate = if (parsedSeconds != null) Date(System.currentTimeMillis() + parsedSeconds * 1000) else null
            banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
        }

        plugin.databaseHandler.addPunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
        plugin.proxyBridgeMessenger.notifyBan(uuid, reason, end)

        val formattedTime = plugin.timeHandler.formatTime(templateLevel.time)
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType,
            extra = mapOf("id" to (punishmentId?.toString() ?: "?"))
        )

        val targetPlayer = Bukkit.getPlayer(uuid)
        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "ban", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "ban", "ban", placeholders)
        plugin.actionExecutor.executeAction("banned", targetName, placeholders)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_BAN, "ban", "broadcast", placeholders)
    }

    private fun applyMute(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        reason: String,
        templateLevel: PunishTemplateLevel
    ) {
        val parsedSeconds = parseTemplateTime(stack, templateLevel.time) ?: run {
            if (!templateLevel.time.isNullOrBlank()) return
            null
        }
        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "MUTE"

        val punishmentId = plugin.databaseHandler.addPunishment(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
        if (punishmentId == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            return
        }
        plugin.databaseHandler.addPunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)

        val formattedTime = plugin.timeHandler.formatTime(templateLevel.time)
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType,
            extra = mapOf("id" to punishmentId.toString())
        )

        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "mute", "mute", placeholders)
        plugin.actionExecutor.executeAction("muted", targetName, placeholders)
        PunishmentCommandUtils.sendTargetMessages(plugin, Bukkit.getPlayer(uuid), "mute", "mute_message", placeholders)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_MUTE, "mute", "broadcast", placeholders)
    }

    private fun applyWarn(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        reason: String,
        templateLevel: PunishTemplateLevel
    ) {
        val parsedSeconds = parseTemplateTime(stack, templateLevel.time) ?: run {
            if (!templateLevel.time.isNullOrBlank()) return
            null
        }
        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "WARN"

        val punishmentId = plugin.databaseHandler.addPunishment(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
        if (punishmentId == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            return
        }
        plugin.databaseHandler.addPunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)

        val warnCount = plugin.databaseHandler.getActiveWarnCount(uuid.toString())
        val formattedTime = plugin.timeHandler.formatTime(templateLevel.time)
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType,
            extra = mapOf(
                "warn_no" to warnCount.toString(),
                "id" to punishmentId.toString()
            )
        )

        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "warn", "warn", placeholders)
        PunishmentCommandUtils.sendTargetMessages(plugin, Bukkit.getPlayer(uuid), "warn", "warn_message", placeholders)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_WARN, "warn", "broadcast", placeholders)
        plugin.actionExecutor.executeWarnCountActions(targetName, warnCount)
    }

    private fun applyKick(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        reason: String,
        templateLevel: PunishTemplateLevel
    ) {
        val punishmentType = "KICK"
        val start = System.currentTimeMillis()
        val formattedTime = plugin.timeHandler.formatTime(null)
        val history = plugin.config.getBoolean("kick.history", false)
        if (history) {
            plugin.databaseHandler.addPunishmentHistory(
                targetName,
                uuid.toString(),
                reason,
                stack.sender.name,
                punishmentType,
                start,
                start
            )
        }
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType
        )

        val targetPlayer = Bukkit.getPlayer(uuid)
        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "kick", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "kick", "kick", placeholders)
        plugin.actionExecutor.executeAction("kicked", targetName, placeholders)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_KICK, "kick", "broadcast", placeholders)
    }

    private fun applyJail(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        reason: String,
        templateLevel: PunishTemplateLevel
    ) {
        val parsedSeconds = parseTemplateTime(stack, templateLevel.time) ?: run {
            if (!templateLevel.time.isNullOrBlank()) return
            null
        }
        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "JAIL"

        val jailLocation = JailUtils.getJailLocation(plugin.config)
        if (jailLocation == null) {
            plugin.logger.debug("<red>No jail location found! Teleportation aborted.</red>")
            return
        }

        val formattedTime = plugin.timeHandler.formatTime(templateLevel.time)
        val basePlaceholders = mapOf(
            "player" to targetName,
            "operator" to stack.sender.name,
            "reason" to reason,
            "time" to formattedTime,
            "type" to punishmentType
        )

        val targetPlayer = Bukkit.getPlayer(uuid)
        val previousLocation = targetPlayer?.location?.clone()

        fun finalizePunishment() {
            val punishmentId = plugin.databaseHandler.addPunishment(
                targetName,
                uuid.toString(),
                reason,
                stack.sender.name,
                punishmentType,
                start,
                end
            )
            if (punishmentId == null) {
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
                return
            }
            plugin.databaseHandler.addPunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
            plugin.cache.addOrUpdatePunishment(uuid, end, previousLocation)
            val placeholders = basePlaceholders + ("id" to punishmentId.toString())

            targetPlayer?.sendMessage(
                plugin.messageHandler.stringMessageToComponent("jail", "jail_message", placeholders)
            )

            val broadcastMessages = plugin.messageHandler.getSmartMessage("jail", "broadcast", placeholders)
            plugin.server.onlinePlayers.forEach { onlinePlayer ->
                if (PermissionChecker.hasWithSee(onlinePlayer, PermissionChecker.PermissionKey.SEE_JAIL)) {
                    broadcastMessages.forEach { message -> onlinePlayer.sendMessage(message) }
                }
            }

            plugin.messageHandler.getSmartMessage("jail", "jail", placeholders).forEach { stack.sender.sendMessage(it) }
            plugin.actionExecutor.executeAction("jailed", targetName, placeholders)
        }

        if (targetPlayer != null) {
            plugin.safeTeleportService.teleportSafely(targetPlayer, jailLocation) { success ->
                if (success) {
                    targetPlayer.gameMode = GameMode.ADVENTURE
                    finalizePunishment()
                } else {
                    stack.sender.sendMessage(
                        plugin.messageHandler.stringMessageToComponent(
                            "jail",
                            "teleport_failed",
                            mapOf("player" to targetName)
                        )
                    )
                }
            }
        } else {
            finalizePunishment()
        }
    }
}
