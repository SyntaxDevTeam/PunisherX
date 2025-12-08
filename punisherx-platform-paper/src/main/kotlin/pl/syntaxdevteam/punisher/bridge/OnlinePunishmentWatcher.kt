package pl.syntaxdevteam.punisher.bridge

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import pl.syntaxdevteam.core.platform.ServerEnvironment
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData
import java.util.concurrent.TimeUnit

class OnlinePunishmentWatcher(private val plugin: PunisherX) {

    private var asyncTask: BukkitTask? = null
    private var foliaTask: ScheduledTask? = null

    fun start() {
        stop()
        val periodTicks = plugin.config.getLong("bridge.watchdog_period_ticks", DEFAULT_PERIOD_TICKS)
        if (ServerEnvironment.isFoliaBased()) {
            foliaTask = plugin.server.globalRegionScheduler.runAtFixedRate(
                plugin,
                { _ -> checkOnlinePlayers() },
                periodTicks,
                periodTicks
            )
        } else {
            asyncTask = plugin.server.scheduler.runTaskTimerAsynchronously(
                plugin,
                Runnable { checkOnlinePlayers() },
                periodTicks,
                periodTicks
            )
        }
    }

    fun stop() {
        asyncTask?.cancel()
        asyncTask = null
        foliaTask?.cancel()
        foliaTask = null
    }

    private fun checkOnlinePlayers() {
        val onlinePlayers = plugin.server.onlinePlayers
        if (onlinePlayers.isEmpty()) return

        onlinePlayers.forEach { player ->
            val activeBan = findActiveBan(player) ?: return@forEach
            kickPlayer(player, activeBan)
        }
    }

    private fun findActiveBan(player: Player): PunishmentData? {
        val uuid = player.uniqueId.toString()
        val ip = player.address?.address?.hostAddress

        val punishments = plugin.databaseHandler.getPunishments(uuid)
            .filter { plugin.punishmentManager.isPunishmentActive(it) }
        val ipPunishments = ip?.let { plugin.databaseHandler.getPunishmentsByIP(it) }?.filter {
            plugin.punishmentManager.isPunishmentActive(it)
        } ?: emptyList()

        return (punishments + ipPunishments)
            .firstOrNull { it.type.equals("BAN", ignoreCase = true) || it.type.equals("BANIP", ignoreCase = true) }
    }

    private fun kickPlayer(player: Player, punishment: PunishmentData) {
        val remainingSeconds = if (punishment.end >= 0) {
            TimeUnit.MILLISECONDS.toSeconds((punishment.end - System.currentTimeMillis()).coerceAtLeast(0))
                .toString()
        } else {
            null
        }

        val kickMessages = plugin.messageHandler.getSmartMessage(
            "ban",
            "kick_message",
            mapOf(
                "reason" to punishment.reason,
                "time" to plugin.timeHandler.formatTime(remainingSeconds)
            )
        )

        val kickMessage = Component.text()
        kickMessages.forEachIndexed { index, component ->
            kickMessage.append(component)
            if (index != kickMessages.lastIndex) {
                kickMessage.append(Component.newline())
            }
        }

        plugin.schedulerAdapter.runSync { player.kick(kickMessage.build()) }
    }

    companion object {
        private const val DEFAULT_PERIOD_TICKS = 20L
    }
}
