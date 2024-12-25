package pl.syntaxdevteam.punisher.common

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.InputStreamReader

/**
 * Handles the plugin's configuration, including loading, synchronizing, and saving
 * the `config.yml` file. Ensures that the current configuration stays consistent with
 * the default configuration provided in the plugin's resources.
 *
 * This class is designed to handle common configuration-related tasks, such as:
 * - Copying the default `config.yml` if it does not exist.
 * - Adding missing keys and values to the current configuration.
 * - Reloading and updating the configuration dynamically.
 *
 * @property plugin The main plugin instance used to access resources and the plugin's data folder.
 * @constructor Initializes the ConfigHandler and ensures the configuration is ready for use.
 */

class ConfigHandler(private val plugin: PunisherX) {

    private val configFile: File = File(plugin.dataFolder, "config.yml")
    private var config: FileConfiguration = YamlConfiguration.loadConfiguration(configFile)
    private val defaultConfig: FileConfiguration = YamlConfiguration.loadConfiguration(
        InputStreamReader(plugin.getResource("config.yml")!!)
    )

    init {
        copyDefaultConfig()
        synchronizeConfig()
    }

    /**
     * Copies the default `config.yml` file from the plugin's resources to the plugin's data folder.
     *
     * This method checks whether the `config.yml` file already exists and only performs the copy
     * if the file is missing. It is typically called during initialization to ensure the plugin
     * has a valid configuration file to work with.
     */
    private fun copyDefaultConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
    }

    /**
     * Synchronizes the current configuration file with the default `config.yml` file.
     *
     * This method adds any missing keys and values from the default configuration
     * while preserving existing ones in the current configuration. Specific sections
     * can be excluded from synchronization by using the `exclusions` set.
     *
     * This ensures that the configuration stays up-to-date with new default settings
     * introduced in plugin updates without overwriting user-specific changes.
     */
    private fun synchronizeConfig() {
        val exclusions = setOf("mute_cmd", "database")
        synchronizeSections(defaultConfig, config, exclusions)
        saveConfig()
    }

    /**
     * Synchronizes sections of the current configuration with the default configuration.
     *
     * Missing keys and values from the default configuration are added to the current configuration,
     * unless they are specified in the `exclusions` set. This method supports synchronizing nested
     * configurations and lists while ensuring no user-defined settings are overwritten.
     *
     * @param defaultSection The source configuration containing default keys and values.
     * @param currentSection The target configuration to be updated.
     * @param exclusions A set of keys to be excluded from the synchronization process.
     */
    private fun synchronizeSections(
        defaultSection: FileConfiguration,
        currentSection: FileConfiguration,
        exclusions: Set<String> = emptySet()
    ) {
        for (key in defaultSection.getKeys(true)) {
            if (exclusions.contains(key)) continue

            when (val defaultValue = defaultSection.get(key)) {
                is List<*> -> {
                    val currentList = currentSection.getStringList(key)
                    if (currentList.isEmpty()) {
                        currentSection[key] = defaultValue
                    } else {
                        val missingItems = defaultValue.filterNot { currentList.contains(it) }
                        if (missingItems.isNotEmpty()) {
                            currentList.addAll(missingItems.map { it.toString() })
                            currentSection[key] = currentList
                        }
                    }
                }
                else -> {
                    if (!currentSection.contains(key)) {
                        currentSection[key] = defaultValue
                    }
                }
            }
        }
    }

    /**
     * Saves the current configuration to the `config.yml` file.
     *
     * This method writes any changes made to the configuration object back to the
     * `config.yml` file in the plugin's data folder, ensuring that updates persist
     * across plugin restarts.
     */
    private fun saveConfig() {
        config.save(configFile)
    }

    /**
     * Retrieves the current configuration.
     *
     * @return The current `FileConfiguration` object, representing the in-memory state
     *         of the plugin's configuration.
     */
    fun getConfig(): FileConfiguration = config

    /**
     * Reloads the configuration from the `config.yml` file.
     *
     * This method reloads the configuration into memory and ensures it is synchronized
     * with the default configuration. It is useful for dynamically applying changes
     * made to the configuration file while the plugin is running.
     */
    fun reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile)
        synchronizeConfig()
    }

    /**
     * Updates the current configuration by comparing it with the default configuration.
     *
     * This method checks for new keys or values introduced in the default `config.yml`
     * file and adds them to the current configuration, while excluding keys specified
     * in the `exclusions` set.
     *
     * Typically used during plugin updates to ensure the configuration file stays
     * up-to-date without overwriting user-defined changes.
     */
    fun updateConfig() {
        val exclusions = setOf("mute_cmd", "database")
        synchronizeSections(defaultConfig, config, exclusions)
        saveConfig()
    }
}
