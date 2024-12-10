package pl.syntaxdevteam.punisher.common

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File

class ConfigHandler(private val plugin: PunisherX) {
    private var configFile: File
    private var config: FileConfiguration
    private val defaultConfig = YamlConfiguration.loadConfiguration(plugin.getResource("config.yml")!!.reader())

    init {
        copyDefaultConfig()
        configFile = File(plugin.dataFolder, "config.yml")
        config = YamlConfiguration.loadConfiguration(configFile)

        synchronizeConfig()
    }

    private fun copyDefaultConfig() {
        val file = File(plugin.dataFolder, "config.yml")
        if (!file.exists()) {
            plugin.saveResource("config.yml", false)
        }
    }

    private fun synchronizeConfig() {

        synchronizeSections(defaultConfig, config)

        if (configFile.exists()) {
            config.save(configFile)
        }
    }

    private fun synchronizeSections(defaultSection: FileConfiguration, currentSection: FileConfiguration) {
        for (key in defaultSection.getKeys(true)) {
            if (!currentSection.contains(key)) {
                currentSection[key] = defaultConfig[key]
            }
        }
    }

    fun getConfig(): FileConfiguration {
        return config
    }

    fun reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile)
        synchronizeConfig()
    }
}
