package pl.syntaxdevteam.punisher.loader

import org.bukkit.plugin.ServicePriority
import org.bukkit.scheduler.BukkitRunnable
import pl.syntaxdevteam.core.SyntaxCore
import pl.syntaxdevteam.message.SyntaxMessages
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.api.PunisherXApi
import pl.syntaxdevteam.punisher.api.PunisherXApiImpl
import pl.syntaxdevteam.punisher.basic.PunishmentActionBarNotifier
import pl.syntaxdevteam.punisher.basic.PunishmentCache
import pl.syntaxdevteam.punisher.basic.PunishmentChecker
import pl.syntaxdevteam.punisher.basic.PunishmentManager
import pl.syntaxdevteam.punisher.basic.TimeHandler
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.CommandLoggerPlugin
import pl.syntaxdevteam.punisher.common.ConfigManager
import pl.syntaxdevteam.punisher.common.PunishmentActionExecutor
import pl.syntaxdevteam.core.platform.ServerEnvironment
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.compatibility.VersionCompatibility
import pl.syntaxdevteam.punisher.gui.materials.GuiMaterialResolver
import pl.syntaxdevteam.punisher.gui.interfaces.GUIHandler
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.listeners.ReloadListener
import pl.syntaxdevteam.punisher.listeners.LegacyLoginListener
import pl.syntaxdevteam.punisher.listeners.ModernLoginListener
import pl.syntaxdevteam.punisher.listeners.PlayerJoinListener
import pl.syntaxdevteam.punisher.placeholders.PlaceholderHandler
import pl.syntaxdevteam.punisher.players.*
import pl.syntaxdevteam.punisher.platform.BukkitSchedulerAdapter
import pl.syntaxdevteam.punisher.teleport.SafeTeleportService
import pl.syntaxdevteam.punisher.bridge.OnlinePunishmentWatcher
import pl.syntaxdevteam.punisher.bridge.ProxyBridgeMessenger
import pl.syntaxdevteam.punisher.templates.PunishTemplateManager
import java.io.File
import java.util.Locale

class PluginInitializer(private val plugin: PunisherX) {

    fun onEnable() {
        setUpLogger()
        setupConfig()
        setupDatabase()
        setupHandlers()
        registerEvents()
        registerCommands()
        checkForUpdates()
    }

    fun onDisable() {
        runCatching { plugin.onlinePunishmentWatcher.stop() }
        runCatching { plugin.punishmentActionBarNotifier.stop() }
        plugin.databaseHandler.closeConnection()
        plugin.logger.err(plugin.pluginMeta.name + " " + plugin.pluginMeta.version + " has been disabled ☹️")
    }

    private fun setUpLogger() {
        plugin.pluginConfig = plugin.config
        plugin.logger = SyntaxCore.logger
    }

    /**
     * Sets up the plugin configuration.
     */
    fun setupConfig() {
        // val confFile = File(plugin.dataFolder, "config.yml")
        //        val version = plugin.config.getInt("config-version")
        //        plugin.cfg = ConfigManager(plugin, plugin.logger, confFile.toString(), version.toString(), 141, 150, true)
        plugin.cfg = ConfigManager(plugin)
        plugin.cfg.load()
        plugin.punishTemplateManager = PunishTemplateManager(plugin)
        plugin.punishTemplateManager.load()
    }

    /**
     * Sets up the database connection and creates necessary tables.
     */
    private fun setupDatabase() {
        plugin.databaseHandler = DatabaseHandler(plugin)
        val databaseSetupTask = Runnable {
            plugin.databaseHandler.openConnection()
            plugin.databaseHandler.createTables()
        }
        plugin.logger.debug("Detected server: ${ServerEnvironment.platformName} (${ServerEnvironment.family})")
        if (ServerEnvironment.isFoliaBased()) {
            plugin.logger.debug("Detected Folia server, using async database connection handling.")
            plugin.server.globalRegionScheduler.execute(plugin, databaseSetupTask)
        } else if (ServerEnvironment.isPaperBased()) {
            plugin.logger.debug("Detected Paper server, using async database connection handling.")
            plugin.server.scheduler.runTaskAsynchronously(plugin, databaseSetupTask)
        }
    }

