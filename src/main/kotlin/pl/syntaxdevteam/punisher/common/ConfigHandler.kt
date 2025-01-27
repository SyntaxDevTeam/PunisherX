package pl.syntaxdevteam.punisher.common

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.LoaderOptions
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader

class ConfigHandler(private val plugin: PunisherX, private val fileName: String) {
    private val configFile: File = File(plugin.dataFolder, fileName)
    private val yaml: Yaml

    init {
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }
        val loaderOptions = LoaderOptions()
        val representer = Representer(options)
        yaml = Yaml(Constructor(loaderOptions), representer, options)
    }

    private fun loadConfig(): List<String> {
        return FileReader(configFile).use { reader ->
            reader.readLines()
        }
    }

    private fun saveConfig(config: List<String>) {
        FileWriter(configFile).use { writer ->
            config.forEach { line ->
                writer.write(line + "\n")
            }
        }
    }

    fun verifyAndUpdateConfig() {
        val defaultConfigStream = plugin.getResource(fileName)
        if (defaultConfigStream != null) {
            val defaultConfig = InputStreamReader(defaultConfigStream).readLines()
            val currentConfig = loadConfig().toMutableList()

            for (line in defaultConfig) {
                if (!currentConfig.contains(line)) {
                    currentConfig.add(line)
                }
            }

            saveConfig(currentConfig)
        }
    }
}
