package pl.syntaxdevteam.punisher.loader

import org.bukkit.plugin.ServicePriority
import pl.syntaxdevteam.core.SyntaxCore
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.api.PunisherXApi
import pl.syntaxdevteam.punisher.api.PunisherXApiImpl
import pl.syntaxdevteam.punisher.basic.PunishmentCache
import pl.syntaxdevteam.punisher.basic.PunishmentChecker
import pl.syntaxdevteam.punisher.basic.PunishmentManager
import pl.syntaxdevteam.punisher.basic.TimeHandler
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.CommandLoggerPlugin
import pl.syntaxdevteam.punisher.common.ConfigHandler
import pl.syntaxdevteam.punisher.common.UUIDManager
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.listeners.LegacyLoginListener
import pl.syntaxdevteam.punisher.listeners.ModernLoginListener
import pl.syntaxdevteam.punisher.placeholders.PlaceholderHandler
import pl.syntaxdevteam.punisher.players.GeoIPHandler
import pl.syntaxdevteam.punisher.players.PlayerIPManager

class PluginInitializer(private val plugin: PunisherX) {

    fun onEnable() {
        setUpLogger()
        setupConfig()
        setupUUID()
        setupDatabase()
        setupHandlers()
        registerEvents()
        registerCommands()
        checkForUpdates()
    }

    fun onDisable() {
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
    private fun setupConfig() {
        plugin.saveDefaultConfig()
        plugin.configHandler = ConfigHandler(plugin)
        plugin.configHandler.verifyAndUpdateConfig()
    }

    /**
     * Setup players uuid manager
     *
     */
    private fun setupUUID() {
        plugin.uuidManager = UUIDManager(plugin)
    }

    /**
     * Sets up the database connection and creates necessary tables.
     */
    private fun setupDatabase() {
        plugin.databaseHandler = DatabaseHandler(plugin)
        plugin.databaseHandler.openConnection()
        plugin.databaseHandler.createTables()
    }

    /**
     * Initializes various handlers used by the plugin.
     */
    private fun setupHandlers() {
        plugin.messageHandler = SyntaxCore.messages
        plugin.pluginsManager = SyntaxCore.pluginManagerx
        plugin.timeHandler = TimeHandler(plugin)
        plugin.punishmentManager = PunishmentManager()
        plugin.geoIPHandler = GeoIPHandler(plugin)
        plugin.cache = PunishmentCache(plugin)
        plugin.punisherXApi = PunisherXApiImpl(plugin.databaseHandler)
        plugin.hookHandler = HookHandler(plugin)
        plugin.discordWebhook = DiscordWebhook(plugin)
        plugin.playerIPManager = PlayerIPManager(plugin, plugin.geoIPHandler)
    }

    /**
     * Registers the plugin commands.
     */
    private fun registerCommands(){
        plugin.commandLoggerPlugin = CommandLoggerPlugin(plugin)
        plugin.commandManager = CommandManager(plugin)
        plugin.commandManager.registerCommands()
    }

    /**
     * Registers the plugin events.
     */
    private fun registerEvents() {
        plugin.server.pluginManager.registerEvents(plugin.playerIPManager, plugin)
        plugin.server.pluginManager.registerEvents(PunishmentChecker(plugin), plugin)
        plugin.versionChecker = VersionChecker(plugin)
        if (plugin.versionChecker.isAtLeast("1.21.7")) {
            plugin.server.pluginManager.registerEvents(ModernLoginListener(plugin), plugin)
            plugin.logger.debug("Registered ModernLoginListener for 1.21.7+")
        } else {
            plugin.server.pluginManager.registerEvents(LegacyLoginListener(plugin), plugin)
            plugin.logger.debug("Registered LegacyLoginListener for pre-1.21.7")
        }
        plugin.server.servicesManager.register(PunisherXApi::class.java, plugin.punisherXApi, plugin, ServicePriority.Normal)
        if (plugin.hookHandler.checkPlaceholderAPI()) {
            PlaceholderHandler(plugin).register()
        }
    }

    /**
     * Checks for updates to the plugin.
     */
    private fun checkForUpdates() {
        plugin.statsCollector = SyntaxCore.statsCollector
        SyntaxCore.updateChecker.checkAsync()
    }
}