package pl.syntaxdevteam.punisher.bridge.proxy

import pl.syntaxdevteam.syntaxdb.Database
import java.util.UUID

data class ActiveBan(val reason: String, val endTime: Long, val type: String = "BAN")
internal data class ConnectionIdentity(val uuid: UUID, val ip: String)

/** Current SQL state is the only authority for disconnects. No replayable messaging is accepted. */
internal object BridgeBanRepository {
    /** Only current, network-scoped UUID/IP bans can disconnect a session. */
    fun findActiveBans(db: Database, identities: List<ConnectionIdentity>): Map<UUID, ActiveBan> {
        if (identities.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        val bans = mutableMapOf<UUID, ActiveBan>()
        identities.distinctBy { it.uuid }.chunked(128).forEach { batch ->
            val uuids = batch.map { it.uuid.toString() }
            val ips = batch.map { it.ip }.distinct()
            fun placeholders(count: Int) = List(count) { "?" }.joinToString(",")
            val sql = """SELECT uuid, punishmentType, reason, endTime FROM punishments
                WHERE server = 'network' AND start <= ? AND (endTime = -1 OR endTime > ?)
                AND ((punishmentType = 'BAN' AND uuid IN (${placeholders(uuids.size)}))
                OR (punishmentType = 'BANIP' AND uuid IN (${placeholders(ips.size)})))
                ORDER BY start DESC""".trimIndent()
            val rows = db.query(sql, listOf(now, now) + uuids + ips)
            val byUuid = mutableMapOf<String, ActiveBan>()
            val byIp = mutableMapOf<String, ActiveBan>()
            rows.forEach { row ->
                val target = row.string("uuid") ?: return@forEach
                val type = row.string("punishmentType") ?: return@forEach
                val end = row.long("endTime") ?: return@forEach
                if (end != -1L && end <= now) return@forEach
                val ban = ActiveBan(row.string("reason").orEmpty(), end, type)
                when (type) {
                    "BAN" -> byUuid.putIfAbsent(target, ban)
                    "BANIP" -> byIp.putIfAbsent(target, ban)
                }
            }
            batch.forEach { identity ->
                val ban = byUuid[identity.uuid.toString()] ?: byIp[identity.ip]
                if (ban != null) bans[identity.uuid] = ban
            }
        }
        return bans
    }

}
