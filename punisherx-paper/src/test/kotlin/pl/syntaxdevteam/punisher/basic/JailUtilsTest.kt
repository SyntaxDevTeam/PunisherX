package pl.syntaxdevteam.punisher.basic

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
    fun `setJailLocation stores namespaced world key and legacy world name`() {
        val world = worldMock(name = "world", key = NamespacedKey.minecraft("overworld"))
        val location = Location(world, 10.0, 64.0, 10.0)
        val config = YamlConfiguration()

        JailUtils.setJailLocation(config, location, radius = 5.0)

        assertEquals("world", config.getString("jail.location.world"))
        assertEquals("minecraft:overworld", config.getString("jail.location.world_key"))
    }

    @Test
    fun `stored jail cache location keeps namespaced world key and legacy world name`() {
        val world = worldMock(name = "custom_world", key = NamespacedKey("custom", "dimension"))
        val location = Location(world, 1.0, 2.0, 3.0, 90.0f, 45.0f)

        val stored = PunishmentCache.StoredLocation.fromLocation(location)

        assertEquals("custom_world", stored.world)
        assertEquals("custom:dimension", stored.worldKey)
        assertEquals(1.0, stored.x)
        assertEquals(2.0, stored.y)
        assertEquals(3.0, stored.z)
    }

    @Test
    fun `getUnjailLocation on folia skips synchronous safety scan`() {
        val world = worldMock()
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

    private fun worldMock(
        name: String = "world",
        key: NamespacedKey = NamespacedKey.minecraft("overworld")
    ): World {
        val world = mock<World>()
        whenever(world.name).thenReturn(name)
        whenever(world.key).thenReturn(key)
        return world
    }
}
