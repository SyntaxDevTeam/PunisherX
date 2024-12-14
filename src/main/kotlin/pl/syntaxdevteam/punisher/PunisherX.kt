package pl.syntaxdevteam.punisher

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import pl.syntaxdevteam.punisher.basic.*
import pl.syntaxdevteam.punisher.commands.CommandManager
import pl.syntaxdevteam.punisher.common.*
import pl.syntaxdevteam.punisher.databases.*
import pl.syntaxdevteam.punisher.players.*
import java.io.File
import java.util.*


@Suppress("UnstableApiUsage", "unused")
class PunisherX : JavaPlugin(), Listener {
    val pluginMetas = this.pluginMeta
    private val configHandler by lazy { ConfigHandler(this) }
    private val config: FileConfiguration = configHandler.getConfig()
    private var debugMode = config.getBoolean("debug")
    var logger: Logger = Logger(pluginMetas, debugMode)
    private val language = config.getString("language") ?: "EN"
    private lateinit var pluginsManager: PluginManager
    private lateinit var statsCollector: StatsCollector
    lateinit var databaseHandler: DatabaseHandler
    lateinit var messageHandler: MessageHandler
    lateinit var timeHandler: TimeHandler
    lateinit var punishmentManager: PunishmentManager
    private lateinit var updateChecker: UpdateChecker
    private var geoIPHandler: GeoIPHandler = GeoIPHandler(this)
    var playerIPManager: PlayerIPManager = PlayerIPManager(this, geoIPHandler)
    val uuidManager = UUIDManager(this)
    private lateinit var commandManager: CommandManager

    override fun onEnable() {
        setupConfig()
        setupDatabase()
        setupHandlers()
        registerEvents()
        registerCommands()
        checkForUpdates()
    }

    fun onReload() {
        reloadMyConfig()
    }

    override fun onDisable() {
        databaseHandler.closeConnection()
        AsyncChatEvent.getHandlerList().unregister(this as Plugin)
        logger.err(pluginMetas.name + " " + pluginMetas.version + " has been disabled ☹️")
    }

    private fun setupConfig() {
        configHandler.updateConfig()
    }

    private fun setupDatabase() {
        databaseHandler = DatabaseHandler(this, config)
        databaseHandler.openConnection()
        databaseHandler.createTables()
    }

    private fun setupHandlers() {
        messageHandler = MessageHandler(this, pluginMetas).apply { initial() }
        timeHandler = TimeHandler(this, pluginMetas)
        punishmentManager = PunishmentManager()
        geoIPHandler = GeoIPHandler(this)
        pluginsManager = PluginManager(this)

    }

    private fun registerCommands(){
        commandManager = CommandManager(this)
        commandManager.registerCommands()
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(playerIPManager, this)
        server.pluginManager.registerEvents(PunishmentChecker(this), this)
    }

    private fun checkForUpdates() {
        statsCollector = StatsCollector(this)
        updateChecker = UpdateChecker(this, pluginMetas, config)
        updateChecker.checkForUpdates()
    }

    fun getPluginFile(): File {
        return this.file
    }

    fun reloadMyConfig() {
        databaseHandler.closeConnection()
        try {
            configHandler.reloadConfig()
            messageHandler.reloadMessages()
        } catch (e: Exception) {
            logger.err(messageHandler.getMessage("error", "reload") + e.message)
        }
        databaseHandler = DatabaseHandler(this, this.config)
        databaseHandler.openConnection()
        databaseHandler.createTables()

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
                logger.debug("Właściwość 'server-name' nie została znaleziona w pliku server.properties.")
            }
        } else {
            logger.debug("Plik server.properties nie istnieje.")
        }
        return "Unknown Server"
    }

    fun getPunishmentHistory(player: Player): List<PunishmentData> {
        val uuid = uuidManager.getUUID(player.name)
        return databaseHandler.getLastTenPunishments(uuid.toString())
    }
}
