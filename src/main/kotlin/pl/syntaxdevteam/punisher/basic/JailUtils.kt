package pl.syntaxdevteam.punisher.basic

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import pl.syntaxdevteam.punisher.hooks.HookHandler

object JailUtils {

    fun getJailLocation(config: FileConfiguration): Location? {
        val worldName = config.getString("jail.location.world") ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null
        val x = config.getDouble("jail.location.x")
        val y = config.getDouble("jail.location.y")
        val z = config.getDouble("jail.location.z")
        val yaw = config.getDouble("jail.location.yaw", 0.0).toFloat()
        val pitch = config.getDouble("jail.location.pitch", 0.0).toFloat()
        return Location(world, x, y, z, yaw, pitch)
    }

    fun setJailLocation(config: FileConfiguration, location: Location, radius: Double): Boolean {
        val world = location.world?.name ?: return false

        config.set("jail.location.world", world)
        config.set("jail.location.x", location.x)
        config.set("jail.location.y", location.y)
        config.set("jail.location.z", location.z)
        config.set("jail.radius", radius)
        config.set("jail.location.yaw", location.yaw.toDouble())
        config.set("jail.location.pitch", location.pitch.toDouble())

        return true
    }

    fun setUnjailLocation(config: FileConfiguration, location: Location): Boolean {
        val world = location.world?.name ?: return false

        config.set("spawn.location.world", world)
        config.set("spawn.location.x", location.x)
        config.set("spawn.location.y", location.y)
        config.set("spawn.location.z", location.z)
        config.set("spawn.location.yaw", location.yaw.toDouble())
        config.set("spawn.location.pitch", location.pitch.toDouble())

        return true
    }

    fun getUnjailLocation(config: FileConfiguration, hookHandler: HookHandler? = null): Location? {
        if (config.getBoolean("spawn.use_external_set.enabled")) {
            when (config.getString("spawn.use_external_set.set")?.lowercase()) {
                "essx" -> {
                    val location = hookHandler?.getEssentialsSpawnLocation()
                    if (location != null) {
                        return location
                    }
                }
                "world" -> {
                    val defaultWorld = Bukkit.getWorlds().firstOrNull() ?: return null
                    return defaultWorld.spawnLocation.clone()
                }
            }
        }

        val worldName = config.getString("spawn.location.world") ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null
        val x = config.getDouble("spawn.location.x")
        val y = config.getDouble("spawn.location.y")
        val z = config.getDouble("spawn.location.z")
        val yaw = config.getDouble("spawn.location.yaw", 0.0).toFloat()
        val pitch = config.getDouble("spawn.location.pitch", 0.0).toFloat()
        return Location(world, x, y, z, yaw, pitch)
    }
}
