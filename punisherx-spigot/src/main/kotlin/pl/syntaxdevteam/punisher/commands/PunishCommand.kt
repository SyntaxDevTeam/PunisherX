package pl.syntaxdevteam.punisher.commands
import pl.syntaxdevteam.punisher.compatibility.*

import org.bukkit.Bukkit
import org.bukkit.BanList
import org.bukkit.GameMode
import org.bukkit.ban.ProfileBanList
import org.jetbrains.annotations.NotNull
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import pl.syntaxdevteam.punisher.templates.PunishTemplate
import pl.syntaxdevteam.punisher.templates.PunishTemplateLevel
import java.util.Date
import java.util.Locale
import java.util.UUID

class PunishCommand(private val plugin: PunisherX) : BasicCommand {
    private data class TemplateTime(val seconds: Long?)

    override fun execute(@NotNull stack: CommandSourceStack, @NotNull args: Array<String>) {
        val options = PunishmentCommandOptions.parse(PunishCommandCompletion.arguments(args))
        val arguments = options.arguments
        val silent = options.silent
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISH)) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission"))
            return
        }
        if (arguments.size < 2) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("punish", "usage"))
            return
        }

        val targetName = arguments[0]
        val templateName = arguments[1]
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

        val levelArg = arguments.getOrNull(2)
        val requestedLevel = levelArg?.toIntOrNull()
        if (arguments.size > 3 || (levelArg != null && (requestedLevel == null || requestedLevel < 1))) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "punish",
                    "invalid_level",
                    mapOf("level" to levelArg.orEmpty())
                )
            )
            return
        }

        val uuid = plugin.resolvePlayerUuid(targetName)
        val templateReason = template.reason
        val computedLevel = if (requestedLevel != null) {
            requestedLevel
        } else {
            try {
                plugin.databaseHandler.countEscalationHistory(uuid.toString(), targetName, templateReason, template.name) + 1
            } catch (error: Exception) {
                plugin.logger.err("Failed to count punishment escalation for $targetName. ${error.message}")
                stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("punish", "history_error"))
                return
            }
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

        applyTemplatePunishment(stack, targetName, uuid, template, templateLevel, options.force, silent)
    }


    override fun suggest(stack: CommandSourceStack, args: Array<String>): List<String> {
        if (!PermissionChecker.hasWithLegacy(stack.sender, PermissionChecker.PermissionKey.PUNISH)) {
            return emptyList()
        }
        return PunishCommandCompletion.suggest(
            args,
            plugin.server.onlinePlayers.map { it.name },
            plugin.punishTemplateManager.getTemplateNames()
        ) { name -> plugin.punishTemplateManager.getTemplate(name)?.levels?.keys.orEmpty() }
    }

    private fun applyTemplatePunishment(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel,
        force: Boolean,
        silent: Boolean
    ) {
        val type = templateLevel.type.uppercase(Locale.getDefault())
        val targetPlayer = Bukkit.getPlayer(uuid)
        if (!force && targetPlayer != null && isBypassed(targetPlayer, type)) {
            stack.sender.sendMessage(
                plugin.messageHandler.stringMessageToComponent(
                    "error",
                    "bypass",
                    mapOf("player" to targetName)
                )
            )
            return
        }
        when (type) {
            "BAN" -> applyBan(stack, targetName, uuid, template, templateLevel, silent)
            "BANIP" -> applyBanIp(stack, targetName, uuid, template, templateLevel, silent)
            "MUTE" -> applyMute(stack, targetName, uuid, template, templateLevel, silent)
            "WARN" -> applyWarn(stack, targetName, uuid, template, templateLevel, silent)
            "KICK" -> applyKick(stack, targetName, uuid, template, templateLevel, silent)
            "JAIL" -> applyJail(stack, targetName, uuid, template, templateLevel, silent)
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
            "BANIP" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_BANIP)
            "MUTE" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_MUTE)
            "WARN" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_WARN)
            "KICK" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_KICK)
            "JAIL" -> PermissionChecker.hasWithBypass(targetPlayer, PermissionChecker.PermissionKey.BYPASS_JAIL)
            else -> false
        }
    }

    private fun parseTemplateTime(stack: CommandSourceStack, time: String?): TemplateTime? {
        val trimmed = time?.trim()?.takeIf { it.isNotEmpty() } ?: return TemplateTime(null)
        if (trimmed.equals("permanent", ignoreCase = true)) {
            return TemplateTime(null)
        }

        return try {
            TemplateTime(plugin.timeHandler.parseTime(trimmed))
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
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel,
        silent: Boolean
    ) {
        val reason = template.reason
        val parsedTemplateTime = parseTemplateTime(stack, templateLevel.time) ?: return
        val parsedSeconds = parsedTemplateTime.seconds
        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "BAN"

        val punishmentId = plugin.databaseHandler.addPunishment(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
        if (punishmentId == null) {
            plugin.logger.err("Failed to add ban to database for player $targetName. Using fallback method.")
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            val playerProfile = Bukkit.createPlayerProfile(uuid, targetName)
            val banList: ProfileBanList = Bukkit.getBanList(BanList.Type.PROFILE)
            val banEndDate = if (parsedSeconds != null) Date(System.currentTimeMillis() + parsedSeconds * 1000) else null
            banList.addBan(playerProfile, reason, banEndDate, stack.sender.name)
        }

        plugin.databaseHandler.addTemplatePunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end, templateName = template.name, templateUuid = uuid.toString())
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
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "ban", "ban", placeholders, silent = silent)
        plugin.actionExecutor.executeAction("banned", targetName, placeholders, silent = silent)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_BAN, "ban", "broadcast", placeholders, silent = silent)
    }

    private fun applyBanIp(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel,
        silent: Boolean
    ) {
        val reason = template.reason
        val parsedTemplateTime = parseTemplateTime(stack, templateLevel.time) ?: return
        val parsedSeconds = parsedTemplateTime.seconds
        val playerIps = (
            plugin.playerIPManager.getPlayerIPsByUUID(uuid.toString()) +
                plugin.playerIPManager.getPlayerIPsByName(targetName.lowercase())
            ).distinct()

        if (playerIps.isEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("banip", "not_found"))
            return
        }

        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "BANIP"
        val punishmentIds = mutableListOf<Long>()
        val failedIps = mutableListOf<String>()
        val bannedIps = mutableListOf<String>()

        playerIps.forEach { ip ->
            val punishmentId = plugin.databaseHandler.addPunishment(targetName, ip, reason, stack.sender.name, punishmentType, start, end)
            if (punishmentId == null) {
                plugin.logger.err("Failed to add IP ban to database for $ip.")
                failedIps.add(ip)
                return@forEach
            }
            punishmentIds.add(punishmentId)
            bannedIps.add(ip)
            plugin.databaseHandler.addTemplatePunishmentHistory(targetName, ip, reason, stack.sender.name, punishmentType, start, end, templateName = template.name, templateUuid = uuid.toString())
            plugin.proxyBridgeMessenger.notifyIpBan(ip, reason, end)
        }

        if (failedIps.isNotEmpty()) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent(
                "error", "banip_db_error", mapOf(
                    "failed_ips" to failedIps.joinToString(", "),
                    "banned_ips" to bannedIps.joinToString(", ").ifEmpty { "-" }
                )))
            return
        }

        val formattedTime = plugin.timeHandler.formatTime(templateLevel.time)
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType,
            extra = mapOf(
                "id" to when {
                    punishmentIds.isEmpty() -> "?"
                    punishmentIds.size == 1 -> punishmentIds.first().toString()
                    else -> punishmentIds.joinToString(",")
                }
            )
        )

        val targetPlayer = Bukkit.getPlayer(uuid)
        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "banip", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "banip", "ban", placeholders, silent = silent)
        plugin.actionExecutor.executeAction("ip_banned", targetName, placeholders, silent = silent)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_BANIP, "banip", "ban", placeholders, silent = silent)
    }

    private fun applyMute(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel,
        silent: Boolean
    ) {
        val reason = template.reason
        val parsedTemplateTime = parseTemplateTime(stack, templateLevel.time) ?: return
        val parsedSeconds = parsedTemplateTime.seconds
        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "MUTE"

        val punishmentId = plugin.databaseHandler.addPunishment(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
        if (punishmentId == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            return
        }
        plugin.databaseHandler.addTemplatePunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end, templateName = template.name, templateUuid = uuid.toString())

        val formattedTime = plugin.timeHandler.formatTime(templateLevel.time)
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType,
            extra = mapOf("id" to punishmentId.toString())
        )

        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "mute", "mute", placeholders, silent = silent)
        plugin.actionExecutor.executeAction("muted", targetName, placeholders, silent = silent)
        PunishmentCommandUtils.sendTargetMessages(plugin, Bukkit.getPlayer(uuid), "mute", "mute_message", placeholders, silent = silent)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_MUTE, "mute", "broadcast", placeholders, silent = silent)
    }

    private fun applyWarn(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel,
        silent: Boolean
    ) {
        val reason = template.reason
        val parsedTemplateTime = parseTemplateTime(stack, templateLevel.time) ?: return
        val parsedSeconds = parsedTemplateTime.seconds
        val start = System.currentTimeMillis()
        val end = parsedSeconds?.let { start + it * 1000 } ?: -1
        val punishmentType = "WARN"

        val punishmentId = plugin.databaseHandler.addPunishment(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end)
        if (punishmentId == null) {
            stack.sender.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error"))
            return
        }
        plugin.databaseHandler.addTemplatePunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end, templateName = template.name, templateUuid = uuid.toString())

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

        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "warn", "warn", placeholders, silent = silent)
        PunishmentCommandUtils.sendTargetMessages(plugin, Bukkit.getPlayer(uuid), "warn", "warn_message", placeholders, silent = silent)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_WARN, "warn", "broadcast", placeholders, silent = silent)
        plugin.actionExecutor.executeWarnCountActions(targetName, warnCount, silent = silent)
    }

    private fun applyKick(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel,
        silent: Boolean
    ) {
        val reason = template.reason
        val punishmentType = "KICK"
        val start = System.currentTimeMillis()
        val formattedTime = plugin.timeHandler.formatTime(null)
        // Template kicks always contribute to escalation, even if direct /kick history is disabled.
        plugin.databaseHandler.addTemplatePunishmentHistory(
            targetName,
            uuid.toString(),
            reason,
            stack.sender.name,
            punishmentType,
            start,
            start,
            templateName = template.name,
            templateUuid = uuid.toString()
        )
        val placeholders = PunishmentCommandUtils.buildPlaceholders(
            player = targetName,
            operator = stack.sender.name,
            reason = reason,
            time = formattedTime,
            type = punishmentType
        )

        val targetPlayer = Bukkit.getPlayer(uuid)
        PunishmentCommandUtils.sendKickMessage(plugin, targetPlayer, "kick", "kick_message", placeholders)
        PunishmentCommandUtils.sendSenderMessages(plugin, stack, "kick", "kick", placeholders, silent = silent)
        plugin.actionExecutor.executeAction("kicked", targetName, placeholders, silent = silent)
        PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_KICK, "kick", "broadcast", placeholders, silent = silent)
    }

    private fun applyJail(
        stack: CommandSourceStack,
        targetName: String,
        uuid: UUID,
        template: PunishTemplate,
        templateLevel: PunishTemplateLevel,
        silent: Boolean
    ) {
        val reason = template.reason
        val parsedTemplateTime = parseTemplateTime(stack, templateLevel.time) ?: return
        val parsedSeconds = parsedTemplateTime.seconds
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
            plugin.databaseHandler.addTemplatePunishmentHistory(targetName, uuid.toString(), reason, stack.sender.name, punishmentType, start, end, templateName = template.name, templateUuid = uuid.toString())
            plugin.cache.addOrUpdatePunishment(uuid, end, previousLocation)
            val placeholders = basePlaceholders + ("id" to punishmentId.toString())

            PunishmentCommandUtils.sendTargetMessages(plugin, targetPlayer, "jail", "jail_message", placeholders, silent = silent)
            PunishmentCommandUtils.sendBroadcast(plugin, PermissionChecker.PermissionKey.SEE_JAIL, "jail", "broadcast", placeholders, silent = silent)
            PunishmentCommandUtils.sendSenderMessages(plugin, stack, "jail", "jail", placeholders, silent = silent)
            plugin.actionExecutor.executeAction("jailed", targetName, placeholders, silent = silent)
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
