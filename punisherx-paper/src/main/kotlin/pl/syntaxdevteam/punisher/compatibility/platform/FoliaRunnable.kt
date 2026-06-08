package pl.syntaxdevteam.punisher.compatibility.platform

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler
import io.papermc.paper.threadedregions.scheduler.EntityScheduler
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler
import io.papermc.paper.threadedregions.scheduler.RegionScheduler
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

/**
 * Kotlinowa wersja wielokontekstowego zadania dla Folii.
 *
 * Jedna instancja może zostać zaplanowana tylko raz.
 */
abstract class FoliaRunnable private constructor(
    private val context: Context
) : Runnable {

    private var task: ScheduledTask? = null

    fun isCancelled(): Boolean {
        val scheduledTask = requireTask()
        return scheduledTask.isCancelled
    }

    fun cancel() {
        val scheduledTask = requireTask()
        scheduledTask.cancel()
    }

    fun run(plugin: Plugin): ScheduledTask {
        requireNotNull(plugin) { "Plugin cannot be null." }
        checkNotYetScheduled()

        val scheduledTask = when (val ctx = context) {
            is Context.Global -> ctx.scheduler.run(plugin) { run() }
            is Context.Entity -> ctx.scheduler.run(plugin, { run() }, ctx.retired)
            is Context.RegionByLocation -> ctx.scheduler.run(plugin, ctx.location) { run() }
            is Context.RegionByChunk -> ctx.scheduler.run(plugin, ctx.world, ctx.chunkX, ctx.chunkZ) { run() }
            is Context.Async -> ctx.scheduler.runNow(plugin) { run() }
        }

        return register(scheduledTask)
    }

    fun runDelayed(plugin: Plugin, delay: Long): ScheduledTask {
        requireNotNull(plugin) { "Plugin cannot be null." }
        checkNotYetScheduled()

        val safeDelay = delay.coerceAtLeast(1)

        val scheduledTask = when (val ctx = context) {
            is Context.Global -> ctx.scheduler.runDelayed(plugin, { run() }, safeDelay)
            is Context.Entity -> ctx.scheduler.runDelayed(plugin, { run() }, ctx.retired, safeDelay)
            is Context.RegionByLocation -> ctx.scheduler.runDelayed(plugin, ctx.location, { run() }, safeDelay)
            is Context.RegionByChunk -> ctx.scheduler.runDelayed(plugin, ctx.world, ctx.chunkX, ctx.chunkZ, { run() }, safeDelay)
            is Context.Async -> ctx.scheduler.runDelayed(plugin, { run() }, safeDelay, ctx.timeUnit)
        }

        return register(scheduledTask)
    }

    fun runAtFixedRate(plugin: Plugin, delay: Long, period: Long): ScheduledTask {
        requireNotNull(plugin) { "Plugin cannot be null." }
        checkNotYetScheduled()

        val safeDelay = delay.coerceAtLeast(1)
        val safePeriod = period.coerceAtLeast(1)

        val scheduledTask = when (val ctx = context) {
            is Context.Global -> ctx.scheduler.runAtFixedRate(plugin, { run() }, safeDelay, safePeriod)
            is Context.Entity -> ctx.scheduler.runAtFixedRate(plugin, { run() }, ctx.retired, safeDelay, safePeriod)
            is Context.RegionByLocation -> ctx.scheduler.runAtFixedRate(plugin, ctx.location, { run() }, safeDelay, safePeriod)
            is Context.RegionByChunk -> ctx.scheduler.runAtFixedRate(plugin, ctx.world, ctx.chunkX, ctx.chunkZ, { run() }, safeDelay, safePeriod)
            is Context.Async -> ctx.scheduler.runAtFixedRate(plugin, { run() }, safeDelay, safePeriod, ctx.timeUnit)
        }

        return register(scheduledTask)
    }

    fun getTaskId(): Int = requireTask().hashCode()

    private fun requireTask(): ScheduledTask {
        return task ?: throw IllegalStateException("This FoliaRunnable has not been scheduled yet.")
    }

    private fun checkNotYetScheduled() {
        if (task != null) {
            throw IllegalStateException("This FoliaRunnable has already been scheduled as task ID: ${task.hashCode()}")
        }
    }

    private fun register(scheduledTask: ScheduledTask?): ScheduledTask {
        val resolvedTask = requireNotNull(scheduledTask) { "Scheduler returned null task." }
        task = resolvedTask
        return resolvedTask
    }

    companion object {
        fun async(scheduler: AsyncScheduler, timeUnit: TimeUnit, block: () -> Unit): FoliaRunnable {
            return object : FoliaRunnable(Context.Async(scheduler, timeUnit)) {
                override fun run() = block()
            }
        }

        fun entity(scheduler: EntityScheduler, retired: Runnable? = null, block: () -> Unit): FoliaRunnable {
            return object : FoliaRunnable(Context.Entity(scheduler, retired)) {
                override fun run() = block()
            }
        }

        fun global(scheduler: GlobalRegionScheduler, block: () -> Unit): FoliaRunnable {
            return object : FoliaRunnable(Context.Global(scheduler)) {
                override fun run() = block()
            }
        }

        fun region(scheduler: RegionScheduler, location: Location, block: () -> Unit): FoliaRunnable {
            return object : FoliaRunnable(Context.RegionByLocation(scheduler, location)) {
                override fun run() = block()
            }
        }

        fun region(scheduler: RegionScheduler, world: World, chunkX: Int, chunkZ: Int, block: () -> Unit): FoliaRunnable {
            return object : FoliaRunnable(Context.RegionByChunk(scheduler, world, chunkX, chunkZ)) {
                override fun run() = block()
            }
        }
    }

    private sealed interface Context {
        data class Async(val scheduler: AsyncScheduler, val timeUnit: TimeUnit) : Context
        data class Entity(val scheduler: EntityScheduler, val retired: Runnable?) : Context
        data class Global(val scheduler: GlobalRegionScheduler) : Context
        data class RegionByLocation(val scheduler: RegionScheduler, val location: Location) : Context
        data class RegionByChunk(val scheduler: RegionScheduler, val world: World, val chunkX: Int, val chunkZ: Int) : Context
    }
}
