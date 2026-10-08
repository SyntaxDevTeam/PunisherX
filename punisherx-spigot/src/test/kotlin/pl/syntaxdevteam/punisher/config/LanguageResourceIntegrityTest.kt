package pl.syntaxdevteam.punisher.config

import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LanguageResourceIntegrityTest {
    private val languages = listOf("ar", "de", "en", "es", "fr", "it", "ko", "nl", "pl", "pt", "ru", "ua")

    @Test
    fun `all bundled languages are syntactically valid YAML`() {
        languages.forEach { language ->
            val config = loadStrict(language)
            assertTrue(config.getKeys(false).isNotEmpty(), "messages_$language.yml must not be empty")
            assertNotNull(config.getString("prefix"), "messages_$language.yml must define prefix")
        }
    }

    @Test
    fun `translated values keep compatible types with English defaults`() {
        val english = loadStrict("en")
        val englishKeys = english.getKeys(true)
            .filterNot(english::isConfigurationSection)
            .toSet()

        languages.filterNot { it == "en" }.forEach { language ->
            val translation = loadStrict(language)
            val commonKeys = translation.getKeys(true)
                .filterNot(translation::isConfigurationSection)
                .filter(englishKeys::contains)

            commonKeys.forEach { path ->
                assertEquals(
                    valueKind(english.get(path)),
                    valueKind(translation.get(path)),
                    "Incompatible YAML type for '$path' in messages_$language.yml"
                )
            }
        }
    }

    private fun loadStrict(language: String): YamlConfiguration {
        val resource = "/lang/messages_$language.yml"
        val stream = assertNotNull(javaClass.getResourceAsStream(resource), "Missing $resource")
        val text = stream.use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { it.readText() }
        }
        return YamlConfiguration().apply { loadFromString(text) }
    }

    private fun valueKind(value: Any?): String = when (value) {
        is String -> "string"
        is List<*> -> "list"
        is Number -> "number"
        is Boolean -> "boolean"
        null -> "null"
        else -> value.javaClass.name
    }
}
