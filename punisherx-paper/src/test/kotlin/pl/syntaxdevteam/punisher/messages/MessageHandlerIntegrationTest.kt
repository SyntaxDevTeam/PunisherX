package pl.syntaxdevteam.punisher.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import pl.syntaxdevteam.message.MessageHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessageHandlerIntegrationTest {
    private val messages = TestMessages.create()

    @Test fun `unmark placeholders render in all bundled languages`() {
        for (language in listOf("ar", "de", "en", "es", "fr", "it", "ko", "nl", "pl", "pt", "ru", "ua")) {
            val text = assertNotNull(javaClass.getResourceAsStream("/lang/messages_$language.yml"))
                .bufferedReader().use { it.readText() }
            val handler = TestMessages.create(text)
            val values = mapOf("mark" to "check:test", "count" to "42", "player" to "TestPlayer")
            for ((key, required) in mapOf(
                "paused" to listOf("mark"),
                "success" to listOf("mark", "count", "player"),
                "silent_success" to listOf("mark", "count", "player"),
                "pause_success" to listOf("mark", "count"),
                "resumed" to listOf("mark")
            )) {
                val rendered = handler.getPlainText(handler.stringMessageToComponent("unmark", key, values))
                required.forEach { placeholder ->
                    assertTrue(values.getValue(placeholder) in rendered, "$language/$key: $rendered")
                    assertFalse("{$placeholder}" in rendered, "$language/$key: $rendered")
                    assertFalse("<$placeholder>" in rendered, "$language/$key: $rendered")
                }
            }
        }
    }

    @Test fun `book payload remains literal including tags backslashes and legacy colors`() {
        val reason = "<red>&c reason \\ <click:run_command:'/op User'>click</click>"
        val rendered = messages.stringMessageToComponentNoPrefixLiteral("messages", "literal", mapOf("reason" to reason))
        assertEquals("Reason: $reason", messages.getPlainText(rendered))
        assertEquals(NamedTextColor.GRAY, rendered.color())
        fun hasClick(component: Component): Boolean = component.clickEvent() != null || component.children().any(::hasClick)
        assertFalse(hasClick(rendered))
    }

    @Test fun `handler serializes display components in every supported format`() {
        val component = NameFormatting(messages).displayComponent("User", "&c[P] ", "#123456", " &7[S]")
        for (format in MessageHandler.MessageFormat.entries) {
            val serialized = messages.componentToString(component, format)
            val parsed = messages.formatTextToComponent(serialized, format)
            assertEquals("[P] User [S]", messages.getPlainText(parsed), format.name)
        }
    }
}
