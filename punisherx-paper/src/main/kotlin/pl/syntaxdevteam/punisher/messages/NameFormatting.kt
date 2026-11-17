package pl.syntaxdevteam.punisher.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import pl.syntaxdevteam.message.MessageHandler

/** Expands the configured layout. MessageHandler owns all text parsing and color conversion. */
class NameFormatting(private val messages: MessageHandler) {
    companion object {
        const val DEFAULT_LAYOUT = "\$prefix\$name\$suffix"
        private val layoutToken = Regex("""\$\$|\$(prefix|suffix|name|player|operator)(?![A-Za-z0-9_])""")
    }

    fun renderComponent(text: String): Component = messages.formatRichTextToComponent(text)

    fun displayComponent(
        name: String,
        prefix: String,
        color: String,
        suffix: String,
        layout: String = DEFAULT_LAYOUT
    ): Component {
        val hex = color.removePrefix("#")
        val nameColor = when {
            hex.length == 6 && hex.all { it.digitToIntOrNull(16) != null } -> "<#$hex>"
            color.isNotEmpty() && color.all { it.isLetter() || it == '_' } -> "<$color>"
            else -> color
        }
        val formatted = layoutToken.replace(layout) { token ->
            when (token.groupValues[1]) {
                "prefix" -> prefix
                "suffix" -> suffix
                "name", "player", "operator" -> "$nameColor<prx_literal_name>"
                else -> "$"
            }
        }
        return messages.formatRichTextToComponent(formatted, Placeholder.unparsed("prx_literal_name", name))
    }
}
