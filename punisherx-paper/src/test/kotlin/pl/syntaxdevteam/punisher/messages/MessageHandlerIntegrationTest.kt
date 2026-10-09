package pl.syntaxdevteam.punisher.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import pl.syntaxdevteam.message.MessageHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MessageHandlerIntegrationTest {
    private val messages = TestMessages.create()

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
