package pl.syntaxdevteam.punisher.platform

import org.bukkit.Location
import org.bukkit.plugin.Plugin
import pl.syntaxdevteam.core.platform.ServerEnvironment

interface SchedulerAdapter {
    fun runAsync(task: Runnable)
    fun runSync(task: Runnable)
    fun runSyncLater(delayTicks: Long, task: Runnable)
    fun runRegionally(location: Location, task: Runnable)
}

class BukkitSchedulerAdapter(
    private val plugin: Plugin,
    foliaBasedOverride: Boolean? = null
) : SchedulerAdapter {

    private val foliaBased: Boolean = foliaBasedOverride ?: ServerEnvironment.isFoliaBased()

    override fun runAsync(task: Runnable) {
        if (foliaBased) {
            plugin.server.globalRegionScheduler.execute(plugin, task)
        } else {
            plugin.server.scheduler.runTaskAsynchronously(plugin, task)
        }
    }

    override fun runSync(task: Runnable) {
        if (foliaBased) {
            plugin.server.globalRegionScheduler.execute(plugin) { task.run() }
        } else {
            plugin.server.scheduler.runTask(plugin, task)
        }
    }

    override fun runSyncLater(delayTicks: Long, task: Runnable) {
        if (foliaBased) {
            plugin.server.globalRegionScheduler.runDelayed(plugin, { task.run() }, delayTicks)
        } else {
            plugin.server.scheduler.runTaskLater(plugin, task, delayTicks)
        }
    }

    override fun runRegionally(location: Location, task: Runnable) {
        if (foliaBased) {
            plugin.server.regionScheduler.execute(plugin, location, task)
        } else {
            runSync(task)
        }
    }
}
