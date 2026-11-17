package pl.syntaxdevteam.punisher.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NameFormattingTest {
    private val formatter = NameFormatting(TestMessages.create())

    private fun colors(component: Component, inherited: TextColor? = null): List<Pair<Char, TextColor?>> {
        val color = component.color() ?: inherited
        return (if (component is TextComponent) component.content().map { it to color } else emptyList()) +
            component.children().flatMap { colors(it, color) }
    }

    @Test fun `trailing legacy prefix color applies to nickname`() {
        val component = formatter.displayComponent("Adminoxus", "&8[&cAdmin&8] &a", "", "")
        assertEquals("[Admin] Adminoxus", PlainTextComponentSerializer.plainText().serialize(component))
        assertEquals(NamedTextColor.GREEN, colors(component).last().second)
    }

    @Test fun `metadata color overrides prefix and suffix keeps its own color`() {
        val component = formatter.displayComponent("Adminoxus", "<gold>[Admin]</gold> &a", "#123456", " &7!")
        val colored = colors(component)
        assertEquals(TextColor.color(0x123456), colored.first { it.first == 'x' }.second)
        assertEquals(NamedTextColor.GRAY, colored.last().second)
    }

    @Test fun `section and expanded hex codes work`() {
        val component = formatter.displayComponent("User", "§x§1§2§3§4§5§6", "", "")
        assertEquals(TextColor.color(0x123456), colors(component).last().second)
        assertEquals(TextColor.color(0xabcdef), colors(formatter.renderComponent("&#abcdefHex")).last().second)
    }

    @Test fun `fallback preserves literal name and has no forced color`() {
        val component = formatter.displayComponent("Console <red>", "", "", "")
        assertEquals("Console <red>", PlainTextComponentSerializer.plainText().serialize(component))
        assertNull(component.color())
    }

    @Test fun `layout can reorder elements and add formatting`() {
        val component = formatter.displayComponent("Admin", "&c[P]", "&a", "&7[S]", "\$suffix <gold>|</gold> \$name \$prefix")
        assertEquals("[S] | Admin [P]", PlainTextComponentSerializer.plainText().serialize(component))
        assertEquals(NamedTextColor.GREEN, colors(component).first { it.first == 'A' }.second)
    }

    @Test fun `nickname aliases can omit prefix and suffix`() {
        for (token in listOf("\$name", "\$player", "\$operator")) {
            val component = formatter.displayComponent("User", "[P]", "", "[S]", token)
            assertEquals("User", PlainTextComponentSerializer.plainText().serialize(component))
        }
    }

    @Test fun `layout substitution does not interpret tokens inside metadata or nickname`() {
        val component = formatter.displayComponent("<red>\$suffix", "\$name", "", "[S]", "\$prefix \$name")
        assertEquals("\$name <red>\$suffix", PlainTextComponentSerializer.plainText().serialize(component))
    }

    @Test fun `layout supports literal dollar signs and respects token boundaries`() {
        val component = formatter.displayComponent("User", "", "", "", "\$\$name \$name \$name_extra")
        assertEquals("\$name User \$name_extra", PlainTextComponentSerializer.plainText().serialize(component))
    }
}
