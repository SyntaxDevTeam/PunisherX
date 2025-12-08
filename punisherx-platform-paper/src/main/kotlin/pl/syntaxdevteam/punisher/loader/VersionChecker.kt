package pl.syntaxdevteam.punisher.loader

import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.core.platform.ServerEnvironment

class VersionChecker(private val plugin: PunisherX) {

    companion object {
        private val SUPPORTED_VERSIONS: Set<String> = setOf(
            "1.20.6",
            "1.21",
            "1.21.0",
            "1.21.1",
            "1.21.2",
            "1.21.3",
            "1.21.4",
            "1.21.5",
            "1.21.6",
            "1.21.7",
            "1.21.8",
            "1.21.9",
            "1.21.10",
            "1.21.11"
        )
    }

    private fun getRawVersion(): String =
        Bukkit.getServer().bukkitVersion

    fun getServerVersion(): String =
        getRawVersion().substringBefore("-")

    fun isSupported(): Boolean =
        SUPPORTED_VERSIONS.contains(getServerVersion())

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
