package pl.syntaxdevteam.punisher

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import pl.syntaxdevteam.punisher.api.PunisherXApi
import pl.syntaxdevteam.punisher.basic.*
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.*
import pl.syntaxdevteam.punisher.databases.*
import pl.syntaxdevteam.punisher.players.*
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.loader.PluginInitializer
import pl.syntaxdevteam.punisher.loader.VersionChecker
import java.io.File
import java.util.*

class PunisherX : JavaPlugin(), Listener {
    private val config: FileConfiguration = getConfig()
    var logger: Logger = Logger(this, config.getBoolean("debug"))
    lateinit var configHandler: ConfigHandler
    lateinit var databaseHandler: DatabaseHandler
    lateinit var messageHandler: MessageHandler
    lateinit var timeHandler: TimeHandler
    lateinit var geoIPHandler: GeoIPHandler
    lateinit var punishmentManager: PunishmentManager
    lateinit var pluginsManager: PluginManager
    lateinit var cache: PunishmentCache
    lateinit var punisherXApi: PunisherXApi
    lateinit var hookHandler: HookHandler
    lateinit var discordWebhook: DiscordWebhook
    lateinit var commandLoggerPlugin: CommandLoggerPlugin
    lateinit var commandManager: CommandManager
    lateinit var playerIPManager: PlayerIPManager
    lateinit var statsCollector: StatsCollector
    lateinit var updateChecker: UpdateChecker
    lateinit var uuidManager: UUIDManager
    lateinit var versionChecker: VersionChecker
    lateinit var pluginInitializer: PluginInitializer

    override fun onLoad() {
        versionChecker = VersionChecker(this)
        versionChecker.checkAndLog()
    }
    /**
     * Called when the plugin is enabled.
     * Initializes the configuration, database, handlers, events, and commands.
     */
    override fun onEnable() {
        pluginInitializer = PluginInitializer(this)
        pluginInitializer.onEnable()
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
}
