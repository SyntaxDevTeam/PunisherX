package pl.syntaxdevteam.punisher.teleport

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import pl.syntaxdevteam.punisher.common.ServerEnvironment
import pl.syntaxdevteam.punisher.platform.SchedulerAdapter
import kotlin.math.abs

class SafeTeleportService(
    private val plugin: Plugin,
    private val scheduler: SchedulerAdapter,
    private val foliaBasedOverride: Boolean? = null,
    private val safetyEvaluator: (Location) -> Boolean = Companion::isLocationSafe,
    private val chunkLoader: (Location) -> Unit = { location ->
        if (!location.chunk.isLoaded) {
            location.chunk.load()
        }
    }
) {

    private val foliaBased: Boolean = foliaBasedOverride ?: ServerEnvironment.isFoliaBased()

    fun teleportSafely(
        player: Player,
        location: Location,
        callback: (Boolean) -> Unit = {}
    ) {
        val target = location.clone()
        if (foliaBased) {
            scheduler.runRegionally(target) {
                chunkLoader(target)
                val safeLocation = findNearestSafeLocation(target)
                if (safeLocation == null) {
                    scheduler.runSync { callback(false) }
                    return@runRegionally
                }
                scheduler.runSync {
                    player.teleportAsync(safeLocation).thenAccept { result ->
                        callback(result)
                    }.exceptionally {
                        callback(false)
                        null
                    }
                }
            }
        } else {
            scheduler.runSync {
                chunkLoader(target)
                val safeLocation = findNearestSafeLocation(target)
                if (safeLocation == null) {
                    callback(false)
                    return@runSync
                }
                val result = player.teleport(safeLocation)
                callback(result)
            }
        }
    }

    fun findNearestSafeLocation(
        location: Location,
        horizontalRange: Int = 6,
        verticalRange: Int = 4
    ): Location? {
        val base = location.clone()
        if (safetyEvaluator(base)) {
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
            if (safetyEvaluator(candidate)) {
                return candidate
            }
        }

        return null
    }

    private companion object {
        fun isLocationSafe(location: Location): Boolean {
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

        fun isSafeBodyBlock(block: Block, allowSolid: Boolean): Boolean {
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

        fun isSafeFloor(block: Block): Boolean {
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
}
