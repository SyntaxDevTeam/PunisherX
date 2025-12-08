package pl.syntaxdevteam.punisher.core.platform

/**
 * Platform scheduler abstraction used by core services.
 */
interface SchedulerAdapter {
    fun runAsync(task: Runnable)
    fun runSync(task: Runnable)
    fun runSyncLater(delayTicks: Long, task: Runnable)
    fun runRegionally(anchor: Any, task: Runnable)
}
