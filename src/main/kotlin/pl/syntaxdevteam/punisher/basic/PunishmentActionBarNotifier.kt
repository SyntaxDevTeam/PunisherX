package pl.syntaxdevteam.punisher.basic

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import pl.syntaxdevteam.core.platform.ServerEnvironment
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.databases.PunishmentData

class PunishmentActionBarNotifier(private val plugin: PunisherX) {

    private var asyncTask: BukkitTask? = null
    private var foliaTask: ScheduledTask? = null

    fun start() {
        stop()
        if (!plugin.config.getBoolean("notifications.punishment_bar.enabled", true)) {
            return
        }

        val periodTicks = plugin.config.getLong("notifications.punishment_bar.period_ticks", DEFAULT_PERIOD_TICKS)
        if (ServerEnvironment.isFoliaBased()) {
            foliaTask = plugin.server.globalRegionScheduler.runAtFixedRate(
                plugin,
                { _ -> notifyPlayers() },
                periodTicks,
                periodTicks
            )
        } else {
            asyncTask = plugin.server.scheduler.runTaskTimerAsynchronously(
                plugin,
                Runnable { notifyPlayers() },
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

    private fun notifyPlayers() {
        val now = System.currentTimeMillis()
        val onlinePlayers = plugin.server.onlinePlayers
        if (onlinePlayers.isEmpty()) return

        onlinePlayers.forEach { player ->
            val punishments = plugin.databaseHandler.getPunishments(player.uniqueId.toString())
                .filter { plugin.punishmentManager.isPunishmentActive(it) }

            val actionBarMessage = buildActionBar(punishments, now) ?: return@forEach
            sendActionBar(player, actionBarMessage)
        }
    }

    private fun sendActionBar(player: Player, message: Component) {
        plugin.schedulerAdapter.runRegionally(player.location) {
            if (player.isOnline) {
                player.sendActionBar(message)
            }
        }
    }

    private fun buildActionBar(punishments: List<PunishmentData>, now: Long): Component? {
        val components = mutableListOf<Component>()

        getRemainingTimeComponent(punishments, "JAIL", "jail_remaining_time", now)?.let { components.add(it) }
        getRemainingTimeComponent(punishments, "MUTE", "mute_remaining_time", now)?.let { components.add(it) }

        if (components.isEmpty()) {
            return null
        }

        return components.reduce { acc, component ->
            acc.append(SEPARATOR_COMPONENT).append(component)
        }
    }

    private fun getRemainingTimeComponent(
        punishments: List<PunishmentData>,
        type: String,
        messageKey: String,
        now: Long
    ): Component? {
        val punishment = punishments.firstOrNull { it.type.equals(type, ignoreCase = true) } ?: return null
        val endTime = punishment.end
        val remainingSeconds = if (endTime == -1L) null else ((endTime - now) / 1000).coerceAtLeast(0)
        val timeText = remainingSeconds?.let { plugin.timeHandler.formatTime(it.toString()) } ?: PERMANENT_LABEL

        val baseMessage = plugin.messageHandler.stringMessageToComponentNoPrefix("placeholders", messageKey)
        val timeComponent = Component.text(timeText, NamedTextColor.GOLD)
        return baseMessage.append(timeComponent)
    }

    companion object {
        private const val DEFAULT_PERIOD_TICKS = 20L
        private const val PERMANENT_LABEL = "permanent"
        private val SEPARATOR_COMPONENT = Component.text(" | ", NamedTextColor.DARK_GRAY)
    }
}
