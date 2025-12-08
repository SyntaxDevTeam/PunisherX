package pl.syntaxdevteam.punisher.gui.materials

import org.bukkit.Material
import pl.syntaxdevteam.punisher.loader.SemanticVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class GuiMaterialResolverTest {

    private fun resolverFor(version: String, matcher: (String) -> Material?): GuiMaterialResolver {
        return GuiMaterialResolver(SemanticVersion.parse(version), materialMatcher = matcher)
    }

    @Test
    fun `prefers timeline candidates before explicit fallbacks`() {
        val resolver = resolverFor("1.20.6") { key ->
            when (key) {
                "CHAIN" -> Material.IRON_BARS
                "IRON_BARS" -> Material.IRON_BARS
                else -> null
            }
        }

        val resolved = resolver.resolveMaterial("CHAIN", "IRON_BARS")

        assertEquals(Material.IRON_BARS, resolved)
    }

    @Test
    fun `uses fallback when primary variant is unavailable`() {
        val resolver = resolverFor("1.21.10") { key ->
            when (key) {
                "IRON_BARS" -> Material.IRON_BARS
                else -> null
            }
        }

        val resolved = resolver.resolveMaterial("IRON_CHAIN", "IRON_BARS", "CHAIN")

        assertEquals(Material.IRON_BARS, resolved)
    }

    @Test
    fun `falls back to explicit candidates when no timeline entry exists`() {
        val resolver = resolverFor("1.21.10") { key ->
            when (key) {
                "IRON_BARS" -> Material.IRON_BARS
                else -> null
            }
        }

        val resolved = resolver.resolveMaterial("MISSING_MATERIAL", "IRON_BARS")

        assertEquals(Material.IRON_BARS, resolved)
    }
}
