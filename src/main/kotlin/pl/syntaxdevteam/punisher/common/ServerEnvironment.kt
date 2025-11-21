package pl.syntaxdevteam.punisher.common

import org.bukkit.Bukkit

/**
 * Centralized detector that identifies the current server platform so that the
 * plugin can adjust behavior for Paper-based and Folia-based implementations
 * (including their forks).
 */
object ServerEnvironment {

    private val knownNames = mapOf(
        "folia" to "Folia",
        "luminol" to "Luminol",
        "mint" to "Mint",
        "paper" to "Paper",
        "pufferfish" to "Pufferfish",
        "purpur" to "Purpur",
        "leaves" to "Leaves"
    )

    private val foliaMarkers = setOf("folia", "luminol", "mint")
    private val paperMarkers = setOf("paper", "purpur", "pufferfish", "leaves", "spigot", "bukkit")

    private val versionBrandRegex = Regex("git-([A-Za-z][A-Za-z0-9_-]*)")

    enum class PlatformFamily { FOLIA_BASED, PAPER_BASED, UNKNOWN }

    data class Platform(val family: PlatformFamily, val name: String)

    private val platform: Platform by lazy { detectPlatform() }

    val platformName: String
        get() = platform.name

    val family: PlatformFamily
        get() = platform.family

    fun isFoliaBased(): Boolean = platform.family == PlatformFamily.FOLIA_BASED

    fun isPaperBased(): Boolean = platform.family == PlatformFamily.PAPER_BASED

    fun describeWithVersion(version: String): String = "$platformName $version".trim()

    private fun detectPlatform(): Platform {
        val candidates = listOfNotNull(
            Bukkit.getName(),
            Bukkit.getServer().name,
            Bukkit.getVersion(),
            Bukkit.getServer().bukkitVersion,
            runCatching { Bukkit.getServer().javaClass.simpleName }.getOrNull(),
            runCatching { Bukkit.getServer().javaClass.name }.getOrNull()
        )

        val tokens = candidates.flatMap { candidate ->
            candidate.split("""[\\s\\-\\(\\)\\[\\]:,]+""".toRegex())
                .filter { it.isNotBlank() }
        }
        val normalized = tokens.map { it.lowercase() }

        val detectedName = detectPlatformName(tokens, candidates)

        val foliaApiPresent = hasClass("io.papermc.paper.threadedregions.RegionizedServer") ||
            hasClass("io.papermc.paper.threadedregions.scheduler.RegionScheduler")

        return when {
            foliaApiPresent || normalized.any { token -> foliaMarkers.contains(token) } ->
                Platform(PlatformFamily.FOLIA_BASED, detectedName.ifBlank { "Folia" })

            normalized.any { token -> paperMarkers.contains(token) } ||
                hasClass("com.destroystokyo.paper.PaperConfig") ||
                hasClass("io.papermc.paper.configuration.Configuration") ->
                Platform(PlatformFamily.PAPER_BASED, detectedName.ifBlank { "Paper" })

            else -> Platform(PlatformFamily.UNKNOWN, detectedName.ifBlank { "Unknown" })
        }
    }

    private fun detectPlatformName(tokens: List<String>, rawCandidates: List<String>): String {
        val packageInfo = runCatching { Bukkit.getServer().javaClass.`package` }.getOrNull()
        val manifestName = listOfNotNull(
            packageInfo?.implementationTitle,
            packageInfo?.specificationTitle,
            packageInfo?.implementationVendor
        ).firstOrNull { it.isNotBlank() }

        val normalizedTokens = tokens.map { it.lowercase() }
        val knownName = normalizedTokens.firstNotNullOfOrNull { key -> knownNames[key] }

        val gitName = rawCandidates.firstNotNullOfOrNull { candidate ->
            versionBrandRegex.find(candidate)?.groupValues?.getOrNull(1)
        }

        return knownName
            ?: gitName
            ?: manifestName
            ?: rawCandidates.firstOrNull { it.isNotBlank() }
            ?: "Unknown"
    }

    private fun hasClass(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            true
        }
    }
}
