package pl.syntaxdevteam.punisher.identity

import java.util.UUID
import kotlin.test.*

class PlayerIdentityTest {
    private val oldOwner = UUID.randomUUID()
    private val newOwner = UUID.randomUUID()
    @Test fun `unknown and formerly used names never create an account UUID`() {
        for (online in listOf(true, false)) {
            val directory = PlayerIdentityDirectory(IdentitySettings.create(if (online) null else "offline-test", null, online))
            assertEquals(IdentityResolution.NotFound, directory.resolve("Steve"))
            directory.joined(oldOwner, "Steve")
            assertEquals(oldOwner, (directory.resolve("steve") as IdentityResolution.Resolved).identity.uuid)
            directory.left(oldOwner)
            assertEquals(IdentityResolution.NotFound, directory.resolve("Steve"))
            directory.joined(newOwner, "Steve")
            assertEquals(newOwner, (directory.resolve("Steve") as IdentityResolution.Resolved).identity.uuid)
        }
    }
    @Test fun `duplicate live name is ambiguous and realms do not share sessions`() {
        val a = PlayerIdentityDirectory(IdentitySettings.create("network-a", "online", true))
        val b = PlayerIdentityDirectory(IdentitySettings.create("network-b", "online", true))
        a.joined(oldOwner, "Steve"); a.joined(newOwner, "STEVE")
        assertEquals(IdentityResolution.Ambiguous, a.resolve("Steve"))
        assertEquals(IdentityResolution.NotFound, b.resolve("Steve"))
    }
    @Test fun `only canonical UUID spelling is accepted and carries realm and source`() {
        val directory = PlayerIdentityDirectory(IdentitySettings.create("network-a", "trusted-proxy", false))
        val identity = (directory.resolve(oldOwner.toString()) as IdentityResolution.Resolved).identity
        assertEquals("network-a", identity.realm); assertEquals(oldOwner, identity.uuid)
        assertEquals("explicit-uuid", identity.authority)
        assertEquals(IdentityResolution.NotFound, directory.resolve("1-1-1-1-1"))
        assertEquals(IdentityResolution.NotFound, directory.resolve("2001:db8::1"))
        assertEquals("2001:db8::1", canonicalIp("2001:0db8:0:0:0:0:0:1"))
        assertNull(canonicalIp("example.com")); assertNull(canonicalIp("999.999.999.999"))
    }
    @Test fun `authority must agree with server mode and proxy trust must be explicit`() {
        assertFailsWith<IllegalArgumentException> { IdentitySettings.create(null, "online", false) }
        assertFailsWith<IllegalArgumentException> { IdentitySettings.create("BAD realm", "auto", true) }
        assertFailsWith<IllegalArgumentException> { IdentitySettings.create(null, "auto", false) }
        assertFailsWith<IllegalArgumentException> { IdentitySettings.create(null, "trusted-proxy", false) }
        assertFalse(IdentitySettings.create("offline-test", "auto", false).onlineDomain)
        assertTrue(IdentitySettings.create("network", "trusted-proxy", false).onlineDomain)
    }
}
