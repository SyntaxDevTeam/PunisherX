package pl.syntaxdevteam.punisher.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File

class MessageHandler(private val plugin: PunisherX) {
    private val language = plugin.config.getString("language") ?: "EN"
    private var messages: FileConfiguration

    init {
        copyDefaultMessages()
        updateLanguageFile()
        messages = loadMessages()
    }


    fun initial() {
        val author = when (language.lowercase()) {
            "pl" -> "WieszczY"
            "en" -> "Syntaxerr"
            "fr" -> "OpenAI Chat GPT-3.5"
            "es" -> "OpenAI Chat GPT-3.5"
            "de" -> "OpenAI Chat GPT-3.5"
            else -> plugin.getServerName()
        }
        plugin.logger.log("<gray>Loaded \"$language\" language file by: <white><b>$author</b></white>")
    }

    private fun copyDefaultMessages() {
        val messageFile = File(plugin.dataFolder, "lang/messages_${language.lowercase()}.yml")
        if (!messageFile.exists()) {
            messageFile.parentFile.mkdirs()
            plugin.saveResource("lang/messages_${language.lowercase()}.yml", false)
        }
    }

    private fun updateLanguageFile() {
        val langFile = File(plugin.dataFolder, "lang/messages_${language.lowercase()}.yml")
        val defaultLangStream = plugin.getResource("lang/messages_${language.lowercase()}.yml")

        if (defaultLangStream == null) {
            plugin.logger.err("Default language file for $language not found in plugin resources!")
            return
        }

        val defaultConfig = YamlConfiguration.loadConfiguration(defaultLangStream.reader())
        val currentConfig = YamlConfiguration.loadConfiguration(langFile)

        var updated = false

        fun synchronizeSections(defaultSection: ConfigurationSection, currentSection: ConfigurationSection) {
            for (key in defaultSection.getKeys(false)) {
                if (!currentSection.contains(key)) {
                    currentSection[key] = defaultSection[key]
                    updated = true
                } else if (defaultSection.isConfigurationSection(key)) {
                    synchronizeSections(
                        defaultSection.getConfigurationSection(key)!!,
                        currentSection.getConfigurationSection(key)!!
                    )
                }
            }
        }

        synchronizeSections(defaultConfig, currentConfig)

        if (updated) {
            plugin.logger.success("Updating language file: messages_${language.lowercase()}.yml with missing entries.")
            currentConfig.save(langFile)
        }
    }

    private fun loadMessages(): FileConfiguration {
        val langFile = File(plugin.dataFolder, "lang/messages_${language.lowercase()}.yml")
        return YamlConfiguration.loadConfiguration(langFile)
    }

    fun reloadMessages() {
        messages = loadMessages()
    }

    private fun getPrefix(): String {
        return messages.getString("prefix") ?: "[PunisherX] "
    }

    fun getMessage(category: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val prefix = getPrefix()
        val message = messages.getString("$category.$key") ?: run {
            plugin.logger.err("There was an error loading the message $key from category $category")
            "Message not found. Check console..."
        }
        val formattedMessage = placeholders.entries.fold(message) { acc, entry ->
            acc.replace("{${entry.key}}", entry.value)
        }
        return "$prefix $formattedMessage"
    }

    fun getLogMessage(category: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val message = messages.getString("$category.$key") ?: run {
            plugin.logger.err("There was an error loading the message $key from category $category")
            "Message not found. Check console..."
        }
        val formattedMessage = placeholders.entries.fold(message) { acc, entry ->
            acc.replace("{${entry.key}}", entry.value)
        }
        return formattedMessage
    }

    fun getComplexMessage(category: String, key: String, placeholders: Map<String, String> = emptyMap()): List<Component> {
        val messageList = messages.getStringList("$category.$key")
        if (messageList.isEmpty()) {
            plugin.logger.err("There was an error loading the message list $key from category $category")
            return listOf(Component.text("Message list not found. Check console..."))
        }
        return messageList.map { message ->
            val formattedMessage = placeholders.entries.fold(message) { acc, entry ->
                acc.replace("{${entry.key}}", entry.value)
            }
            if (formattedMessage.contains("<")) {
                MiniMessage.miniMessage().deserialize(formattedMessage)
            } else {
                LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessage)
            }
        }
    }

    fun getReasons(category: String, key: String): List<String> {
        return messages.getStringList("$category.$key")
    }
}
