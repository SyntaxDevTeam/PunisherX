package pl.syntaxdevteam.punisher.teleport

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SafeTeleportServiceTest {

    private class ImmediateSchedulerAdapter : pl.syntaxdevteam.punisher.platform.SchedulerAdapter {
        override fun runAsync(task: Runnable) = task.run()
        override fun runSync(task: Runnable) = task.run()
        override fun runSyncLater(delayTicks: Long, task: Runnable) = task.run()
        override fun runRegionally(location: Location, task: Runnable) = task.run()
        override fun isFoliaBased(): Boolean = false
    }

    @Test
    fun `findNearestSafeLocation falls back to closest safe position`() {
        val plugin = mock<Plugin>()
        val world = mock<World>()
        val base = Location(world, 0.0, 64.0, 0.0)

        val service = SafeTeleportService(
            ImmediateSchedulerAdapter(),
            foliaBasedOverride = false,
            safetyEvaluator = { location ->
                location.blockX == 1 && location.blockY == 64 && location.blockZ == 0
            },
            chunkLoader = {}
        )

        val result = service.findNearestSafeLocation(base, horizontalRange = 2, verticalRange = 1)

        assertNotNull(result)
        assertEquals(1, result.blockX)
        assertEquals(0, result.blockZ)
    }

    @Test
    fun `findNearestSafeLocation returns null when folia chunk not loaded`() {
        val world = mock<World>()
        whenever(world.isChunkLoaded(0, 0)).thenReturn(false)

        val service = SafeTeleportService(
            ImmediateSchedulerAdapter(),
            foliaBasedOverride = true,
            safetyEvaluator = {
                error("safetyEvaluator should not be called")
            },
            chunkLoader = {}
        )

        val result = service.findNearestSafeLocation(Location(world, 0.0, 64.0, 0.0))

        assertNull(result)
    }
}
