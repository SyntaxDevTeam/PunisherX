package pl.syntaxdevteam.punisher

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import pl.syntaxdevteam.core.SyntaxCore
import pl.syntaxdevteam.core.logging.Logger
import pl.syntaxdevteam.core.manager.PluginManagerX
import pl.syntaxdevteam.core.platform.ServerEnvironment
import pl.syntaxdevteam.core.stats.StatsCollector
import pl.syntaxdevteam.core.update.GitHubSource
import pl.syntaxdevteam.core.update.ModrinthSource
import pl.syntaxdevteam.message.MessageHandler
import pl.syntaxdevteam.punisher.api.PunisherXApi
import pl.syntaxdevteam.punisher.basic.*
import pl.syntaxdevteam.punisher.bridge.OnlinePunishmentWatcher
import pl.syntaxdevteam.punisher.bridge.ProxyBridgeMessenger
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.CommandLoggerPlugin
import pl.syntaxdevteam.punisher.common.ConfigManager
import pl.syntaxdevteam.punisher.common.PunishmentActionExecutor
import pl.syntaxdevteam.punisher.compatibility.VersionChecker
import pl.syntaxdevteam.punisher.compatibility.VersionCompatibility
import pl.syntaxdevteam.punisher.compatibility.platform.SchedulerAdapter
import pl.syntaxdevteam.punisher.config.ConfigurationPreflight
import pl.syntaxdevteam.punisher.config.ConfigurationValidationReport
import pl.syntaxdevteam.punisher.databases.*
import pl.syntaxdevteam.punisher.gui.GuiPunishmentService
import pl.syntaxdevteam.punisher.gui.GuiSearchService
import pl.syntaxdevteam.punisher.gui.materials.GuiMaterialResolver
import pl.syntaxdevteam.punisher.hooks.DiscordBridge
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.identity.PunisherXIdentityMigrationService
import pl.syntaxdevteam.punisher.listeners.PlayerJoinListener
import pl.syntaxdevteam.punisher.loader.PluginInitializer
import pl.syntaxdevteam.punisher.players.*
import pl.syntaxdevteam.punisher.reports.ReportService
import pl.syntaxdevteam.punisher.stats.FastStatsBridge
import pl.syntaxdevteam.punisher.teleport.SafeTeleportService
import pl.syntaxdevteam.punisher.templates.PunishTemplateManager
import java.io.File
import java.util.Properties
import java.util.UUID

class PunisherX : JavaPlugin(), Listener {
    private val fastStatsBridge: FastStatsBridge by lazy {
        FastStatsBridge(this, "face5edae5524c9d98184b87d9f48aeb")
    }

    @Volatile
    var commandsRegistered: Boolean = false
    private lateinit var pluginInitializer: PluginInitializer

    lateinit var logger: Logger
    lateinit var messageHandler: MessageHandler
    lateinit var pluginsManager: PluginManagerX

    lateinit var pluginConfig: FileConfiguration
    lateinit var statsCollector: StatsCollector

    lateinit var punishmentChecker: PunishmentChecker
    lateinit var playerJoinListener: PlayerJoinListener

    lateinit var databaseHandler: DatabaseHandler
    lateinit var timeHandler: TimeHandler
    lateinit var geoIPHandler: GeoIPHandler
    lateinit var punishmentManager: PunishmentManager
    lateinit var cache: PunishmentCache
    lateinit var punishmentActionBarNotifier: PunishmentActionBarNotifier
    lateinit var punisherXApi: PunisherXApi
    lateinit var hookHandler: HookHandler
    lateinit var discordWebhook: DiscordWebhook
    lateinit var commandLoggerPlugin: CommandLoggerPlugin
    lateinit var commandManager: CommandManager
    lateinit var playerIPManager: PlayerIPManager
    lateinit var versionChecker: VersionChecker
    lateinit var versionCompatibility: VersionCompatibility
    lateinit var guiMaterialResolver: GuiMaterialResolver
    lateinit var guiPunishmentService: GuiPunishmentService
    lateinit var guiSearchService: GuiSearchService
    lateinit var actionExecutor: PunishmentActionExecutor
    lateinit var schedulerAdapter: SchedulerAdapter
    lateinit var safeTeleportService: SafeTeleportService
    lateinit var cfg: ConfigManager
    lateinit var proxyBridgeMessenger: ProxyBridgeMessenger
    lateinit var onlinePunishmentWatcher: OnlinePunishmentWatcher
    lateinit var punishTemplateManager: PunishTemplateManager
    lateinit var discordBridge: DiscordBridge
    val reportService: ReportService by lazy(LazyThreadSafetyMode.NONE) { ReportService(this) }

