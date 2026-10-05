package pl.syntaxdevteam.punisher.gui

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.JailUtils
import pl.syntaxdevteam.punisher.compatibility.*
import pl.syntaxdevteam.punisher.databases.PunishmentData
import pl.syntaxdevteam.punisher.permissions.PermissionChecker
import java.util.UUID

class GuiPunishmentService(private val plugin: PunisherX) {
    private fun permission(type: String) = when (type.uppercase()) {
        "BAN" -> PermissionChecker.PermissionKey.BAN
        "JAIL" -> PermissionChecker.PermissionKey.JAIL
        "MUTE" -> PermissionChecker.PermissionKey.MUTE
        "WARN" -> PermissionChecker.PermissionKey.WARN
        else -> null
    }

    fun apply(operator: Player, target: OfflinePlayer, type: String, time: String, reason: String) {
        val permission = permission(type) ?: return
        if (!PermissionChecker.hasWithLegacy(operator, permission)) {
            operator.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission")); return
        }
        val online = target.player
        val bypass = when (type.uppercase()) {
            "BAN" -> PermissionChecker.PermissionKey.BYPASS_BAN
            "JAIL" -> PermissionChecker.PermissionKey.BYPASS_JAIL
            "MUTE" -> PermissionChecker.PermissionKey.BYPASS_MUTE
            "WARN" -> PermissionChecker.PermissionKey.BYPASS_WARN
            else -> null
        }
        if (!plugin.config.getBoolean("gui.punish.use_force", false) && online != null && bypass != null && PermissionChecker.hasWithLegacy(online, bypass)) {
            operator.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "bypass", mapOf("player" to (target.name ?: target.uniqueId.toString())))); return
        }
        val name = target.name ?: target.uniqueId.toString()
        val start = System.currentTimeMillis()
        val end = if (time.equals("perm", true)) -1L else start + plugin.timeHandler.parseTime(time) * 1000

        fun persist(previousLocation: Location? = null) {
            plugin.schedulerAdapter.runAsync(Runnable {
                val id = plugin.databaseHandler.addPunishment(name, target.uniqueId.toString(), reason, operator.name, type.uppercase(), start, end)
                if (id != null) plugin.databaseHandler.addPunishmentHistory(name, target.uniqueId.toString(), reason, operator.name, type.uppercase(), start, end)
                if (id != null && type.equals("JAIL", true)) plugin.cache.addOrUpdatePunishment(target.uniqueId, end, previousLocation)
                val warnCount = if (id != null && type.equals("WARN", true)) plugin.databaseHandler.getActiveWarnCount(target.uniqueId.toString()) else 0
                plugin.schedulerAdapter.runForPlayer(operator, Runnable {
                    if (id == null) {
                        operator.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error")); return@Runnable
                    }
                    val placeholders = mapOf("player" to name, "operator" to operator.name, "reason" to reason,
                        "time" to plugin.timeHandler.formatTime(time), "type" to type.uppercase(), "id" to id.toString())
                    plugin.commandLoggerPlugin.logCommand(operator.name, type.uppercase(), name, reason)
                    plugin.messageHandler.getSmartMessage(type.lowercase(), type.lowercase(), placeholders).forEach(operator::sendMessage)
                    val actionKey = mapOf("BAN" to "banned", "JAIL" to "jailed", "MUTE" to "muted", "WARN" to "warned")[type.uppercase()] ?: type.lowercase()
                    plugin.actionExecutor.executeAction(actionKey, name, placeholders)
                    if (type.equals("WARN", true)) plugin.actionExecutor.executeWarnCountActions(name, warnCount)
                    val seePermission = mapOf(
                        "BAN" to PermissionChecker.PermissionKey.SEE_BAN, "JAIL" to PermissionChecker.PermissionKey.SEE_JAIL,
                        "MUTE" to PermissionChecker.PermissionKey.SEE_MUTE, "WARN" to PermissionChecker.PermissionKey.SEE_WARN
                    )[type.uppercase()]
                    if (seePermission != null) {
                        val broadcast = plugin.messageHandler.getSmartMessage(type.lowercase(), "broadcast", placeholders)
                        plugin.server.onlinePlayers.filter { it.uniqueId != operator.uniqueId && PermissionChecker.hasWithSee(it, seePermission) }
                            .forEach { viewer -> plugin.schedulerAdapter.runForPlayer(viewer, Runnable { broadcast.forEach(viewer::sendMessage) }) }
                    }
                    if (type.equals("BAN", true)) {
                        plugin.proxyBridgeMessenger.notifyBan(target.uniqueId, reason, end)
                        online?.let { victim -> plugin.schedulerAdapter.runForPlayer(victim, Runnable {
                            victim.kick(plugin.messageHandler.stringMessageToComponent("ban", "kick_message", placeholders))
                        }) }
                    } else online?.let { victim -> plugin.schedulerAdapter.runForPlayer(victim, Runnable {
                        plugin.messageHandler.getSmartMessage(type.lowercase(), "${type.lowercase()}_message", placeholders).forEach(victim::sendMessage)
                    }) }
                })
            })
        }

        if (type.equals("JAIL", true) && online != null) {
            val jail = JailUtils.getJailLocation(plugin.config)
            if (jail == null) {
                operator.sendMessage(plugin.messageHandler.stringMessageToComponent("setjail", "set_error")); return
            }
            val previous = online.location.clone()
            plugin.safeTeleportService.teleportSafely(online, jail) { success ->
                if (success) {
                    online.gameMode = GameMode.ADVENTURE
                    persist(previous)
                }
            }
        } else persist()
    }

    fun revoke(operator: Player, punishment: PunishmentData, onComplete: () -> Unit = {}) {
        val permission = when (punishment.type.uppercase()) {
            "BAN", "BANIP" -> PermissionChecker.PermissionKey.UNBAN
            "JAIL" -> PermissionChecker.PermissionKey.UNJAIL
            "MUTE" -> PermissionChecker.PermissionKey.UNMUTE
            "WARN" -> PermissionChecker.PermissionKey.UNWARN
            else -> PermissionChecker.PermissionKey.CLEAR_ALL
        }
        if (!PermissionChecker.hasWithLegacy(operator, permission)) {
            operator.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission")); return
        }
        plugin.schedulerAdapter.runAsync(Runnable {
            val removed = plugin.databaseHandler.removePunishmentById(punishment.id)
            val remainingJails = if (removed && punishment.type.equals("JAIL", true))
                plugin.databaseHandler.getPunishments(punishment.uuid).any { it.type.equals("JAIL", true) } else true
            plugin.schedulerAdapter.runForPlayer(operator, Runnable {
                if (!removed) {
                    operator.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error")); return@Runnable
                }
                if (punishment.type.equals("JAIL", true) && !remainingJails) runCatching {
                    plugin.cache.removePunishment(UUID.fromString(punishment.uuid), removeDatabase = false)
                }
                operator.sendMessage(plugin.messageHandler.miniMessageFormat("<green>Removed ${punishment.type} #${punishment.id} from ${punishment.name}.</green>"))
                plugin.logger.info("${operator.name} removed ${punishment.type} #${punishment.id} from ${punishment.name} using GUI")
                onComplete()
            })
        })
    }

    fun banIp(operator: Player, target: OfflinePlayer, reason: String) {
        if (!PermissionChecker.hasWithLegacy(operator, PermissionChecker.PermissionKey.BANIP)) {
            operator.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "no_permission")); return
        }
        val name = target.name ?: target.uniqueId.toString()
        plugin.schedulerAdapter.runAsync(Runnable {
            val ips = plugin.playerIPManager.getPlayerIPsByUUID(target.uniqueId.toString()).distinct()
            val start = System.currentTimeMillis()
            val success = ips.isNotEmpty() && ips.all { ip ->
                val id = plugin.databaseHandler.addPunishment(name, ip, reason, operator.name, "BANIP", start, -1)
                if (id != null) plugin.databaseHandler.addPunishmentHistory(name, ip, reason, operator.name, "BANIP", start, -1)
                id != null
            }
            plugin.schedulerAdapter.runForPlayer(operator, Runnable {
                if (!success) operator.sendMessage(plugin.messageHandler.stringMessageToComponent("error", "db_error")) else {
                    ips.forEach { plugin.proxyBridgeMessenger.notifyIpBan(it, reason, -1) }
                    operator.sendMessage(plugin.messageHandler.miniMessageFormat("<green>IP ban applied to $name.</green>"))
                    target.player?.let { victim -> plugin.schedulerAdapter.runForPlayer(victim, Runnable {
                        victim.kick(plugin.messageHandler.stringMessageToComponent("banip", "kick_message",
                            mapOf("player" to name, "operator" to operator.name, "reason" to reason, "time" to "permanent", "type" to "BANIP", "id" to "?")))
                    }) }
                }
            })
        })
    }
}
