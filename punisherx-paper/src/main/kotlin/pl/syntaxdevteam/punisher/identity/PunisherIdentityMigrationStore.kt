package pl.syntaxdevteam.punisher.identity

import java.sql.Connection
import java.util.UUID

internal enum class PunisherIdentityMigrationStatus { READY, NO_DATA, BLOCKED, SUCCESS, FAILURE, ROLLED_BACK }

internal data class PunisherIdentityMigrationBridgeResult(
    val status: PunisherIdentityMigrationStatus,
    val reasonCode: String,
    val legacyEvidence: Boolean = false,
)

internal object PunisherIdentityMigrationStore {
    private const val PREPARED = "PREPARED"
    private const val COMPLETED = "COMPLETED"
    private const val ROLLED_BACK = "ROLLED_BACK"

    fun migrateSchema(connection: Connection) {
        listOf(
            """CREATE TABLE IF NOT EXISTS punisherx_identity_migrations (
                migration_id VARCHAR(36) PRIMARY KEY,
                source_uuid VARCHAR(36) NOT NULL,
                target_uuid VARCHAR(36) NOT NULL,
                state VARCHAR(24) NOT NULL
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS punisherx_identity_migration_items (
                migration_id VARCHAR(36) NOT NULL,
                entity_type VARCHAR(32) NOT NULL,
                entity_key VARCHAR(64) NOT NULL,
                PRIMARY KEY (migration_id, entity_type, entity_key)
            )""".trimIndent(),
        ).forEach { sql -> connection.createStatement().use { it.execute(sql) } }
    }

    fun inspect(connection: Connection, migrationId: UUID, source: UUID, target: UUID): PunisherIdentityMigrationBridgeResult {
        require(source != target)
        migrateSchema(connection)
        val existing = migration(connection, migrationId)
        if (existing != null && (existing.source != source || existing.target != target)) {
            return PunisherIdentityMigrationBridgeResult(
                PunisherIdentityMigrationStatus.BLOCKED,
                "PUNISHERX_MIGRATION_ID_CONFLICT",
            )
        }
        val active = count(connection, "SELECT COUNT(*) FROM punishments WHERE uuid = ?", source)
        val history = count(connection, "SELECT COUNT(*) FROM punishmenthistory WHERE uuid = ?", source)
        val reporter = count(connection, "SELECT COUNT(*) FROM reports WHERE player = ?", source)
        val suspect = count(connection, "SELECT COUNT(*) FROM reports WHERE suspect = ?", source)
        val bridge = count(connection, "SELECT COUNT(*) FROM bridge_events WHERE target = ?", source)
        val journal = existing?.let { itemCount(connection, migrationId) } ?: 0
        val evidence = active + history + reporter + suspect + bridge + journal > 0
        return PunisherIdentityMigrationBridgeResult(
            if (evidence) PunisherIdentityMigrationStatus.READY else PunisherIdentityMigrationStatus.NO_DATA,
            "PUNISHERX_ACTIVE_${active}_HISTORY_${history}_REPORTER_${reporter}_SUSPECT_${suspect}_BRIDGE_${bridge}",
            evidence,
        )
    }

