package pl.syntaxdevteam.punisher.templates

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File

data class PunishTemplateLevel(
    val level: Int,
    val type: String,
    val time: String?
)

data class PunishTemplate(
    val name: String,
    val reason: String,
    val levels: Map<Int, PunishTemplateLevel>
) {
    fun resolveLevel(requestedLevel: Int): PunishTemplateLevel? {
        if (levels.isEmpty()) return null
        levels[requestedLevel]?.let { return it }
        val fallback = levels.keys.filter { it <= requestedLevel }.maxOrNull() ?: return null
        return levels[fallback]
    }
}

class PunishTemplateManager(private val plugin: PunisherX) {
    companion object {
        private const val FILE_NAME = "punish-templates.yml"
        private const val ROOT_PATH = "templates"
    }

    lateinit var config: YamlDocument

    fun load() {
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()
        val dataFile = File(plugin.dataFolder, FILE_NAME)
        if (dataFile.exists()) {
            config = YamlDocument.create(
                dataFile,
                GeneralSettings.builder().setUseDefaults(false).build(),
                LoaderSettings.builder().setAutoUpdate(false).build(),
                DumperSettings.builder().setIndentation(2).build()
            )
            return
        }

        val defaults = plugin.getResource(FILE_NAME)
            ?: error("Missing $FILE_NAME in resources.")
        config = YamlDocument.create(
            dataFile,
            defaults,
            GeneralSettings.builder().setUseDefaults(true).build(),
            LoaderSettings.builder().setAutoUpdate(true).build(),
            DumperSettings.builder().setIndentation(2).build()
        )
        config.save()
    }

    fun reload() {
        load()
    }

    fun getTemplateNames(): List<String> {
        val section = config.getOptionalSection(ROOT_PATH).orElse(null) ?: return emptyList()
        return section.keys.mapNotNull { it?.toString() }
    }

    fun getTemplate(name: String): PunishTemplate? {
        val section = config.getOptionalSection(ROOT_PATH).orElse(null) ?: return null
        val templateKey = section.keys
            .mapNotNull { it?.toString() }
            .firstOrNull { it.equals(name, ignoreCase = true) }
            ?: return null

        val templateSection = section.getOptionalSection(templateKey).orElse(null) ?: return null
        val reason = templateSection.getString("reason") ?: return null
        val levels = parseLevels(templateSection)
        return PunishTemplate(templateKey, reason, levels)
    }

    private fun parseLevels(templateSection: Section): Map<Int, PunishTemplateLevel> {
        val levels = LinkedHashMap<Int, PunishTemplateLevel>()
        for (key in templateSection.keys) {
            val keyString = key?.toString() ?: continue
            if (keyString.equals("reason", ignoreCase = true)) {
                continue
            }
            val level = keyString.toIntOrNull() ?: continue
            val levelSection = templateSection.getOptionalSection(keyString).orElse(null) ?: continue
            val type = levelSection.getString("punish") ?: continue
            val time = levelSection.getString("time")
            levels[level] = PunishTemplateLevel(level, type, time)
        }
        return levels
    }
}
