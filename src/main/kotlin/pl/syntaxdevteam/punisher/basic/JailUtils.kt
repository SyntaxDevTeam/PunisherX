package pl.syntaxdevteam.punisher.basic

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.FileConfiguration
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.teleport.SafeTeleportService
import java.util.LinkedHashSet
import java.util.Locale

object JailUtils {

    enum class UnjailLocationSource {
        CONFIGURED,
        LAST_LOCATION,
        BED,
        ESSENTIALS,
        WORLD
    }

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

        config.set("unjail.unjail_location.world", world)
        config.set("unjail.unjail_location.x", location.x)
        config.set("unjail.unjail_location.y", location.y)
        config.set("unjail.unjail_location.z", location.z)
        config.set("unjail.unjail_location.yaw", location.yaw.toDouble())
        config.set("unjail.unjail_location.pitch", location.pitch.toDouble())

        // Maintain legacy keys for backwards compatibility with existing configs
        config.set("spawn.location.world", world)
        config.set("spawn.location.x", location.x)
        config.set("spawn.location.y", location.y)
        config.set("spawn.location.z", location.z)
        config.set("spawn.location.yaw", location.yaw.toDouble())
        config.set("spawn.location.pitch", location.pitch.toDouble())

        return true
    }

    fun getUnjailLocation(
        config: FileConfiguration,
        hookHandler: HookHandler? = null,
        lastLocation: Location? = null,
        player: OfflinePlayer? = null,
        safeTeleportService: SafeTeleportService
    ): Location? {
        val orderedSources = LinkedHashSet<UnjailLocationSource>().apply {
            addAll(getSpawnTypeSelection(config))
            addAll(getLegacySourceOrder(config))
            addAll(DEFAULT_SOURCE_ORDER)
        }

        for (source in orderedSources) {
            val resolved = when (source) {
                UnjailLocationSource.CONFIGURED -> getConfiguredUnjailLocation(config)
                UnjailLocationSource.LAST_LOCATION -> cloneIfValid(lastLocation)
                UnjailLocationSource.BED -> cloneIfValid(getBedSpawnLocation(player))
                UnjailLocationSource.ESSENTIALS -> hookHandler?.getEssentialsSpawnLocation()?.clone()
                UnjailLocationSource.WORLD -> Bukkit.getWorlds().firstOrNull()?.spawnLocation?.clone()
            }

            if (resolved != null) {
                val safeLocation = safeTeleportService.findNearestSafeLocation(resolved)
                if (safeLocation != null) {
                    return safeLocation
                }
            }
        }

        return null
    }

    private val DEFAULT_SOURCE_ORDER = listOf(
        UnjailLocationSource.CONFIGURED,
        UnjailLocationSource.LAST_LOCATION,
        UnjailLocationSource.BED,
        UnjailLocationSource.ESSENTIALS,
        UnjailLocationSource.WORLD
    )

    private fun getSpawnTypeSelection(config: FileConfiguration): List<UnjailLocationSource> {
        val raw = config.get("unjail.spawn_type_select.set") ?: return emptyList()
        val mapped = when (raw) {
            is String -> listOfNotNull(parseSourceType(raw, null))
            is List<*> -> raw.mapNotNull { (it as? String)?.let { value -> parseSourceType(value, null) } }
            else -> emptyList()
        }
        return mapped
    }

    private fun getLegacySourceOrder(config: FileConfiguration): List<UnjailLocationSource> {
        val normalized = LinkedHashSet<UnjailLocationSource>()

        val rawList = config.getList("spawn.release_sources")
        if (!rawList.isNullOrEmpty()) {
            for (entry in rawList) {
                when (entry) {
                    is String -> parseSourceType(entry, null)?.let { normalized.add(it) }
                    is Map<*, *> -> {
                        val enabled = (entry["enabled"] as? Boolean) ?: true
                        if (!enabled) {
                            continue
                        }

                        val typeKey = entry["type"] ?: entry["source"] ?: entry["name"] ?: entry["id"]
                        val type = (typeKey as? String) ?: continue
                        val provider = (entry["provider"] ?: entry["set"] ?: entry["id"]) as? String

                        parseSourceType(type, provider)?.let { normalized.add(it) }
                    }
                }
            }

            if (normalized.isNotEmpty()) {
                return normalized.toList()
            }
        }

        val priorityList = config.getStringList("spawn.release_priority")
        val externalEnabled = config.getBoolean("spawn.use_external_set.enabled")
        val externalProvider = config.getString("spawn.use_external_set.set")

        if (priorityList.isNotEmpty()) {
            for (entry in priorityList) {
                val mapped = when (parseSourceType(entry, externalProvider)) {
                    UnjailLocationSource.ESSENTIALS, UnjailLocationSource.WORLD ->
                        if (externalEnabled) parseSourceType(entry, externalProvider) else null
                    else -> parseSourceType(entry, externalProvider)
                }

                if (mapped != null) {
                    normalized.add(mapped)
                }
            }

            if (normalized.isNotEmpty()) {
                return normalized.toList()
            }
        }

        if (!externalEnabled) {
            normalized.add(UnjailLocationSource.LAST_LOCATION)
            normalized.add(UnjailLocationSource.CONFIGURED)
            return normalized.toList()
        }

        if (!externalProvider.isNullOrBlank()) {
            normalized.add(UnjailLocationSource.LAST_LOCATION)
            parseSourceType("external", externalProvider)?.let { normalized.add(it) }
            normalized.add(UnjailLocationSource.CONFIGURED)
            return normalized.toList()
        }

        return emptyList()
    }

    private fun parseSourceType(value: String, provider: String?): UnjailLocationSource? {
        return when (value.lowercase(Locale.ROOT)) {
            "unjail", "configured", "config", "set", "spawn" -> UnjailLocationSource.CONFIGURED
            "last_location", "last-location", "lastlocation", "player" -> UnjailLocationSource.LAST_LOCATION
            "bed", "bed_spawn", "bed-spawn", "respawn" -> UnjailLocationSource.BED
            "essx", "essentials", "foliessentials" -> UnjailLocationSource.ESSENTIALS
            "world", "default", "default_world" -> UnjailLocationSource.WORLD
            "external", "external_set", "external-set", "plugin" -> mapExternalProvider(provider)
            else -> null
        }
    }

    private fun mapExternalProvider(provider: String?): UnjailLocationSource {
        return when (provider?.lowercase(Locale.ROOT)) {
            "essx", "essentials", "foliessentials" -> UnjailLocationSource.ESSENTIALS
            else -> UnjailLocationSource.WORLD
        }
    }

    private fun getConfiguredUnjailLocation(config: FileConfiguration): Location? {
        val basePath = when {
            config.contains("unjail.unjail_location.world") -> "unjail.unjail_location"
            else -> "spawn.location"
        }

        val worldName = config.getString("$basePath.world") ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null
        val x = config.getDouble("$basePath.x")
        val y = config.getDouble("$basePath.y")
        val z = config.getDouble("$basePath.z")
        val yaw = config.getDouble("$basePath.yaw", 0.0).toFloat()
        val pitch = config.getDouble("$basePath.pitch", 0.0).toFloat()
        return Location(world, x, y, z, yaw, pitch)
    }

    private fun cloneIfValid(location: Location?): Location? {
        return location?.takeIf { it.world != null }?.clone()
    }

    @Suppress("DEPRECATION")
    private fun getBedSpawnLocation(player: OfflinePlayer?): Location? {
        return player?.bedSpawnLocation
    }
}
