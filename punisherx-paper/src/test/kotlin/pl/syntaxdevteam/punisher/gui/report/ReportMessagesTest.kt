package pl.syntaxdevteam.punisher.gui.report

import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReportMessagesTest {

    private val bundledLanguages = listOf("ar", "de", "en", "es", "fr", "it", "ko", "nl", "pl", "pt", "ru", "ua")

    private val requiredGuiKeys = listOf(
        "GUI.Report.menu.title",
        "GUI.Report.menu.online",
        "GUI.Report.menu.offline",
        "GUI.Report.player.title",
        "GUI.Report.offline.title",
        "GUI.Report.reason.title",
        "GUI.Report.lore.clickToReport",
        "GUI.Report.lore.clickToChoose"
    )

    private val requiredReportKeys = listOf(
        "reports.gui-title", "reports.gui-details", "reports.gui-history", "reports.admin-usage",
        "reports.inbox-title", "reports.history-title", "reports.empty", "reports.details-title",
        "reports.not-found", "reports.already-closed", "reports.handled", "reports.status-open",
        "reports.status-resolved", "reports.status-rejected", "reports.usage",
        "reports.cannot-report-self", "reports.already-submitted", "reports.already-reported-target",
        "reports.report-limit-reached", "reports.invalid-reason",
        "reports.no-targets", "reports.invalid-form", "reports.report-sent", "reports.admin-notify",
        "reports.dialog-title", "reports.dialog-body", "reports.dialog-body-target",
        "reports.dialog-target-label", "reports.dialog-reason-label", "reports.dialog-submit",
        "reports.dialog-cancel", "reports.dialog-list-entry", "reports.dialog-details-body",
        "reports.dialog-resolution-body", "punishment-dialog.close", "punishment-dialog.entry",
        "punishment-dialog.tooltip", "punishment-dialog.details"
    )

    @Test
    fun `report GUI messages exist in bundled languages`() {
        bundledLanguages.forEach { language ->
            val resource = "/lang/messages_${language}.yml"
            val stream = assertNotNull(javaClass.getResourceAsStream(resource), "Missing $resource")
            val messages = stream.use { YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)) }

            (requiredGuiKeys + requiredReportKeys).forEach { key ->
                val value = messages.getString(key)
                assertNotNull(value, "Missing $key in $resource")
                assertFalse(value.isBlank(), "Empty $key in $resource")
            }
        }
    }

    @Test
    fun `bundled languages preserve Polish runtime placeholders`() {
        val polish = loadLanguage("pl")
        val mismatches = mutableListOf<String>()
        val runtimePlaceholders = setOf(
            "player", "ip", "world", "locationx", "locationy", "locationz", "radius",
            "reason", "time", "id", "message", "warn_no", "template", "level", "type",
            "uuid", "operator", "server", "reporter", "target", "count", "limit", "page",
            "reporter_uuid", "target_uuid", "filed_at", "handled_by", "handled_at", "note",
            "servername", "daily", "tps", "onlineplayers", "totalplayers", "onlinestr",
            "totalstr", "lastactive", "playerip", "punishments", "punishstr", "geo",
            "lastseen", "lastlocation", "logout", "offlinetime", "punishment", "date",
            "start", "end", "list"
        )
        bundledLanguages.filterNot { it == "pl" }.forEach { language ->
            val translation = loadLanguage(language)
            (requiredGuiKeys + requiredReportKeys)
                .filterNot { it.endsWith("usage") }
                .forEach { key ->
                    val expected = placeholders(polish.get(key), runtimePlaceholders)
                    val actual = placeholders(translation.get(key), runtimePlaceholders)
                    if (expected != actual) {
                        mismatches += "messages_$language.yml: $key expected=$expected actual=$actual"
                    }
                }
        }
        assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
    }

    private fun loadLanguage(language: String): YamlConfiguration {
        val resource = "/lang/messages_${language}.yml"
        val stream = assertNotNull(javaClass.getResourceAsStream(resource), "Missing $resource")
        return stream.use { YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)) }
    }

    private fun placeholders(value: Any?, allowed: Set<String>): Set<String> {
        val strings = when (value) {
            is String -> listOf(value)
            is List<*> -> value.filterIsInstance<String>()
            else -> emptyList()
        }
        return strings.flatMap { text ->
            Regex("<([A-Za-z_][A-Za-z0-9_-]*)>").findAll(text).map { it.groupValues[1] }.toList()
        }.filterTo(mutableSetOf()) { it in allowed }
    }
}
