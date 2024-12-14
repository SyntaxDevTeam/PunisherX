package pl.syntaxdevteam.punisher.common

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.InputStreamReader

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
     * Copies the default `config.yml` file to the plugin folder
     * if the file does not already exist.
     */
    private fun copyDefaultConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
    }

    /**
     * Synchronizes the configuration with the default `config.yml` file.
     * Missing keys and values are added, while specific sections are excluded.
     */
    private fun synchronizeConfig() {
        val exclusions = setOf("mute_cmd", "database")
        synchronizeSections(defaultConfig, config, exclusions)
        saveConfig()
    }

    /**
     * Adds missing keys and values from the default configuration to the current configuration,
     * ignoring keys specified in the exclusions list.
     *
     * @param defaultSection The default configuration (source of missing keys and values).
     * @param currentSection The current configuration (target for synchronization).
     * @param exclusions Keys to be excluded during synchronization.
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
     */
    private fun saveConfig() {
        config.save(configFile)
    }

    /**
     * Returns the current configuration as a `FileConfiguration` object.
     *
     * @return The current configuration.
     */
    fun getConfig(): FileConfiguration = config

    /**
     * Reloads the configuration from the `config.yml` file and synchronizes it
     * with the default configuration.
     */
    fun reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile)
        synchronizeConfig()
    }

    /**
     * Updates the configuration based on the default one, accounting for changes in the new
     * `config.yml` file and ignoring selected sections.
     */
    fun updateConfig() {
        val exclusions = setOf("mute_cmd", "database")
        synchronizeSections(defaultConfig, config, exclusions)
        saveConfig()
    }
}
