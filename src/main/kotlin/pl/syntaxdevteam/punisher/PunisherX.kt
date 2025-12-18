package pl.syntaxdevteam.punisher

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import pl.syntaxdevteam.core.SyntaxCore
import pl.syntaxdevteam.core.manager.PluginManagerX
import pl.syntaxdevteam.core.logging.Logger
import pl.syntaxdevteam.core.stats.StatsCollector
import pl.syntaxdevteam.core.update.GitHubSource
import pl.syntaxdevteam.core.update.ModrinthSource
import pl.syntaxdevteam.message.MessageHandler
import pl.syntaxdevteam.punisher.api.PunisherXApi
import pl.syntaxdevteam.punisher.basic.*
import pl.syntaxdevteam.punisher.common.PunishmentActionExecutor
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.CommandLoggerPlugin
import pl.syntaxdevteam.punisher.common.ConfigManager
import pl.syntaxdevteam.punisher.compatibility.VersionCompatibility
import pl.syntaxdevteam.punisher.databases.*
import pl.syntaxdevteam.punisher.players.*
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.gui.materials.GuiMaterialResolver
import pl.syntaxdevteam.punisher.loader.PluginInitializer
import pl.syntaxdevteam.punisher.loader.VersionChecker
import pl.syntaxdevteam.punisher.listeners.PlayerJoinListener
import pl.syntaxdevteam.punisher.platform.SchedulerAdapter
import pl.syntaxdevteam.punisher.bridge.OnlinePunishmentWatcher
import pl.syntaxdevteam.punisher.bridge.ProxyBridgeMessenger
import pl.syntaxdevteam.punisher.teleport.SafeTeleportService
import java.io.File
import java.util.*

class PunisherX : JavaPlugin(), Listener {
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
    lateinit var actionExecutor: PunishmentActionExecutor
    lateinit var schedulerAdapter: SchedulerAdapter
    lateinit var safeTeleportService: SafeTeleportService
    lateinit var cfg: ConfigManager
    lateinit var proxyBridgeMessenger: ProxyBridgeMessenger
    lateinit var onlinePunishmentWatcher: OnlinePunishmentWatcher


    /**
     * Called when the plugin is enabled.
     * Initializes the configuration, database, handlers, events, and commands.
     */
    override fun onEnable() {
        SyntaxCore.registerUpdateSources(
            GitHubSource("SyntaxDevTeam/PunisherX"),
            ModrinthSource("VCNRcwC2")
        )
        SyntaxCore.init(this, versionType = "paper")
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
        runCatching { proxyBridgeMessenger.unregisterChannel() }
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
        pluginInitializer.onDisable()
        server.scheduler.cancelTasks(this)
        runCatching { proxyBridgeMessenger.unregisterChannel() }
        HandlerList.unregisterAll(this as Plugin)
        reloadConfig()
        pluginInitializer = PluginInitializer(this)
        pluginInitializer.onEnable()
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
