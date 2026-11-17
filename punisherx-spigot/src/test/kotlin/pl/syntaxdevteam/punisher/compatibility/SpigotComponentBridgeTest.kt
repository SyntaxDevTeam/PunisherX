package pl.syntaxdevteam.punisher.compatibility

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.md_5.bungee.api.chat.BaseComponent
import kotlin.test.*

class SpigotComponentBridgeTest {
    @Test fun `Adventure 5 messages retain text color and clickable command on Spigot`() {
        val components = Component.text("Zażółć 🐉", NamedTextColor.RED)
            .clickEvent(ClickEvent.runCommand("/history Target 2"))
            .hoverEvent(HoverEvent.showText(Component.text("Next page")))
            .toSpigotComponents()
        assertEquals("Zażółć 🐉", BaseComponent.toPlainText(*components))
        val text = components.first()
        assertEquals("red", text.color.name)
        assertEquals("/history Target 2", text.clickEvent.value)
        assertNotNull(text.hoverEvent)
    }

    @Test fun `multiline and empty messages convert without serializer initialization failures`() {
        assertEquals("first\nsecond", BaseComponent.toPlainText(*Component.text("first\nsecond").toSpigotComponents()))
        assertEquals("", BaseComponent.toPlainText(*Component.empty().toSpigotComponents()))
    }
}
