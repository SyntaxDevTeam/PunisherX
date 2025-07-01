package pl.syntaxdevteam.punisher

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import pl.syntaxdevteam.punisher.api.PunisherXApi
import pl.syntaxdevteam.punisher.api.PunisherXApiImpl
import pl.syntaxdevteam.punisher.basic.*
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.*
import pl.syntaxdevteam.punisher.databases.*
import pl.syntaxdevteam.punisher.players.*
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.listeners.LegacyLoginListener
import pl.syntaxdevteam.punisher.listeners.ModernLoginListener
import pl.syntaxdevteam.punisher.loader.VersionChecker
import pl.syntaxdevteam.punisher.placeholders.PlaceholderHandler
import java.io.File
import java.util.*

class PunisherX : JavaPlugin(), Listener {
    private val config: FileConfiguration = getConfig()
    var logger: Logger = Logger(this, config.getBoolean("debug"))
    lateinit var pluginsManager: PluginManager
    private lateinit var statsCollector: StatsCollector
    lateinit var databaseHandler: DatabaseHandler
    lateinit var messageHandler: MessageHandler
    lateinit var timeHandler: TimeHandler
    lateinit var punishmentManager: PunishmentManager
    lateinit var updateChecker: UpdateChecker
    private var geoIPHandler: GeoIPHandler = GeoIPHandler(this)
    var playerIPManager: PlayerIPManager = PlayerIPManager(this, geoIPHandler)
    val uuidManager = UUIDManager(this)
    private lateinit var commandManager: CommandManager
    lateinit var cache: PunishmentCache
    private var configHandler = ConfigHandler(this)
    lateinit var commandLoggerPlugin: CommandLoggerPlugin
    lateinit var punisherXApi: PunisherXApi
    lateinit var discordWebhook: DiscordWebhook
    lateinit var hookHandler: HookHandler
    lateinit var versionChecker: VersionChecker

    override fun onLoad() {
        versionChecker = VersionChecker(this)
        versionChecker.checkAndLog()
    }
    /**
     * Called when the plugin is enabled.
     * Initializes the configuration, database, handlers, events, and commands.
     */
    override fun onEnable() {
        setupConfig()
        setupDatabase()
        setupHandlers()
        registerEvents()
        registerCommands()
        checkForUpdates()
    }

    /**
     * Called when the plugin is reloaded.
     * Reloads the configuration and reinitializes the database connection.
     */
    fun onReload() {
        reloadMyConfig()
    }

    /**
     * Called when the plugin is disabled.
     * Closes the database connection and unregisters events.
     */
    override fun onDisable() {
        databaseHandler.closeConnection()
        AsyncChatEvent.getHandlerList().unregister(this as Plugin)
        logger.err(this.pluginMeta.name + " " + this.pluginMeta.version + " has been disabled ☹️")
    }

    /**
     * Sets up the plugin configuration.
     */
    private fun setupConfig() {
        saveDefaultConfig()
        configHandler.verifyAndUpdateConfig()
    }

    /**
     * Sets up the database connection and creates necessary tables.
     */
    private fun setupDatabase() {
        databaseHandler = DatabaseHandler(this)
        databaseHandler.openConnection()
        databaseHandler.createTables()
    }

    /**
     * Initializes various handlers used by the plugin.
     */
    private fun setupHandlers() {
        messageHandler = MessageHandler(this).apply { initial() }
        timeHandler = TimeHandler(this)
        punishmentManager = PunishmentManager()
        geoIPHandler = GeoIPHandler(this)
        pluginsManager = PluginManager(this)
        cache = PunishmentCache(this)
        punisherXApi = PunisherXApiImpl(databaseHandler)
        hookHandler = HookHandler(this)
        discordWebhook = DiscordWebhook(this)
    }

    /**
     * Registers the plugin commands.
     */
    private fun registerCommands(){
        commandLoggerPlugin = CommandLoggerPlugin(this)
        commandManager = CommandManager(this)
        commandManager.registerCommands()
    }

