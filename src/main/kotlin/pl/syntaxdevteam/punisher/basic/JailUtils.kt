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

    fun setJailLocation(config: FileConfiguration, location: Location, radius: Double): Boolean {
        val world = location.world?.name ?: return false

        config.set("jail.location.world", world)
        config.set("jail.location.x", location.x)
        config.set("jail.location.y", location.y)
        config.set("jail.location.z", location.z)
        config.set("jail.radius", radius)

        return true
    }
}
