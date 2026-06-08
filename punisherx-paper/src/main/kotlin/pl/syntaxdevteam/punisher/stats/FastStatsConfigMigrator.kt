package pl.syntaxdevteam.punisher.stats

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

internal object FastStatsConfigMigrator {
    private const val CURRENT_CONFIG_VERSION = 1

    fun migrate(configPath: Path) {
        if (!Files.isRegularFile(configPath)) {
            return
        }

        val originalContent = Files.readString(configPath, Charsets.UTF_8)
        val properties = Properties()
        originalContent.reader().use(properties::load)

        val configVersion = properties.getProperty("configVersion")?.trim()?.toIntOrNull()
        if (configVersion != null && configVersion > CURRENT_CONFIG_VERSION) {
            return
        }

        val additions = buildList {
            if (!properties.containsKey("configVersion")) {
                add("configVersion=$CURRENT_CONFIG_VERSION")
            }
            if (!properties.containsKey("submitMetrics")) {
                add("submitMetrics=true")
            }
        }
        if (additions.isEmpty()) {
            return
        }

        val separator = if (originalContent.isEmpty() || originalContent.endsWith('\n')) "" else System.lineSeparator()
        val migratedContent = originalContent + separator + additions.joinToString(
            System.lineSeparator(),
            postfix = System.lineSeparator()
        )
        val temporaryFile = Files.createTempFile(configPath.parent, "config", ".properties.tmp")
        try {
            Files.writeString(temporaryFile, migratedContent, Charsets.UTF_8)
            try {
                Files.move(
                    temporaryFile,
                    configPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporaryFile, configPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
    }
}