    /**
     * Registers the plugin events.
     */
    private fun registerEvents() {
        server.pluginManager.registerEvents(playerIPManager, this)
        server.pluginManager.registerEvents(PunishmentChecker(this), this)
        if (versionChecker.isAtLeast("1.21.7")) {
            server.pluginManager.registerEvents(ModernLoginListener(this), this)
            logger.debug("Registered ModernLoginListener for 1.21.7+")
        } else {
            server.pluginManager.registerEvents(LegacyLoginListener(this), this)
            logger.debug("Registered LegacyLoginListener for pre-1.21.7")
        }
        server.servicesManager.register(PunisherXApi::class.java, punisherXApi, this, ServicePriority.Normal)
        if (hookHandler.checkPlaceholderAPI()) {
            PlaceholderHandler(this).register()
        }
    }

    /**
     * Checks for updates to the plugin.
     */
    private fun checkForUpdates() {
        statsCollector = StatsCollector(this)
        updateChecker = UpdateChecker(this)
        updateChecker.checkForUpdates()
    }

    /**
     * Retrieves the plugin file.
     *
     * @return The plugin file.
     */
    fun getPluginFile(): File {
        return this.file
    }

    /**
     * Reloads the plugin configuration and reinitializes the database connection.
     */
    private fun reloadMyConfig() {
        databaseHandler.closeConnection()
        try {
            messageHandler.reloadMessages()
        } catch (e: Exception) {
            logger.err("${messageHandler.getMessage("error", "reload")} ${e.message}")
        }
        databaseHandler = DatabaseHandler(this)
        databaseHandler.openConnection()
        databaseHandler.createTables()
    }

    /**
     * Retrieves the server name from the server.properties file.
     *
     * @return The server name, or "Unknown Server" if not found.
     */
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

    /**
     * Retrieves punishment history for a given player.
     *
     * @deprecated This method is deprecated and will be removed in version 2.0.0.
     * Please migrate to {@link pl.syntaxdevteam.punisher.api.PunisherXApi#getPunishmentHistory}
     * which provides the following improvements:
     * - Fully asynchronous execution using CompletableFuture, reducing main-thread lag.
     * - Improved caching, minimizing redundant database queries.
     * - Supports filtering by punishment type (e.g., WARN, MUTE, JAIL, BAN).
     * - Optimized for large datasets, ensuring better scalability.
     *
     * Migration example:
     * ```kotlin
     * // Old implementation (deprecated)
     * val history: List<PunishmentData> = plugin.getPunishmentHistory(player)
     *
     * // New implementation using PunisherXApi
     * val future: CompletableFuture<List<PunishmentData>> = plugin.punisherXApi
     *     .getPunishmentHistory()
     *     .thenApply { punishments ->
     *         punishments.filter { it.playerUUID == player.uniqueId.toString() }
     *     }
     * ```
     *
     * @see pl.syntaxdevteam.punisher.api.PunisherXApi
     * @see pl.syntaxdevteam.punisher.api.PunisherXApiImpl
     * @see java.util.concurrent.CompletableFuture
     *
     * @param player The player whose punishment history should be retrieved
     * @return A list of [PunishmentData] objects representing the player's punishment history
     */
    @Deprecated(
        message = "Use PunisherXApi#getPunishmentHistory for improved async operations and filtering",
        replaceWith = ReplaceWith(
            expression = "punisherXApi.getPunishmentHistory(uuid.toString())",
            imports = ["pl.syntaxdevteam.punisher.api.PunisherXApi"]
        ),
        level = DeprecationLevel.WARNING
    )
    fun getPunishmentHistory(player: Player): List<PunishmentData> {
        val uuid = uuidManager.getUUID(player.name)
        return databaseHandler.getLastTenPunishments(uuid.toString())
    }
}
