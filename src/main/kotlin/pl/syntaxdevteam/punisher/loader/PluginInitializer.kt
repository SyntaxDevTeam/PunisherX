package pl.syntaxdevteam.punisher.loader

import org.bukkit.plugin.ServicePriority
import org.bukkit.scheduler.BukkitRunnable
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
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.gui.interfaces.GUIHandler
import pl.syntaxdevteam.punisher.hooks.DiscordWebhook
import pl.syntaxdevteam.punisher.hooks.HookHandler
import pl.syntaxdevteam.punisher.listeners.LegacyLoginListener
import pl.syntaxdevteam.punisher.listeners.ModernLoginListener
import pl.syntaxdevteam.punisher.listeners.PlayerJoinListener
import pl.syntaxdevteam.punisher.placeholders.PlaceholderHandler
import pl.syntaxdevteam.punisher.players.*
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
     * Sets up the database connection and creates necessary tables.
     */
    private fun setupDatabase() {
        plugin.databaseHandler = DatabaseHandler(plugin)
        if (plugin.server.name.contains("Folia")) {
            plugin.logger.debug("Detected Folia server, using sync database connection handling.")
            plugin.databaseHandler.openConnection()
            plugin.databaseHandler.createTables()
        }else{
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                plugin.databaseHandler.openConnection()
                plugin.databaseHandler.createTables()
            })
        }

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
        plugin.punishmentChecker = PunishmentChecker(plugin)
        checkLegacyPlaceholders()
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
        plugin.playerJoinListener = PlayerJoinListener(plugin.playerIPManager, plugin.punishmentChecker)
        plugin.server.pluginManager.registerEvents(plugin.playerJoinListener, plugin)
        plugin.server.pluginManager.registerEvents(plugin.punishmentChecker, plugin)
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
        if (plugin.server.name.contains("Folia", ignoreCase = true)) {
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