    fun migrate(connection: Connection, migrationId: UUID, source: UUID, target: UUID): PunisherIdentityMigrationBridgeResult =
        transaction(connection) {
            migrateSchema(connection)
            val existing = migration(connection, migrationId)
            if (existing != null) {
                if (existing.source != source || existing.target != target) {
                    return@transaction PunisherIdentityMigrationBridgeResult(
                        PunisherIdentityMigrationStatus.BLOCKED,
                        "PUNISHERX_MIGRATION_ID_CONFLICT",
                    )
                }
                if (existing.state == COMPLETED) {
                    return@transaction PunisherIdentityMigrationBridgeResult(
                        PunisherIdentityMigrationStatus.SUCCESS,
                        "PUNISHERX_ALREADY_MIGRATED",
                        true,
                    )
                }
                clearItems(connection, migrationId)
                updateState(connection, migrationId, PREPARED)
            } else {
                connection.prepareStatement(
                    "INSERT INTO punisherx_identity_migrations(migration_id, source_uuid, target_uuid, state) VALUES (?, ?, ?, ?)",
                ).use {
                    it.setString(1, migrationId.toString())
                    it.setString(2, source.toString())
                    it.setString(3, target.toString())
                    it.setString(4, PREPARED)
                    it.executeUpdate()
                }
            }

            val inspection = inspect(connection, migrationId, source, target)
            if (!inspection.legacyEvidence) {
                updateState(connection, migrationId, COMPLETED)
                return@transaction PunisherIdentityMigrationBridgeResult(
                    PunisherIdentityMigrationStatus.NO_DATA,
                    "PUNISHERX_NO_DATABASE_DATA",
                )
            }

            journalIds(connection, migrationId, "PUNISHMENT", "SELECT id FROM punishments WHERE uuid = ?", source)
            journalIds(connection, migrationId, "HISTORY", "SELECT id FROM punishmenthistory WHERE uuid = ?", source)
            journalIds(connection, migrationId, "REPORTER", "SELECT id FROM reports WHERE player = ?", source)
            journalIds(connection, migrationId, "SUSPECT", "SELECT id FROM reports WHERE suspect = ?", source)
            journalIds(connection, migrationId, "BRIDGE", "SELECT id FROM bridge_events WHERE target = ?", source)

            replaceUuid(connection, "punishments", "uuid", source, target)
            replaceUuid(connection, "punishmenthistory", "uuid", source, target)
            replaceUuid(connection, "reports", "player", source, target)
            replaceUuid(connection, "reports", "suspect", source, target)
            replaceUuid(connection, "bridge_events", "target", source, target)

            updateState(connection, migrationId, COMPLETED)
            PunisherIdentityMigrationBridgeResult(PunisherIdentityMigrationStatus.SUCCESS, inspection.reasonCode, true)
        }

    fun rollback(connection: Connection, migrationId: UUID, source: UUID, target: UUID): PunisherIdentityMigrationBridgeResult =
        transaction(connection) {
            migrateSchema(connection)
            val row = migration(connection, migrationId)
                ?: return@transaction PunisherIdentityMigrationBridgeResult(
                    PunisherIdentityMigrationStatus.NO_DATA,
                    "PUNISHERX_NO_MIGRATION",
                )
            if (row.source != source || row.target != target) {
                return@transaction PunisherIdentityMigrationBridgeResult(
                    PunisherIdentityMigrationStatus.BLOCKED,
                    "PUNISHERX_MIGRATION_ID_CONFLICT",
                )
            }
            if (row.state == ROLLED_BACK) {
                return@transaction PunisherIdentityMigrationBridgeResult(
                    PunisherIdentityMigrationStatus.ROLLED_BACK,
                    "PUNISHERX_ALREADY_ROLLED_BACK",
                )
            }
            for (item in items(connection, migrationId)) {
                when (item.type) {
                    "PUNISHMENT" -> restore(connection, "punishments", "id", item.key, "uuid", source, target)
                    "HISTORY" -> restore(connection, "punishmenthistory", "id", item.key, "uuid", source, target)
                    "REPORTER" -> restore(connection, "reports", "id", item.key, "player", source, target)
                    "SUSPECT" -> restore(connection, "reports", "id", item.key, "suspect", source, target)
                    "BRIDGE" -> restore(connection, "bridge_events", "id", item.key, "target", source, target)
                }
            }
            updateState(connection, migrationId, ROLLED_BACK)
            PunisherIdentityMigrationBridgeResult(
                PunisherIdentityMigrationStatus.ROLLED_BACK,
                "PUNISHERX_ROLLBACK_COMPLETED",
            )
        }

    private fun <T> transaction(connection: Connection, action: () -> T): T {
        val autoCommit = connection.autoCommit
        val isolation = connection.transactionIsolation
        try {
            if (!connection.metaData.databaseProductName.equals("SQLite", ignoreCase = true)) {
                connection.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
            }
            connection.autoCommit = false
            val result = action()
            connection.commit()
            return result
        } catch (failure: Throwable) {
            runCatching { connection.rollback() }
            throw failure
        } finally {
            connection.autoCommit = autoCommit
            if (connection.transactionIsolation != isolation) connection.transactionIsolation = isolation
        }
    }

