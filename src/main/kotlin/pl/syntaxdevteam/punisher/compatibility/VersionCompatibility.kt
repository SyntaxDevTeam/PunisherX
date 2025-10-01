package pl.syntaxdevteam.punisher.compatibility

import org.bukkit.Material
import pl.syntaxdevteam.punisher.loader.SemanticVersion
import pl.syntaxdevteam.punisher.loader.VersionChecker
import pl.syntaxdevteam.punisher.loader.isBetween

/**
 * Handles runtime compatibility between different Minecraft versions.
 *
 * The manager keeps a timeline of elements that were introduced or removed
 * across supported versions and provides helper methods to resolve the
 * closest available alternative for the current server runtime.
 */
class VersionCompatibility(
    private val versionChecker: VersionChecker,
    private val parseVersion: (String) -> SemanticVersion = { SemanticVersion.parse(it) }
) {

    private val currentVersion: SemanticVersion = versionChecker.getSemanticVersion()

    private data class MaterialVariant(
        val materialName: String,
        val fromVersion: SemanticVersion,
        val untilVersion: SemanticVersion? = null
    ) {
        fun isApplicable(version: SemanticVersion): Boolean {
            return version.isBetween(fromVersion, untilVersion)
        }
    }

    private val materialTimeline: Map<String, List<MaterialVariant>> = mapOf(
        "CHAIN" to listOf(
            MaterialVariant(
                materialName = "CHAIN",
                fromVersion = parseVersion("1.16.0"),
                untilVersion = parseVersion("1.21.8")
            ),
            MaterialVariant(
                materialName = "IRON_BARS",
                fromVersion = parseVersion("1.21.9")
            )
        )
        // Additional materials can be added here as new runtime differences appear.
    )

    /**
     * Resolves a [Material] that is guaranteed to exist on the running server.
     *
     * The method first looks up version-specific variants registered for the
     * provided [key], then falls back to the [key] itself and finally to
     * optional [fallbacks]. The first material that exists in the current
     * runtime is returned.
     *
     * @throws IllegalArgumentException when none of the candidates exist on
     * the current server version.
     */
    fun resolveMaterial(key: String, vararg fallbacks: String): Material {
        val candidates = buildList {
            materialTimeline[key]
                ?.filter { it.isApplicable(currentVersion) }
                ?.mapTo(this) { it.materialName }
            add(key)
            fallbacks.forEach { add(it) }
            materialTimeline[key]
                ?.filterNot { it.isApplicable(currentVersion) }
                ?.mapTo(this) { it.materialName }
        }.distinct()

        for (candidate in candidates) {
            Material.matchMaterial(candidate)?.let { return it }
        }

        throw IllegalArgumentException(
            "No compatible material found for $key on Minecraft $currentVersion (candidates: ${candidates.joinToString()})"
        )
    }

    /**
     * Checks whether the provided [materialName] exists on the current server.
     */
    fun materialExists(materialName: String): Boolean {
        return Material.matchMaterial(materialName) != null
    }
}
