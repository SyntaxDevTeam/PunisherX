package pl.syntaxdevteam.punisher.basic

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.mock
import pl.syntaxdevteam.punisher.compatibility.platform.SchedulerAdapter
import pl.syntaxdevteam.punisher.teleport.SafeTeleportService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JailUtilsTest {

    private class ImmediateSchedulerAdapter : SchedulerAdapter {
        override fun runAsync(task: Runnable) = task.run()
        override fun runSync(task: Runnable) = task.run()
        override fun runSyncLater(delayTicks: Long, task: Runnable) = task.run()
        override fun runRegionally(location: Location, task: Runnable) = task.run()
        override fun isFoliaBased(): Boolean = true
    }

    @Test
    fun `getUnjailLocation on folia skips synchronous safety scan`() {
        val world = mock<World>()
        val lastLocation = Location(world, 10.0, 64.0, 10.0)
        val config = YamlConfiguration()

        val safeTeleportService = SafeTeleportService(
            scheduler = ImmediateSchedulerAdapter(),
            foliaBasedOverride = true,
            safetyEvaluator = { error("Safety evaluator should not be called on Folia path") },
            chunkLoader = {}
        )

        val result = JailUtils.getUnjailLocation(
            config = config,
            lastLocation = lastLocation,
            safeTeleportService = safeTeleportService
        )

        assertNotNull(result)
        assertEquals(lastLocation.x, result.x)
        assertEquals(lastLocation.y, result.y)
        assertEquals(lastLocation.z, result.z)
    }
}
