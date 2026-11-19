package pl.syntaxdevteam.punisher.identity

import java.sql.DriverManager
import java.util.UUID
import kotlin.test.*
import java.sql.Connection

class IdentityRegistryStoreTest {
    private fun database(block: (Connection) -> Unit) {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { c ->
            c.createStatement().use { s ->
                s.execute("CREATE TABLE player_identity_domain (id INTEGER PRIMARY KEY, domain TEXT NOT NULL)")
                s.execute("CREATE TABLE player_identities (realm_id TEXT, player_uuid TEXT, canonical_name TEXT, normalized_name TEXT, authority TEXT, last_confirmed_at BIGINT, version BIGINT, PRIMARY KEY(realm_id, player_uuid))")
                s.execute("CREATE TABLE bridge_events (id INTEGER PRIMARY KEY, target TEXT, action TEXT, processed INTEGER)")
                s.execute("CREATE TABLE punishments (id INTEGER PRIMARY KEY, uuid TEXT, punishmentType TEXT)")
            }
            block(c)
        }
    }
    @Test fun `database binding forbids another realm or online-offline domain`() = database { c ->
        val settings = IdentitySettings.create("network", "online", true)
        IdentityRegistryStore.bind(c, settings); IdentityRegistryStore.bind(c, settings)
        assertFailsWith<IllegalStateException> { IdentityRegistryStore.bind(c, IdentitySettings.create("network", "offline", false)) }
        assertFailsWith<IllegalStateException> { IdentityRegistryStore.bind(c, IdentitySettings.create("other", "online", true)) }
    }
    @Test fun `legacy offline migration seeds scope without changing punishment UUIDs`() = database { c ->
        val uuid = UUID.randomUUID().toString()
        c.createStatement().use { it.execute("INSERT INTO punishments VALUES(1, '$uuid', 'BAN')") }
        val a = IdentityRegistryStore.resolveAndBind(c, null, "auto", false, "survival")
        assertEquals("survival", a.realm)
        assertEquals("offline", a.authority)
        val b = IdentityRegistryStore.resolveAndBind(c, "", "auto", false, "skyblock")
        assertEquals(a, b)
        c.createStatement().use { s -> s.executeQuery("SELECT uuid FROM punishments WHERE id = 1").use {
            assertTrue(it.next()); assertEquals(uuid, it.getString(1))
        } }
    }
    @Test fun `existing explicit realm is inherited without changing binding`() = database { c ->
        val expected = IdentitySettings.create("moonvale", "offline", false)
        IdentityRegistryStore.bind(c, expected)
        assertEquals(expected, IdentityRegistryStore.resolveAndBind(c, "", "auto", false, "network"))
        assertFailsWith<IllegalStateException> {
            IdentityRegistryStore.resolveAndBind(c, "another", "auto", false, "network")
        }
        assertFailsWith<IllegalStateException> {
            IdentityRegistryStore.resolveAndBind(c, "", "auto", true, "network")
        }
        assertEquals(expected, IdentityRegistryStore.resolveAndBind(c, null, "offline", false, "network"))
    }
    @Test fun `standalone online keeps previous default and proxy trust is never inferred`() = database { c ->
        val online = IdentityRegistryStore.resolveAndBind(c, null, "auto", true, "survival")
        assertEquals("online", online.realm)
        assertTrue(online.onlineDomain)
        assertFailsWith<IllegalStateException> {
            IdentityRegistryStore.resolveAndBind(c, null, "auto", false, "survival")
        }
        val proxy = IdentityRegistryStore.resolveAndBind(c, null, "trusted-proxy", false, "skyblock")
        assertEquals("online", proxy.realm)
        assertEquals("trusted-proxy", proxy.authority)
    }
    @Test fun `invalid server scope does not break automatic migration`() = database { c ->
        assertEquals("network", IdentityRegistryStore.resolveAndBind(c, "", "auto", false, "invalid scope").realm)
    }
    @Test fun `older asynchronous observations cannot overwrite later names and same nick never merges accounts`() = database { c ->
        val uuid = UUID.randomUUID()
        val identity = PlayerIdentity("network", uuid, "NewName", "online")
        IdentityRegistryStore.record(c, identity, 20)
        IdentityRegistryStore.record(c, identity.copy(canonicalName = "OldName"), 10)
        IdentityRegistryStore.record(c, identity.copy(canonicalName = "TieName"), 20)
        IdentityRegistryStore.record(c, identity.copy(canonicalName = "LatestName"), 30)
        IdentityRegistryStore.record(c, identity.copy(uuid = UUID.randomUUID(), canonicalName = "LatestName"), 40)
        c.createStatement().use { s -> s.executeQuery("SELECT canonical_name, version FROM player_identities WHERE player_uuid = '$uuid'").use {
            assertTrue(it.next()); assertEquals("LatestName", it.getString(1)); assertEquals(2, it.getInt(2))
        } }
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM player_identities").use { it.next(); assertEquals(2, it.getInt(1)) } }
    }
    @Test fun `IPv6 migration only normalizes active IP targets and leaves account identifiers intact`() = database { c ->
        val uuid = UUID.randomUUID().toString()
        c.createStatement().use {
            it.execute("INSERT INTO punishments VALUES(1, '2001:0db8:0:0:0:0:0:1', 'BANIP')")
            it.execute("INSERT INTO punishments VALUES(2, '$uuid', 'BAN')")
        }
        IdentityRegistryStore.normalizeActiveAddresses(c)
        c.createStatement().use { s -> s.executeQuery("SELECT uuid FROM punishments ORDER BY id").use {
            assertTrue(it.next()); assertEquals("2001:db8::1", it.getString(1))
            assertTrue(it.next()); assertEquals(uuid, it.getString(1))
        } }
    }
}
