package pl.syntaxdevteam.punisher

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import pl.syntaxdevteam.punisher.basic.*
import pl.syntaxdevteam.punisher.commands.*
import pl.syntaxdevteam.punisher.common.*
import pl.syntaxdevteam.punisher.databases.*
import pl.syntaxdevteam.punisher.players.*
import java.io.File
import java.util.*


@Suppress("UnstableApiUsage", "unused")
class PunisherX : JavaPlugin(), Listener {
    lateinit var logger: Logger
    val pluginMetas = this.pluginMeta
    private var config = getConfig()
    private var debugMode = config.getBoolean("debug")
    private val language = config.getString("language") ?: "EN"
    private lateinit var pluginManager: PluginManager
    private lateinit var statsCollector: StatsCollector
    lateinit var databaseHandler: DatabaseHandler
    lateinit var messageHandler: MessageHandler
    lateinit var timeHandler: TimeHandler
    lateinit var punishmentManager: PunishmentManager
    private lateinit var updateChecker: UpdateChecker
    lateinit var playerIPManager: PlayerIPManager
    private lateinit var geoIPHandler: GeoIPHandler
    val uuidManager = UUIDManager(this)

    override fun onLoad() {
        logger = Logger(pluginMetas, debugMode)
    }

    override fun onEnable() {
        saveDefaultConfig()
        databaseHandler = DatabaseHandler(this, this.config)
        databaseHandler.openConnection()
        databaseHandler.createTables()
        messageHandler = MessageHandler(this, pluginMetas)
        messageHandler.initial()
        timeHandler = TimeHandler(this, pluginMetas)
        punishmentManager = PunishmentManager()
        geoIPHandler = GeoIPHandler(this)
        playerIPManager = PlayerIPManager(this, geoIPHandler)
        server.pluginManager.registerEvents(playerIPManager, this)
        val manager: LifecycleEventManager<Plugin> = this.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands: Commands = event.registrar()
            commands.register("punisherx", "PunisherX plugin command. Type /punisherx help to check available commands", PunishesXCommands(this))
            commands.register("prx", "PunisherX plugin command. Type /prx help to check available commands", PunishesXCommands(this))
            commands.register("check", "Checking player penalties" + messageHandler.getMessage("check", "usage"), CheckCommand(this, pluginMetas, playerIPManager))
            commands.register("history", "Checking player all penalties history" + messageHandler.getMessage("history", "usage"), HistoryCommand(this, pluginMetas, playerIPManager))
            commands.register("kick", messageHandler.getMessage("kick", "usage"), KickCommand(this, pluginMetas))
            commands.register("warn", messageHandler.getMessage("warn", "usage"), WarnCommand(this, pluginMetas))
            commands.register("unwarn", messageHandler.getMessage("unwarn", "usage"), UnWarnCommand(this, pluginMetas))
            commands.register("mute", messageHandler.getMessage("mute", "usage"), MuteCommand(this, pluginMetas))
            commands.register("unmute", messageHandler.getMessage("mute", "usage"),
                UnMuteCommand(this, pluginMetas)
            )
            commands.register("ban", messageHandler.getMessage("ban", "usage"), BanCommand(this, pluginMetas))
            commands.register("banip", messageHandler.getMessage("banip", "usage"), BanIpCommand(this, pluginMetas))
            commands.register("unban", messageHandler.getMessage("ban", "usage"), UnBanCommand(this, pluginMetas))
            commands.register("change-reason", messageHandler.getMessage("change-reason", "usage"), ChangeReasonCommand(this, pluginMetas))
            commands.register("clearall", messageHandler.getMessage("clear", "usage"), ClearAllCommand(this))
            val aliases = config.getConfigurationSection("aliases")
            aliases?.getKeys(false)?.forEach { key ->
                val commandName = aliases.getString(key) ?: key
                when (key) {
                    "check" -> commands.register(commandName, "Checking player penalties" + messageHandler.getMessage("check", "usage"), CheckCommand(this, pluginMetas, playerIPManager))
                    "history" -> commands.register(commandName, "Checking player all penalties history" + messageHandler.getMessage("history", "usage"), HistoryCommand(this, pluginMetas, playerIPManager))
                    "kick" -> commands.register(commandName, messageHandler.getMessage("kick", "usage"), KickCommand(this, pluginMetas))
                    "warn" -> commands.register(commandName, messageHandler.getMessage("warn", "usage"), WarnCommand(this, pluginMetas))
                    "unwarn" -> commands.register(commandName, messageHandler.getMessage("unwarn", "usage"), UnWarnCommand(this, pluginMetas))
                    "mute" -> commands.register(commandName, messageHandler.getMessage("mute", "usage"), MuteCommand(this, pluginMetas))
                    "unmute" -> commands.register(commandName, messageHandler.getMessage("mute", "usage"),
                        UnMuteCommand(this, pluginMetas)
                    )
                    "ban" -> commands.register(commandName, messageHandler.getMessage("ban", "usage"), BanCommand(this, pluginMetas))
                    "banip" -> commands.register(commandName, messageHandler.getMessage("banip", "usage"), BanIpCommand(this, pluginMetas))
                    "unban" -> commands.register(commandName, messageHandler.getMessage("ban", "usage"), UnBanCommand(this, pluginMetas))
                    "change-reason" -> commands.register(commandName, messageHandler.getMessage("change-reason", "usage"), ChangeReasonCommand(this, pluginMetas))
                    "clearall" -> commands.register(commandName, messageHandler.getMessage("clear", "usage"), ClearAllCommand(this))
                }
            }
        }

        server.pluginManager.registerEvents(PunishmentChecker(this), this)
        pluginManager = PluginManager(this)

        val author = when (language.lowercase()) {
            "pl" -> "WieszczY"
            "en" -> "Syntaxerr"
            "fr" -> "OpenAI Chat GPT-3.5"
            "es" -> "OpenAI Chat GPT-3.5"
            "de" -> "OpenAI Chat GPT-3.5"
            else -> getServerName()
        }
        logger.log("<gray>Loaded \"$language\" language file by: <white><b>$author</b></white>")
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
            super.reloadConfig()
            messageHandler.reloadMessages()
        } catch (e: Exception) {
            logger.err(messageHandler.getMessage("error", "reload") + e.message)
        }
        databaseHandler = DatabaseHandler(this, this.config)
        databaseHandler.openConnection()
        databaseHandler.createTables()

    }

    override fun onDisable() {
        databaseHandler.closeConnection()
        AsyncChatEvent.getHandlerList().unregister(this as Plugin)
        logger.err(pluginMetas.name + " " + pluginMetas.version + " has been disabled ☹️")
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
