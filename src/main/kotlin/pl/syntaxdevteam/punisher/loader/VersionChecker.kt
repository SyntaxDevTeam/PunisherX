package pl.syntaxdevteam.punisher.loader

import org.bukkit.Bukkit
import pl.syntaxdevteam.core.platform.ServerEnvironment
import pl.syntaxdevteam.punisher.PunisherX

class VersionChecker(private val plugin: PunisherX) {

    companion object {
        private val SUPPORTED_VERSIONS: Set<SemanticVersion> = setOf(
            SemanticVersion(1, 20, 6),
            SemanticVersion(1, 21, 0),
            SemanticVersion(1, 21, 1),
            SemanticVersion(1, 21, 2),
            SemanticVersion(1, 21, 3),
            SemanticVersion(1, 21, 4),
            SemanticVersion(1, 21, 5),
            SemanticVersion(1, 21, 6),
            SemanticVersion(1, 21, 7),
            SemanticVersion(1, 21, 8),
            SemanticVersion(1, 21, 9),
            SemanticVersion(1, 21, 10),
            SemanticVersion(1, 21, 11),
            SemanticVersion(26, 1, 0)
        )

        fun isVersionSupported(version: String): Boolean =
            SUPPORTED_VERSIONS.contains(SemanticVersion.parse(version))
    }

    private fun getRawVersion(): String =
        Bukkit.getServer().bukkitVersion

    fun getServerVersion(): String =
        getRawVersion().substringBefore("-")

    fun isSupported(): Boolean =
        isVersionSupported(getServerVersion())

    fun checkAndLog(): Boolean {
        val version = getServerVersion()
        return if (isSupported()) {
            val platformVersion = ServerEnvironment.describeWithVersion(version)
            plugin.logger.success("The server is running on a supported version $platformVersion.")
            true
        } else {
            plugin.logger.warning("Warning! Unsupported version $version – use with caution!")
            false
        }
    }

    fun getSemanticVersion(): SemanticVersion = SemanticVersion.parse(getServerVersion())

    fun isAtLeast(minVersion: String): Boolean {
        val current = getSemanticVersion()
        val required = SemanticVersion.parse(minVersion)
        return current >= required
    }
}
