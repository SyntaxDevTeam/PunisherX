package pl.syntaxdevteam.punisher.basic

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration

object JailUtils {

    fun getJailLocation(config: FileConfiguration): Location? {
        val worldName = config.getString("jail.location.world") ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null
        val x = config.getDouble("jail.location.x")
        val y = config.getDouble("jail.location.y")
        val z = config.getDouble("jail.location.z")
        val location = Location(world, x, y, z)

        if (!location.chunk.isLoaded) {
            location.chunk.load(true)
        }
        return location
    }
}
