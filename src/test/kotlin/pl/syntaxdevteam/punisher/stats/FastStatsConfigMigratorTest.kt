package pl.syntaxdevteam.punisher.stats

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastStatsConfigMigratorTest {
    @Test
    fun `migrates FastStats 0_23 config without changing existing choices`() {
        val directory = Files.createTempDirectory("faststats-migration-test")
        val config = directory.resolve("config.properties")
        Files.writeString(
            config,
            """
            serverId=65e2016d-d098-41ff-afb5-348ef3b8ae8c
            enabled=false
            submitErrors=false
            submitAdditionalMetrics=false
            debug=true
            """.trimIndent()
        )

        FastStatsConfigMigrator.migrate(config)

        val properties = loadProperties(config)
        assertEquals("1", properties.getProperty("configVersion"))
        assertEquals("true", properties.getProperty("submitMetrics"))
        assertEquals("false", properties.getProperty("enabled"))
        assertEquals("false", properties.getProperty("submitErrors"))
        assertEquals("false", properties.getProperty("submitAdditionalMetrics"))
        assertEquals("true", properties.getProperty("debug"))
        assertEquals("65e2016d-d098-41ff-afb5-348ef3b8ae8c", properties.getProperty("serverId"))
    }

    @Test
    fun `does not overwrite FastStats 0_25 properties`() {
        val directory = Files.createTempDirectory("faststats-current-config-test")
        val config = directory.resolve("config.properties")
        Files.writeString(config, "configVersion=1\nsubmitMetrics=false\nenabled=true\n")

        FastStatsConfigMigrator.migrate(config)

        val properties = loadProperties(config)
        assertEquals("1", properties.getProperty("configVersion"))
        assertEquals("false", properties.getProperty("submitMetrics"))
        assertEquals("true", properties.getProperty("enabled"))
    }

    @Test
    fun `does not modify configuration from a newer FastStats version`() {
        val directory = Files.createTempDirectory("faststats-future-config-test")
        val config = directory.resolve("config.properties")
        val original = "configVersion=2\nenabled=true\n"
        Files.writeString(config, original)

        FastStatsConfigMigrator.migrate(config)

        assertEquals(original, Files.readString(config))
    }

    @Test
    fun `does not create shared config on first FastStats run`() {
        val directory = Files.createTempDirectory("faststats-first-run-test")
        val config = directory.resolve("faststats").resolve("config.properties")

        FastStatsConfigMigrator.migrate(config)

        assertFalse(Files.exists(config))
        assertTrue(Files.exists(directory))
    }

    private fun loadProperties(path: Path): Properties = Properties().apply {
        Files.newBufferedReader(path, Charsets.UTF_8).use(::load)
    }
}
