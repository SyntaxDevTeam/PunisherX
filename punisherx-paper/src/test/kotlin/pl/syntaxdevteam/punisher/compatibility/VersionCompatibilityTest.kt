package pl.syntaxdevteam.punisher.compatibility

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionCompatibilityTest {

    @Test
    fun `dialogs start at Minecraft 1_21_6`() {
        assertFalse(compatibilityFor("1.21.5").supports(VersionCompatibility.CompatibilityFlag.DIALOGS))
        assertTrue(compatibilityFor("1.21.6").supports(VersionCompatibility.CompatibilityFlag.DIALOGS))
        assertTrue(compatibilityFor("1.21.11").supports(VersionCompatibility.CompatibilityFlag.DIALOGS))
    }

    @Test
    fun `year based versions support dialogs`() {
        assertTrue(compatibilityFor("26.1").supports(VersionCompatibility.CompatibilityFlag.DIALOGS))
    }

    private fun compatibilityFor(version: String): VersionCompatibility {
        val checker = mock<VersionChecker>()
        whenever(checker.getSemanticVersion()).thenReturn(SemanticVersion.parse(version))
        return VersionCompatibility(checker)
    }
}