    private fun replaceUuid(connection: Connection, table: String, column: String, source: UUID, target: UUID) {
        connection.prepareStatement("UPDATE $table SET $column = ? WHERE $column = ?").use {
            it.setString(1, target.toString())
            it.setString(2, source.toString())
            it.executeUpdate()
        }
    }

    private fun restore(
        connection: Connection,
        table: String,
        idColumn: String,
        id: String,
        uuidColumn: String,
        source: UUID,
        target: UUID,
    ) {
        val changed = connection.prepareStatement(
            "UPDATE $table SET $uuidColumn = ? WHERE $idColumn = ? AND $uuidColumn = ?",
        ).use {
            it.setString(1, source.toString())
            it.setString(2, id)
            it.setString(3, target.toString())
            it.executeUpdate()
        }
        check(changed == 1) { "PunisherX cannot rollback $table/$id because it changed after migration" }
    }

    private fun journalIds(
        connection: Connection,
        migrationId: UUID,
        type: String,
        sql: String,
        source: UUID,
    ) {
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, source.toString())
            statement.executeQuery().use { rows ->
                while (rows.next()) addItem(connection, migrationId, type, rows.getString(1))
            }
        }
    }

    private fun count(connection: Connection, sql: String, uuid: UUID): Int =
        connection.prepareStatement(sql).use {
            it.setString(1, uuid.toString())
            it.executeQuery().use { rows -> check(rows.next()); rows.getInt(1) }
        }

    private fun addItem(connection: Connection, migrationId: UUID, type: String, key: String) {
        connection.prepareStatement(
            "INSERT INTO punisherx_identity_migration_items(migration_id, entity_type, entity_key) VALUES (?, ?, ?)",
        ).use {
            it.setString(1, migrationId.toString())
            it.setString(2, type)
            it.setString(3, key)
            it.executeUpdate()
        }
    }

    private fun clearItems(connection: Connection, migrationId: UUID) {
        connection.prepareStatement("DELETE FROM punisherx_identity_migration_items WHERE migration_id = ?").use {
            it.setString(1, migrationId.toString())
            it.executeUpdate()
        }
    }

    private fun updateState(connection: Connection, migrationId: UUID, state: String) {
        connection.prepareStatement("UPDATE punisherx_identity_migrations SET state = ? WHERE migration_id = ?").use {
            it.setString(1, state)
            it.setString(2, migrationId.toString())
            check(it.executeUpdate() == 1)
        }
    }

    private fun itemCount(connection: Connection, migrationId: UUID): Int =
        connection.prepareStatement("SELECT COUNT(*) FROM punisherx_identity_migration_items WHERE migration_id = ?").use {
            it.setString(1, migrationId.toString())
            it.executeQuery().use { rows -> check(rows.next()); rows.getInt(1) }
        }

    private fun migration(connection: Connection, migrationId: UUID): MigrationRow? =
        connection.prepareStatement(
            "SELECT source_uuid, target_uuid, state FROM punisherx_identity_migrations WHERE migration_id = ?",
        ).use {
            it.setString(1, migrationId.toString())
            it.executeQuery().use { rows ->
                if (!rows.next()) null else MigrationRow(
                    UUID.fromString(rows.getString(1)),
                    UUID.fromString(rows.getString(2)),
                    rows.getString(3),
                )
            }
        }

    private fun items(connection: Connection, migrationId: UUID): List<Item> =
        connection.prepareStatement(
            "SELECT entity_type, entity_key FROM punisherx_identity_migration_items WHERE migration_id = ? ORDER BY entity_type, entity_key",
        ).use {
            it.setString(1, migrationId.toString())
            it.executeQuery().use { rows -> buildList {
                while (rows.next()) add(Item(rows.getString(1), rows.getString(2)))
            } }
        }

    private data class MigrationRow(val source: UUID, val target: UUID, val state: String)
    private data class Item(val type: String, val key: String)
}
