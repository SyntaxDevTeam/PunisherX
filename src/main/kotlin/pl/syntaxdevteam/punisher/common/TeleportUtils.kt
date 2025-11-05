package pl.syntaxdevteam.punisher.common

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.math.abs

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

    /**
     * Finds the nearest safe [Location] to the provided [location]. The search scans
     * a cubic area around the location in increasing distance order. If the
     * provided location is already safe it will be returned unchanged. When no
     * safe location can be found within the search range, `null` is returned.
     */
    fun findNearestSafeLocation(
        location: Location,
        horizontalRange: Int = 6,
        verticalRange: Int = 4
    ): Location? {
        val base = location.clone()
        if (isLocationSafe(base)) {
            return base
        }

        val world = base.world ?: return null
        val offsets = mutableListOf<Triple<Int, Int, Int>>()

        for (dy in -verticalRange..verticalRange) {
            for (dx in -horizontalRange..horizontalRange) {
                for (dz in -horizontalRange..horizontalRange) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue
                    }
                    offsets.add(Triple(dx, dy, dz))
                }
            }
        }

        offsets.sortBy { (dx, dy, dz) -> dx * dx + dy * dy + dz * dz }

        for ((dx, dy, dz) in offsets) {
            val candidate = Location(
                world,
                base.x + dx,
                base.y + dy,
                base.z + dz,
                base.yaw,
                base.pitch
            )
            if (isLocationSafe(candidate)) {
                return candidate
            }
        }

        return null
    }

    private fun isLocationSafe(location: Location): Boolean {
        val world = location.world ?: return false
        val feetBlock = world.getBlockAt(location)
        val headBlock = feetBlock.getRelative(BlockFace.UP)
        val belowBlock = feetBlock.getRelative(BlockFace.DOWN)

        val yOffset = location.y - location.blockY
        val allowSolidFeet = abs(yOffset) < 1.0E-3

        if (!isSafeBodyBlock(feetBlock, allowSolidFeet) || !isSafeBodyBlock(headBlock, false)) {
            return false
        }

        if (!isSafeFloor(belowBlock)) {
            return false
        }

        return true
    }

    private fun isSafeBodyBlock(block: Block, allowSolid: Boolean): Boolean {
        val type = block.type
        if (type == Material.LAVA || type == Material.FIRE || type == Material.CACTUS ||
            type == Material.SWEET_BERRY_BUSH || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE ||
            type == Material.MAGMA_BLOCK || type == Material.WATER || type == Material.BUBBLE_COLUMN ||
            type == Material.POWDER_SNOW
        ) {
            return false
        }
        if (block.isPassable || type == Material.AIR) {
            return true
        }
        if (allowSolid && type.isSolid) {
            return true
        }
        return false
    }

    private fun isSafeFloor(block: Block): Boolean {
        val type = block.type
        if (type == Material.AIR || !type.isSolid) {
            return false
        }
        if (type == Material.LAVA || type == Material.FIRE || type == Material.WATER ||
            type == Material.MAGMA_BLOCK || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE
        ) {
            return false
        }
        return true
    }
}
