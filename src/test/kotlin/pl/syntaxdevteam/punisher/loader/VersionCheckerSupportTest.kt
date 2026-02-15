package pl.syntaxdevteam.punisher.loader

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionCheckerSupportTest {

    @Test
    fun `supports approved legacy versions`() {
        assertTrue(VersionChecker.isVersionSupported("1.20.6"))
        assertTrue(VersionChecker.isVersionSupported("1.21"))
        assertTrue(VersionChecker.isVersionSupported("1.21.0"))
        assertTrue(VersionChecker.isVersionSupported("1.21.11"))
    }

    @Test
    fun `supports approved year-based versions`() {
        assertTrue(VersionChecker.isVersionSupported("26.1"))
        assertTrue(VersionChecker.isVersionSupported("26.1.0"))
    }

    @Test
    fun `rejects versions that are not explicitly approved`() {
        assertFalse(VersionChecker.isVersionSupported("1.20.5"))
        assertFalse(VersionChecker.isVersionSupported("1.22"))
        assertFalse(VersionChecker.isVersionSupported("26.0"))
        assertFalse(VersionChecker.isVersionSupported("26.2"))
        assertFalse(VersionChecker.isVersionSupported("27.1"))
    }
}
