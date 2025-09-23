package pl.syntaxdevteam.punisher

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.HandlerList
import org.bukkit.plugin.ServicePriority
import pl.syntaxdevteam.core.SyntaxCore
import pl.syntaxdevteam.core.manager.PluginManagerX
import pl.syntaxdevteam.core.messaging.MessageHandler
import pl.syntaxdevteam.core.logging.Logger
import pl.syntaxdevteam.core.stats.StatsCollector
import pl.syntaxdevteam.core.update.GitHubSource
import pl.syntaxdevteam.core.update.ModrinthSource
import pl.syntaxdevteam.punisher.api.PunisherXApi
import pl.syntaxdevteam.punisher.api.PunisherXApiImpl
import pl.syntaxdevteam.punisher.basic.*
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.CommandLoggerPlugin
import pl.syntaxdevteam.punisher.common.ConfigHandler
import pl.syntaxdevteam.punisher.databases.*
import pl.syntaxdevteam.punisher.players.*
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.loader.PluginInitializer
import pl.syntaxdevteam.punisher.loader.VersionChecker
import pl.syntaxdevteam.punisher.listeners.PlayerJoinListener
import java.io.File
import java.util.*

class PunisherX : JavaPlugin(), Listener {
    private lateinit var pluginInitializer: PluginInitializer

    lateinit var logger: Logger
    lateinit var messageHandler: MessageHandler
    lateinit var pluginsManager: PluginManagerX

    lateinit var configHandler: ConfigHandler
    lateinit var pluginConfig: FileConfiguration
    lateinit var statsCollector: StatsCollector

    lateinit var punishmentChecker: PunishmentChecker
    lateinit var playerJoinListener: PlayerJoinListener

    lateinit var databaseHandler: DatabaseHandler
    lateinit var timeHandler: TimeHandler
    lateinit var geoIPHandler: GeoIPHandler
    lateinit var punishmentManager: PunishmentManager
    lateinit var cache: PunishmentCache
    lateinit var punisherXApi: PunisherXApi
    lateinit var hookHandler: HookHandler
    lateinit var discordWebhook: DiscordWebhook
    lateinit var commandLoggerPlugin: CommandLoggerPlugin
    lateinit var commandManager: CommandManager
    lateinit var playerIPManager: PlayerIPManager
    lateinit var versionChecker: VersionChecker


    /**
     * Called when the plugin is enabled.
     * Initializes the configuration, database, handlers, events, and commands.
     */
    override fun onEnable() {
        SyntaxCore.registerUpdateSources(
            GitHubSource("SyntaxDevTeam/PunisherX"),
            ModrinthSource("VCNRcwC2")
        )
        SyntaxCore.init(this)
        pluginInitializer = PluginInitializer(this)
        pluginInitializer.onEnable()
        versionChecker.checkAndLog()
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
        pluginInitializer.onDisable()
    }

    /**
     * Retrieves the plugin file.
     *
     * @return The plugin file.
     */
    fun getPluginFile(): File {
        return this.file
    }

    fun resolvePlayerUuid(identifier: String): UUID {
        runCatching { UUID.fromString(identifier) }.getOrNull()?.let { return it }
        Bukkit.getPlayerUniqueId(identifier)?.let { return it }
        Bukkit.getOfflinePlayerIfCached(identifier)?.uniqueId?.let { return it }
        return Bukkit.getOfflinePlayer(identifier).uniqueId
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

        saveDefaultConfig()
        reloadConfig()
        configHandler = ConfigHandler(this)
        configHandler.verifyAndUpdateConfig()

        databaseHandler = DatabaseHandler(this)
        if (server.name.contains("Folia")) {
            databaseHandler.openConnection()
            databaseHandler.createTables()
        } else {
            server.scheduler.runTaskAsynchronously(this, Runnable {
                databaseHandler.openConnection()
                databaseHandler.createTables()
            })
        }

        server.servicesManager.unregister(punisherXApi)
        punisherXApi = PunisherXApiImpl(databaseHandler)
        server.servicesManager.register(
            PunisherXApi::class.java,
            punisherXApi,
            this,
            ServicePriority.Normal
        )

        discordWebhook = DiscordWebhook(this)
        HandlerList.unregisterAll(playerJoinListener)
        HandlerList.unregisterAll(punishmentChecker)
        geoIPHandler = GeoIPHandler(this)
        playerIPManager = PlayerIPManager(this, geoIPHandler)
        punishmentChecker = PunishmentChecker(this)
        playerJoinListener = PlayerJoinListener(playerIPManager, punishmentChecker)
        server.pluginManager.registerEvents(playerJoinListener, this)
        server.pluginManager.registerEvents(punishmentChecker, this)
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
