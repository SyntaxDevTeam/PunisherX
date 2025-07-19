package pl.syntaxdevteam.punisher.loader

import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX

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
            "1.21.8"
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

    fun isAtLeast(minVersion: String): Boolean {
        val current = getServerVersion().normalizeVersion()
        val required = minVersion.normalizeVersion()
        return compareVersions(current, required) >= 0
    }

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0..2) {
            val cmp = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (cmp != 0) return cmp
        }
        return 0
    }

    private fun String.normalizeVersion(): List<Int> {
        return this.split(".")
            .map { it.toIntOrNull() ?: 0 }
            .let {
                when (it.size) {
                    1 -> listOf(it[0], 0, 0)
                    2 -> listOf(it[0], it[1], 0)
                    else -> listOf(it[0], it[1], it[2])
                }
            }
    }
}
