package pl.syntaxdevteam.punisher.common

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

object TeleportUtils {

    /**
     * Checks if the current server is running Folia.
     */
    fun isFolia(): Boolean = Bukkit.getServer().name.contains("Folia", ignoreCase = true)

    /**
     * Runs the given [task] asynchronously using the appropriate scheduler for
     * the current server implementation.
     */
    fun runAsync(plugin: Plugin, task: Runnable) {
        if (isFolia()) {
            Bukkit.getServer().globalRegionScheduler.execute(plugin, task)
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        }
    }

    /**
     * Runs the given [task] synchronously on the main server thread using the
     * appropriate scheduler for the current server implementation.
     */
    fun runSync(plugin: Plugin, task: Runnable) {
        if (isFolia()) {
            Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                task.run()
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    /**
     * Runs the given [task] synchronously after [delayTicks] ticks using the
     * appropriate scheduler for the current server implementation.
     */
    fun runSyncLater(plugin: Plugin, delayTicks: Long, task: Runnable) {
        if (isFolia()) {
            Bukkit.getServer().globalRegionScheduler.runDelayed(plugin, { task.run() }, delayTicks)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks)
        }
    }

    /**
     * Teleports [player] to [location] using the correct scheduler for either
     * Folia or Paper. Chunk loading is handled automatically, the teleport
     * occurs asynchronously, and the destination is made safe.
     */
    fun teleportSafely(plugin: Plugin, player: Player, location: Location, callback: (Boolean) -> Unit = {}) {
        if (isFolia()) {
            Bukkit.getServer().regionScheduler.execute(plugin, location) {
                if (!location.chunk.isLoaded) {
                    location.chunk.load()
                }
                val safe = findSafeLocation(location)
                Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                    player.teleportAsync(safe).thenAccept { result ->
                        callback(result)
                    }.exceptionally {
                        callback(false)
                        null
                    }
                }
            }
        } else {
            runSync(plugin) {
                if (!location.chunk.isLoaded) {
                    location.chunk.load()
                }
                val safe = findSafeLocation(location)
                val result = player.teleport(safe)
                callback(result)
            }
        }
    }

    private fun findSafeLocation(location: Location): Location {
        val world = location.world ?: return location
        val x = location.blockX
        val z = location.blockZ
        var block = world.getHighestBlockAt(x, z)
        while (isUnsafe(block) && block.y > world.minHeight) {
            block = block.getRelative(BlockFace.DOWN)
        }
        val safe = block.location.add(0.5, 1.0, 0.5)
        safe.yaw = location.yaw
        safe.pitch = location.pitch
        return safe
    }

    private fun isUnsafe(block: Block): Boolean {
        val type = block.type
        return type == Material.LAVA || type == Material.FIRE || !type.isSolid
    }
}
