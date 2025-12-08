package pl.syntaxdevteam.punisher.platform

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler
import io.papermc.paper.threadedregions.scheduler.RegionScheduler
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.Answers
import kotlin.test.Test

class SchedulerAdapterTest {

    @Test
    fun `runSync delegates to paper scheduler when not folia`() {
        val plugin = mock<Plugin>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        val server = mock<Server>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        val scheduler = mock<BukkitScheduler>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)

        whenever(plugin.server).thenReturn(server)
        val task = mock<Runnable>()

        whenever(server.scheduler).thenReturn(scheduler)
        whenever(server.scheduler.runTask(eq(plugin), eq(task))).thenReturn(mock())

        val adapter = BukkitSchedulerAdapter(plugin, foliaBasedOverride = false)

        adapter.runSync(task)

        verify(scheduler).runTask(eq(plugin), eq(task))
    }

    @Test
    fun `runSync uses global scheduler on folia`() {
        val plugin = mock<Plugin>()
        val server = mock<Server>()
        val globalScheduler = mock<GlobalRegionScheduler>()

        whenever(plugin.server).thenReturn(server)
        whenever(server.globalRegionScheduler).thenReturn(globalScheduler)

        val adapter = BukkitSchedulerAdapter(plugin, foliaBasedOverride = true)
        val task = mock<Runnable>()

        adapter.runSync(task)

        verify(globalScheduler).execute(eq(plugin), any())
    }

    @Test
    fun `runRegionally executes region scheduler on folia`() {
        val plugin = mock<Plugin>()
        val server = mock<Server>()
        val regionScheduler = mock<RegionScheduler>()
        val location = mock<org.bukkit.Location>()

        whenever(plugin.server).thenReturn(server)
        whenever(server.regionScheduler).thenReturn(regionScheduler)

        val adapter = BukkitSchedulerAdapter(plugin, foliaBasedOverride = true)
        val task = mock<Runnable>()

        adapter.runRegionally(location, task)

        verify(regionScheduler).execute(eq(plugin), eq(location), any())
    }
}