    override fun onEnable() {
        SyntaxCore.registerUpdateSources(
            GitHubSource("SyntaxDevTeam/PunisherX"),
            ModrinthSource("VCNRcwC2")
        )
        SyntaxCore.init(this, versionType = "paper")

        // Language YAML is deliberately excluded here: MessageHandler must be able to
        // preserve a broken translation and start on its bundled in-memory fallback.
        val startupValidation = validateConfiguration(includeLanguageFiles = false)
        if (!startupValidation.valid) {
            startupValidation.issues.forEach { issue ->
                super.getLogger().severe(
                    "[Config validation] ${startupValidation.relativePath(issue.file)}: ${issue.error}"
                )
            }
            throw IllegalStateException(
                "PunisherX startup aborted because YAML validation failed: ${startupValidation.compactMessage()}"
            )
        }

        pluginInitializer = PluginInitializer(this)
        pluginInitializer.onEnable()
        server.servicesManager.register(
            PunisherXIdentityMigrationService::class.java,
            PunisherXIdentityMigrationService(this),
            this,
            org.bukkit.plugin.ServicePriority.Normal,
        )
        versionChecker.checkAndLog()
        fastStatsBridge.ready()
    }

    /**
     * Validates runtime YAML files without modifying them.
     */
    fun validateConfiguration(includeLanguageFiles: Boolean = true): ConfigurationValidationReport =
        ConfigurationPreflight(this).validate(includeLanguageFiles)

    /**
     * Reloads PunisherX only after a successful read-only YAML preflight.
     * If validation fails, the currently running plugin instance remains untouched.
     */
    fun onReload() {
        val validation = validateConfiguration()
        if (!validation.valid) {
            validation.issues.forEach { issue ->
                logger.err("[Config validation] ${validation.relativePath(issue.file)}: ${issue.error}")
            }
            throw IllegalStateException(
                "Reload aborted because YAML validation failed: ${validation.compactMessage()}"
            )
        }
        reloadMyConfig()
    }

    override fun onDisable() {
        server.servicesManager.unregisterAll(this)
        runCatching { fastStatsBridge.shutdown() }
        if (this::databaseHandler.isInitialized) {
            databaseHandler.closeConnection()
        }
        AsyncChatEvent.getHandlerList().unregister(this as Plugin)
        if (::pluginInitializer.isInitialized) {
            pluginInitializer.onDisable()
        }
        if (this::proxyBridgeMessenger.isInitialized) {
            runCatching { proxyBridgeMessenger.unregisterChannel() }
        }
    }

    fun resolvePlayerUuid(identifier: String): UUID {
        runCatching { UUID.fromString(identifier) }.getOrNull()?.let { return it }
        Bukkit.getPlayerUniqueId(identifier)?.let { return it }
        Bukkit.getOfflinePlayerIfCached(identifier)?.uniqueId?.let { return it }
        return Bukkit.getOfflinePlayer(identifier).uniqueId
    }

    private fun reloadMyConfig() {
        pluginInitializer.onDisable()
        cancelPluginTasks()
        runCatching { proxyBridgeMessenger.unregisterChannel() }
        HandlerList.unregisterAll(this as Plugin)
        reloadConfig()
        pluginInitializer = PluginInitializer(this)
        pluginInitializer.onEnable()
    }

    private fun cancelPluginTasks() {
        if (ServerEnvironment.isFoliaBased()) {
            tryCancelScheduler("getGlobalRegionScheduler")
            tryCancelScheduler("getRegionScheduler")
            tryCancelScheduler("getAsyncScheduler")
        } else {
            server.scheduler.cancelTasks(this)
        }
    }

    private fun tryCancelScheduler(methodName: String) {
        try {
            val scheduler = server.javaClass.getMethod(methodName).invoke(server) ?: return
            scheduler.javaClass.getMethod("cancelTasks", Plugin::class.java).invoke(scheduler, this)
        } catch (_: Throwable) {
            reportError(Throwable("Failed to cancel tasks using $methodName, falling back to standard cancellation."))
        }
    }

    fun getServerName(): String {
        val properties = Properties()
        val file = File("server.properties")
        if (file.exists()) {
            properties.load(file.inputStream())
            val serverName = properties.getProperty("server-name")
            if (serverName != null) {
                return serverName
            } else {
                logger.debug("Property 'server-name' not found in server.properties file.")
            }
        } else {
            logger.debug("The server.properties file does not exist.")
        }
        return "Unknown Server"
    }

    fun reportError(throwable: Throwable) {
        fastStatsBridge.trackError(throwable)
    }
}
