package pl.syntaxdevteam.punisher.loader

import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX

class VersionChecker(private val plugin: PunisherX) {

    companion object {

        private val SUPPORTED_VERSIONS: Set<String> = setOf(
            "1.20.6",
            "1.21",
            "1.21.1",
            "1.21.2",
            "1.21.3",
            "1.21.4",
            "1.21.5"
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
            plugin.logger.success("The server is running on a supported version $version.")
            true
        } else {
            plugin.logger.warning("Warning! Unsupported version $version – use with caution!")
            false
        }
    }
}
