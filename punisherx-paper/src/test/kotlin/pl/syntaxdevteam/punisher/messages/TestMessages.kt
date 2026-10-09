package pl.syntaxdevteam.punisher.messages

import pl.syntaxdevteam.message.MessageHandler
import pl.syntaxdevteam.message.PluginMetaProvider
import pl.syntaxdevteam.message.ResourceProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/** Uses the real library; these regression tests exercise its integration rather than a parser mock. */
object TestMessages {
    private val language = """prefix: '[Test]'
messages:
  literal: '<gray>Reason: <reason></gray>'
"""
    fun create(): MessageHandler {
        val folder = kotlin.io.path.createTempDirectory("punisher-messages-test").toFile()
        folder.deleteOnExit()
        return MessageHandler(object : ResourceProvider {
            override val dataFolder: File = folder
            @Suppress("UNCHECKED_CAST")
            override fun <T> getConfigValue(path: String, default: T): T =
                if (path == "language" || path == "fallback-language") "en" as T else default
            override fun saveResource(resourcePath: String, replace: Boolean) {
                val file = File(folder, resourcePath)
                file.parentFile.mkdirs()
                if (replace || !file.exists()) file.writeText(language)
                file.deleteOnExit()
                file.parentFile.deleteOnExit()
            }
            override fun getResourceStream(resourcePath: String): InputStream? =
                if (resourcePath == "lang/messages_en.yml") ByteArrayInputStream(language.toByteArray()) else null
        }, object : PluginMetaProvider { override val name = "PunisherXTest" })
    }
}
