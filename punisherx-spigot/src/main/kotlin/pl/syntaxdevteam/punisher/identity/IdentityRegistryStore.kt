package pl.syntaxdevteam.punisher.identity

import java.sql.Connection
import java.sql.SQLException
import java.util.Locale
import java.util.UUID

/** Observation registry, deliberately not a nickname resolver. No punishment-history imports. */
internal object IdentityRegistryStore {
    fun bind(connection: Connection, settings: IdentitySettings) {
        val domain = settings.realm + if (settings.onlineDomain) ":online" else ":offline"
        try {
            connection.prepareStatement("INSERT INTO player_identity_domain (id, domain) SELECT 1, ? WHERE NOT EXISTS (SELECT 1 FROM player_identity_domain WHERE id = 1)").use {
                it.setString(1, domain); it.executeUpdate()
            }
        } catch (error: SQLException) {
            // Concurrent first start: accept only an independently confirmed matching binding.
            if (boundDomain(connection) != domain) throw error
        }
        check(boundDomain(connection) == domain) { "Identity domain mismatch: use a separate database for different realms or online/offline namespaces" }
    }

    private fun boundDomain(connection: Connection): String? =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT domain FROM player_identity_domain WHERE id = 1").use { if (it.next()) it.getString(1) else null }
        }

    /** Normalize only active IP targets. Account UUIDs and immutable punishment history remain untouched. */
    fun normalizeActiveAddresses(connection: Connection) {
        val previous = connection.autoCommit
        connection.autoCommit = false
        try {
            val rows = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id, uuid FROM punishments WHERE punishmentType = 'BANIP'").use { rows ->
                    buildList { while (rows.next()) add(rows.getLong(1) to rows.getString(2)) }
                }
            }
            for ((id, raw) in rows) {
                val canonical = raw?.let(::canonicalIp) ?: continue
                if (canonical != raw) connection.prepareStatement("UPDATE punishments SET uuid = ? WHERE id = ? AND uuid = ? AND punishmentType = 'BANIP'").use {
                    it.setString(1, canonical); it.setLong(2, id); it.setString(3, raw); it.executeUpdate()
                }
            }
            val pending = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id, target FROM bridge_events WHERE action = 'BANIP' AND processed = 0").use { rows ->
                    buildList { while (rows.next()) add(rows.getLong(1) to rows.getString(2)) }
                }
            }
            for ((id, raw) in pending) {
                val canonical = raw?.let(::canonicalIp) ?: continue
                if (canonical != raw) connection.prepareStatement("UPDATE bridge_events SET target = ? WHERE id = ? AND target = ? AND action = 'BANIP' AND processed = 0").use {
                    it.setString(1, canonical); it.setLong(2, id); it.setString(3, raw); it.executeUpdate()
                }
            }
            connection.commit()
        } catch (error: Exception) { connection.rollback(); throw error }
        finally { connection.autoCommit = previous }
    }

    fun record(connection: Connection, identity: PlayerIdentity, observedAt: Long) {
        require(observedAt >= 0)
        val name = requireNotNull(identity.canonicalName)
        require(name.matches(Regex("[A-Za-z0-9_]{3,16}")))
        repeat(5) {
            val previous = connection.prepareStatement("SELECT last_confirmed_at, version FROM player_identities WHERE realm_id = ? AND player_uuid = ?").use {
                it.setString(1, identity.realm); it.setString(2, identity.uuid.toString())
                it.executeQuery().use { rows -> if (rows.next()) rows.getLong(1) to rows.getLong(2) else null }
            }
            if (previous != null && previous.first >= observedAt) return
            val inserted = previous == null
            try {
                val sql = if (inserted) "INSERT INTO player_identities (canonical_name, normalized_name, authority, last_confirmed_at, version, realm_id, player_uuid) VALUES (?, ?, ?, ?, 1, ?, ?)"
                    else "UPDATE player_identities SET canonical_name = ?, normalized_name = ?, authority = ?, last_confirmed_at = ?, version = version + 1 WHERE realm_id = ? AND player_uuid = ? AND version = ?"
                val changed = connection.prepareStatement(sql).use {
                    it.setString(1, name); it.setString(2, name.lowercase(Locale.ROOT)); it.setString(3, identity.authority)
                    it.setLong(4, observedAt); it.setString(5, identity.realm); it.setString(6, identity.uuid.toString())
                    if (previous != null) it.setLong(7, previous.second)
                    it.executeUpdate()
                }
                if (changed == 1) return
            } catch (error: SQLException) {
                if (!inserted || it == 4) throw error
                // A concurrent insert won. The next CAS attempt reads that row; never overwrite blindly.
            }
        }
        error("Concurrent identity observation updates did not settle")
    }
}
