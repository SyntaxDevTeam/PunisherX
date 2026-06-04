package pl.syntaxdevteam.punisher.stats

import dev.faststats.ErrorTracker
import dev.faststats.Metrics
import dev.faststats.bukkit.BukkitContext
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path

class FastStatsBridge(
    private val plugin: JavaPlugin,
    private val token: String
) {
    private var errorTracker: ErrorTracker? = null
    private var context: BukkitContext? = null

    fun ready() {
        if (context != null) {
            return
        }

        try {
            FastStatsConfigMigrator.migrate(fastStatsConfigPath())

            val tracker = ErrorTracker.contextAware()
            val fastStatsContext = BukkitContext.Factory(plugin, token)
                .errorTrackerService(tracker)
                .metrics(Metrics.Factory::create)
                .create()

            errorTracker = tracker
            context = fastStatsContext
            fastStatsContext.ready()
        } catch (exception: Exception) {
            plugin.logger.warning("[PunisherX] FastStats is unavailable, metrics disabled: ${exception::class.java.simpleName}: ${exception.message}")
        }
    }

    private fun fastStatsConfigPath(): Path {
        val pluginsFolder = try {
            plugin.server.pluginsFolder.toPath()
        } catch (_: NoSuchMethodError) {
            plugin.dataFolder.parentFile.toPath()
        }
        return pluginsFolder.resolve("faststats").resolve("config.properties")
    }

    fun shutdown() {
        context?.shutdown()
        context = null
        errorTracker = null
    }

    fun trackError(throwable: Throwable) {
        errorTracker?.trackError(throwable)
    }
}
