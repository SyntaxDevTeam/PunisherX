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
        val target = location.clone()
        if (isFolia()) {
            Bukkit.getServer().regionScheduler.execute(plugin, target) {
                if (!target.chunk.isLoaded) {
                    target.chunk.load()
                }
                if (!isLocationSafe(target)) {
                    Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                        callback(false)
                    }
                    return@execute
                }
                Bukkit.getServer().globalRegionScheduler.execute(plugin) {
                    player.teleportAsync(target).thenAccept { result ->
                        callback(result)
                    }.exceptionally {
                        callback(false)
                        null
                    }
                }
            }
        } else {
            runSync(plugin) {
                if (!target.chunk.isLoaded) {
                    target.chunk.load()
                }
                if (!isLocationSafe(target)) {
                    callback(false)
                    return@runSync
                }
                val result = player.teleport(target)
                callback(result)
            }
        }
    }

    private fun isLocationSafe(location: Location): Boolean {
        val world = location.world ?: return false
        val feetBlock = world.getBlockAt(location)
        val headBlock = feetBlock.getRelative(BlockFace.UP)
        val belowBlock = feetBlock.getRelative(BlockFace.DOWN)

        if (!isPassable(feetBlock) || !isPassable(headBlock)) {
            return false
        }

        if (!isSafeFloor(belowBlock)) {
            return false
        }

        return true
    }

    private fun isPassable(block: Block): Boolean {
        val type = block.type
        if (type == Material.LAVA || type == Material.FIRE || type == Material.CACTUS ||
            type == Material.SWEET_BERRY_BUSH || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE ||
            type == Material.MAGMA_BLOCK
        ) {
            return false
        }
        return block.isPassable || type == Material.AIR
    }

    private fun isSafeFloor(block: Block): Boolean {
        val type = block.type
        if (type == Material.AIR || !type.isSolid) {
            return false
        }
        if (type == Material.LAVA || type == Material.FIRE) {
            return false
        }
        return true
    }
}
