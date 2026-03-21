package pl.syntaxdevteam.punisher.platform

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.plugin.Plugin
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FoliaRunnableTest {

    @Test
    fun `run uses global scheduler`() {
        val scheduler = mock<GlobalRegionScheduler>()
        val plugin = mock<Plugin>()
        val scheduledTask = mock<ScheduledTask>()
        whenever(scheduler.run(eq(plugin), any())).thenReturn(scheduledTask)

        val runnable = FoliaRunnable.global(scheduler) {}
        runnable.run(plugin)

        verify(scheduler).run(eq(plugin), any())
    }

    @Test
    fun `runDelayed async forwards unit and normalized delay`() {
        val scheduler = mock<AsyncScheduler>()
        val plugin = mock<Plugin>()
        val scheduledTask = mock<ScheduledTask>()
        whenever(scheduler.runDelayed(eq(plugin), any(), eq(1L), eq(TimeUnit.SECONDS))).thenReturn(scheduledTask)

        val runnable = FoliaRunnable.async(scheduler, TimeUnit.SECONDS) {}
        runnable.runDelayed(plugin, 0)

        verify(scheduler).runDelayed(eq(plugin), any(), eq(1L), eq(TimeUnit.SECONDS))
    }

    @Test
    fun `cannot schedule same instance twice`() {
        val scheduler = mock<GlobalRegionScheduler>()
        val plugin = mock<Plugin>()
        whenever(scheduler.run(eq(plugin), any())).thenReturn(mock())

        val runnable = FoliaRunnable.global(scheduler) {}
        runnable.run(plugin)

        assertFailsWith<IllegalStateException> {
            runnable.run(plugin)
        }
    }
}
