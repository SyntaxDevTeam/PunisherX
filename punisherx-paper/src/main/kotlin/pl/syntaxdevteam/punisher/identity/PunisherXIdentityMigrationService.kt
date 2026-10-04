package pl.syntaxdevteam.punisher.identity

import pl.syntaxdevteam.punisher.PunisherX
import java.util.UUID

data class PunisherXIdentityMigrationResult(
    val status: String,
    val reasonCode: String,
    val legacyEvidence: Boolean,
)

class PunisherXIdentityMigrationService(private val plugin: PunisherX) {
    fun inspect(migrationId: UUID, sourceUuid: UUID, targetUuid: UUID): PunisherXIdentityMigrationResult {
        if (plugin.cache.identityMigrationEvidence(sourceUuid) && plugin.cache.identityMigrationTargetExists(targetUuid)) {
            return PunisherXIdentityMigrationResult(
                "BLOCKED",
                "PUNISHERX_JAIL_CACHE_TARGET_COLLISION",
                true,
            )
        }
        val database = plugin.databaseHandler.inspectIdentityMigration(migrationId, sourceUuid, targetUuid)
        if (database.status == PunisherIdentityMigrationStatus.BLOCKED) return database.external()
        val ipEvidence = plugin.playerIPManager.identityMigrationEvidence(sourceUuid)
        val jailEvidence = plugin.cache.identityMigrationEvidence(sourceUuid)
        val evidence = database.legacyEvidence || ipEvidence > 0 || jailEvidence
        return PunisherXIdentityMigrationResult(
            if (evidence) "READY" else "NO_DATA",
            "${database.reasonCode}_IP_${ipEvidence}_JAIL_${if (jailEvidence) 1 else 0}",
            evidence,
        )
    }

    fun migrate(migrationId: UUID, sourceUuid: UUID, targetUuid: UUID): PunisherXIdentityMigrationResult {
        val inspection = inspect(migrationId, sourceUuid, targetUuid)
        if (inspection.status == "BLOCKED") return inspection
        var databaseMigrated = false
        var ipMigrated = false
        var jailMigrated = false
        try {
            val database = plugin.databaseHandler.migrateIdentity(migrationId, sourceUuid, targetUuid)
            if (database.status == PunisherIdentityMigrationStatus.BLOCKED ||
                database.status == PunisherIdentityMigrationStatus.FAILURE
            ) return database.external()
            databaseMigrated = database.status == PunisherIdentityMigrationStatus.SUCCESS
            ipMigrated = plugin.playerIPManager.migrateIdentityCache(migrationId, sourceUuid, targetUuid) > 0
            jailMigrated = plugin.cache.migrateIdentityCache(migrationId, sourceUuid, targetUuid)
            return PunisherXIdentityMigrationResult(
                if (databaseMigrated || ipMigrated || jailMigrated) "SUCCESS" else "NO_DATA",
                inspection.reasonCode,
                inspection.legacyEvidence,
            )
        } catch (failure: Throwable) {
            if (jailMigrated) runCatching { plugin.cache.rollbackIdentityCache(migrationId) }
            if (ipMigrated) runCatching { plugin.playerIPManager.rollbackIdentityCache(migrationId) }
            if (databaseMigrated) {
                runCatching {
                    plugin.databaseHandler.rollbackIdentityMigration(migrationId, sourceUuid, targetUuid)
                }
            }
            throw failure
        }
    }

    fun rollback(migrationId: UUID, sourceUuid: UUID, targetUuid: UUID): PunisherXIdentityMigrationResult {
        val jail = runCatching { plugin.cache.rollbackIdentityCache(migrationId) }.getOrDefault(false)
        val ip = runCatching { plugin.playerIPManager.rollbackIdentityCache(migrationId) }.getOrDefault(false)
        val database = plugin.databaseHandler.rollbackIdentityMigration(migrationId, sourceUuid, targetUuid)
        return when (database.status) {
            PunisherIdentityMigrationStatus.BLOCKED,
            PunisherIdentityMigrationStatus.FAILURE -> database.external()
            else -> PunisherXIdentityMigrationResult(
                "ROLLED_BACK",
                "${database.reasonCode}_IP_${if (ip) 1 else 0}_JAIL_${if (jail) 1 else 0}",
                database.legacyEvidence || ip || jail,
            )
        }
    }

    private fun PunisherIdentityMigrationBridgeResult.external() = PunisherXIdentityMigrationResult(
        status.name,
        reasonCode,
        legacyEvidence,
    )
}
