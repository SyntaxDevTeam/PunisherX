package pl.syntaxdevteam.punisher.compatibility.platform

import org.bukkit.Location
import org.bukkit.plugin.Plugin

interface SchedulerAdapter {
    fun runAsync(task: Runnable)
    fun runSync(task: Runnable)
    fun runSyncLater(delayTicks: Long, task: Runnable)
    fun runRegionally(location: Location, task: Runnable)
}

class BukkitSchedulerAdapter(
    private val plugin: Plugin
) : SchedulerAdapter {

    override fun runAsync(task: Runnable) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, task)
    }

    override fun runSync(task: Runnable) {
        plugin.server.scheduler.runTask(plugin, task)
    }

    override fun runSyncLater(delayTicks: Long, task: Runnable) {
        plugin.server.scheduler.runTaskLater(plugin, task, delayTicks.coerceAtLeast(1))
    }

    override fun runRegionally(location: Location, task: Runnable) {
        runSync(task)
    }
}