    /**
     * Initializes various handlers used by the plugin.
     */
    private fun setupHandlers() {
        SyntaxMessages.initialize(plugin)
        plugin.messageHandler = SyntaxMessages.messages
        plugin.pluginsManager = SyntaxCore.pluginManagerx
        plugin.timeHandler = TimeHandler(plugin)
        plugin.punishmentManager = PunishmentManager()
        plugin.schedulerAdapter = BukkitSchedulerAdapter(plugin)
        plugin.safeTeleportService = SafeTeleportService(plugin.schedulerAdapter)
        plugin.geoIPHandler = GeoIPHandler(plugin)
        plugin.cache = PunishmentCache(plugin)
        plugin.punishmentActionBarNotifier = PunishmentActionBarNotifier(plugin).also { it.start() }
        plugin.punisherXApi = PunisherXApiImpl(plugin.databaseHandler)
        plugin.hookHandler = HookHandler(plugin)
        plugin.discordWebhook = DiscordWebhook(plugin)
        plugin.playerIPManager = PlayerIPManager(plugin, plugin.geoIPHandler)
        plugin.punishmentChecker = PunishmentChecker(plugin)
        plugin.versionChecker = VersionChecker(plugin)
        plugin.versionCompatibility = VersionCompatibility(plugin.versionChecker)
        plugin.guiMaterialResolver = GuiMaterialResolver(plugin.versionChecker.getSemanticVersion())
        plugin.actionExecutor = PunishmentActionExecutor(plugin)
        plugin.proxyBridgeMessenger = ProxyBridgeMessenger(plugin).also { it.registerChannel() }
        plugin.onlinePunishmentWatcher = OnlinePunishmentWatcher(plugin).also { it.start() }
        checkLegacyPlaceholders()
    }

    /**
     * Registers the plugin commands.
     */
    private fun registerCommands(){
        plugin.commandLoggerPlugin = CommandLoggerPlugin(plugin)
        plugin.commandManager = CommandManager(plugin)
        if (plugin.commandsRegistered) return

        plugin.commandManager.registerCommands()
        plugin.commandsRegistered = true
    }

    /**
     * Registers the plugin events.
     */
    private fun registerEvents() {
        val versionChecker = runCatching { plugin.versionChecker }
            .getOrElse {
                VersionChecker(plugin).also { plugin.versionChecker = it }
            }
        plugin.playerJoinListener = PlayerJoinListener(plugin.playerIPManager, plugin.punishmentChecker)
        plugin.server.pluginManager.registerEvents(plugin.playerJoinListener, plugin)
        plugin.server.pluginManager.registerEvents(plugin.punishmentChecker, plugin)
        if (versionChecker.isAtLeast("1.21.7")) {
            plugin.server.pluginManager.registerEvents(ModernLoginListener(plugin), plugin)
            plugin.logger.debug("Registered ModernLoginListener for 1.21.7+")
        } else {
            plugin.server.pluginManager.registerEvents(LegacyLoginListener(plugin), plugin)
            plugin.logger.debug("Registered LegacyLoginListener for pre-1.21.7")
        }
        plugin.server.pluginManager.registerEvents(ReloadListener(plugin), plugin)
        plugin.server.servicesManager.register(PunisherXApi::class.java, plugin.punisherXApi, plugin, ServicePriority.Normal)
        if (plugin.hookHandler.checkPlaceholderAPI()) {
            PlaceholderHandler(plugin).register()
        }
        plugin.server.pluginManager.registerEvents(GUIHandler(plugin), plugin)
    }

    /**
     * Checks for updates to the plugin.
     */
    private fun checkForUpdates() {
        plugin.statsCollector = SyntaxCore.statsCollector
        SyntaxCore.updateChecker.checkAsync()
    }


    /**
     * Checks the selected language file for legacy placeholders formatted with curly braces.
     * If found, logs a warning and periodically reminds the console to run the placeholder
     * conversion command or regenerate the file.
     */
    private fun checkLegacyPlaceholders() {
        val lang = plugin.config.getString("language")?.lowercase(Locale.getDefault()) ?: "en"
        val langDir = File(plugin.dataFolder, "lang")
        val candidates = listOf(
            File(langDir, "messages_$lang.yml"),
            File(langDir, "message_$lang.yml")
        )
        val langFile = candidates.firstOrNull { it.exists() } ?: return

        val legacyPattern = Regex("\\{\\w+}")
        val warnMsg =
            "Language file ${langFile.name} uses legacy placeholders with {}. Run /langfix or delete the file to regenerate."

        try {
            val content = langFile.readText(Charsets.UTF_8)
            if (!legacyPattern.containsMatchIn(content)) return
        } catch (e: Exception) {
            plugin.logger.warning("Could not check language file placeholders: ${e.message}")
            return
        }

        plugin.logger.warning(warnMsg)

        val delay = 20L * 10L
        if (ServerEnvironment.isFoliaBased()) {
            plugin.server.globalRegionScheduler.runAtFixedRate(plugin, { task ->
                try {
                    val content = langFile.readText(Charsets.UTF_8)
                    if (legacyPattern.containsMatchIn(content)) {
                        plugin.logger.warning(warnMsg)
                    } else {
                        task.cancel()
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Could not check language file placeholders: ${e.message}")
                    task.cancel()
                }
            }, delay, delay)
        } else {
            object : BukkitRunnable() {
                override fun run() {
                    try {
                        val content = langFile.readText(Charsets.UTF_8)
                        if (legacyPattern.containsMatchIn(content)) {
                            plugin.logger.warning(warnMsg)
                        } else {
                            cancel()
                        }
                    } catch (e: Exception) {
                        plugin.logger.warning("Could not check language file placeholders: ${e.message}")
                        cancel()
                    }
                }
            }.runTaskTimer(plugin, delay, delay)
        }
    }
}
