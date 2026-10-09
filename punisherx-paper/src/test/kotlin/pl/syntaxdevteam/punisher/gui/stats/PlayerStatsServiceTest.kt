package pl.syntaxdevteam.punisher.gui.stats

import org.bukkit.Location
import org.bukkit.World
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerStatsServiceTest {
    @Test
    fun `logout coordinates are available immediately and survive reinitialization`() {
        val folder = Files.createTempDirectory("punisher-logout-test").toFile()
        try {
            val world = java.lang.reflect.Proxy.newProxyInstance(
                World::class.java.classLoader, arrayOf(World::class.java)
            ) { _, method, _ ->
                when (method.name) {
                    "getName" -> "custom_world"
                    else -> error("Unexpected world method: ${method.name}")
                }
            } as World
            val uuid = UUID.randomUUID()
            PlayerStatsService.initialize(folder)
            val location = Location(world, -1.5, 64.9, 12.3)
            val save = PlayerStatsService.captureLogoutLocation(uuid, location)
            location.x = 100.0
            assertEquals("custom_world: -2, 64, 12", PlayerStatsService.getLastLocationString(uuid))
            save.run()
            PlayerStatsService.initialize(folder)
            assertEquals("custom_world: -2, 64, 12", PlayerStatsService.getLastLocationString(uuid))
        } finally {
            PlayerStatsService.initialize(folder)
            folder.deleteRecursively()
        }
    }
}
