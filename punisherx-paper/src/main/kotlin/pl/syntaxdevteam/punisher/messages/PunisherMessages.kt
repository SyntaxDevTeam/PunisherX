package pl.syntaxdevteam.punisher.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import pl.syntaxdevteam.message.MessageHandler
import pl.syntaxdevteam.punisher.PunisherX

/** Enriches the message boundary without changing plain names or the delegate's language selection. */
class PunisherMessages(private val plugin: PunisherX, private val delegate: MessageHandler) {
    private val names = NameFormatting(delegate)

    private data class Prepared(val values: Map<String, String>, val replacements: Map<String, Component>)

    private fun prepare(values: Map<String, String>): Prepared {
        val augmented = values.toMutableMap()
        val replacements = mutableMapOf<String, Component>()
        for (role in listOf("player", "operator")) {
            val name = values[role] ?: continue
            val colorKey = plugin.config.getString("name-formatting.name-color-meta", "name-color") ?: "name-color"
            val meta = plugin.nameMetadata(name, colorKey)
            val prefix = meta?.prefix.orEmpty()
            val suffix = meta?.suffix.orEmpty()
            val layout = plugin.config.getString("name-formatting.${role}-display", NameFormatting.DEFAULT_LAYOUT)
                ?: NameFormatting.DEFAULT_LAYOUT
            val components = mapOf(
                "${role}_prefix" to names.renderComponent(prefix),
                "${role}_suffix" to names.renderComponent(suffix),
                "${role}_display" to names.displayComponent(name, prefix, meta?.nameColor.orEmpty(), suffix, layout)
            )
            components.forEach { (key, component) ->
                val marker = "\uE000prx_${key}\uE001"
                augmented[key] = marker
                replacements[marker] = component
            }
        }
        return Prepared(augmented, replacements)
    }

    private fun replace(component: Component, prepared: Prepared): Component =
        prepared.replacements.entries.fold(component) { result, (marker, replacement) ->
            result.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral(marker).replacement(replacement).build())
        }

    private fun replace(text: String, prepared: Prepared, format: MessageHandler.MessageFormat): String {
        if (prepared.replacements.keys.none(text::contains)) return text
        val component = delegate.formatTextToComponent(text, format)
        return delegate.componentToString(replace(component, prepared), format)
    }

    fun stringMessageToComponentNoPrefixLiteral(category: String, key: String, placeholders: Map<String, String> = emptyMap()) =
        delegate.stringMessageToComponentNoPrefixLiteral(category, key, placeholders)

    fun getPlainText(component: Component) = delegate.getPlainText(component)
    fun getPrefix() = delegate.getPrefix()
    fun getMessageStringList(category: String, key: String) = delegate.getMessageStringList(category, key)
    fun miniMessageFormat(message: String) = delegate.miniMessageFormat(message)
    fun formatMixedTextToMiniMessage(message: String, resolver: TagResolver? = TagResolver.empty()) = delegate.formatMixedTextToMiniMessage(message, resolver)
    fun stringMessageToComponent(category: String, key: String, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToComponent(category, key, it.values), it) }
    fun stringMessageToComponent(category: String, key: String, format: MessageHandler.MessageFormat, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToComponent(category, key, format, it.values), it) }
    fun stringMessageToComponentNoPrefix(category: String, key: String, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToComponentNoPrefix(category, key, it.values), it) }
    fun stringMessageToComponentNoPrefix(category: String, key: String, format: MessageHandler.MessageFormat, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToComponentNoPrefix(category, key, format, it.values), it) }
    fun stringMessageToString(category: String, key: String, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToString(category, key, it.values), it, delegate.getMessageFormat(category, key, true)) }
    fun stringMessageToString(category: String, key: String, format: MessageHandler.MessageFormat, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToString(category, key, format, it.values), it, format) }
    fun stringMessageToStringNoPrefix(category: String, key: String, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToStringNoPrefix(category, key, it.values), it, delegate.getMessageFormat(category, key, false)) }
    fun stringMessageToStringNoPrefix(category: String, key: String, format: MessageHandler.MessageFormat, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { replace(delegate.stringMessageToStringNoPrefix(category, key, format, it.values), it, format) }
    fun getSmartMessage(category: String, key: String, placeholders: Map<String, String> = emptyMap()) =
        prepare(placeholders).let { prepared -> delegate.getSmartMessage(category, key, prepared.values).map { replace(it, prepared) } }
}
