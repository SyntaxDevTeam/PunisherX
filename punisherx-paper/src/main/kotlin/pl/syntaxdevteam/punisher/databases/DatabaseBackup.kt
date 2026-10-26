package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.syntaxdb.Database
import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.Locale

typealias DatabaseSnapshot = Map<String, List<Map<String, Any?>>>

/** Complete, parameterized backup/restore. Import accepts INSERT dumps, never executes arbitrary SQL. */
internal object DatabaseBackup {
    private val columns = linkedMapOf(
        "punishments" to listOf("id", "name", "uuid", "reason", "operator", "punishmentType", "start", "endTime", "server"),
        "punishmenthistory" to listOf("id", "name", "uuid", "reason", "operator", "punishmentType", "start", "endTime", "server"),
        "punishment_undos" to listOf("historyId", "undoneBy", "undoneAt"),
        "punishment_templates" to listOf("historyId", "templateName", "playerUuid"),
        "playercache" to listOf("id", "data"),
        "reports" to listOf("id", "player", "suspect", "reason", "filedAt"),
        "report_resolutions" to listOf("reportId", "status", "handledBy", "note", "handledAt"),
        "bridge_events" to listOf("id", "action", "target", "reason", "endTime", "processed", "processedAt"),
        "punisherx_identity_migrations" to listOf("migration_id", "source_uuid", "target_uuid", "state"),
        "punisherx_identity_migration_items" to listOf("migration_id", "entity_type", "entity_key")
    )

    fun snapshot(database: Database): DatabaseSnapshot = database.transaction {
        PunisherDatabaseSchema.tables.associateWith { table -> query("SELECT * FROM $table").map { it.toMap() } }
    }

    fun write(snapshot: DatabaseSnapshot, file: File) {
        Files.createDirectories(file.parentFile.toPath())
        val temporary = Files.createTempFile(file.parentFile.toPath(), "backup-", ".tmp")
        try {
            Files.newBufferedWriter(temporary, Charsets.UTF_8).use { writer ->
                for ((table, rows) in snapshot) for (row in rows) {
                    validate(table, row.keys)
                    writer.append("INSERT INTO $table (${row.keys.joinToString(", ")}) VALUES (")
                    writer.append(row.values.joinToString(", ") { value -> when (value) {
                        null -> "NULL"
                        is Number -> value.toString()
                        is Boolean -> if (value) "1" else "0"
                        else -> "'" + value.toString().replace("'", "''") + "'"
                    } })
                    writer.append(");\n")
                }
            }
            try { Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { Files.deleteIfExists(temporary) }
    }

    fun restore(database: Database, snapshot: DatabaseSnapshot) {
        snapshot.forEach { (table, rows) -> rows.forEach { validate(table, it.keys) } }
        database.transaction {
            for ((table, rows) in snapshot) {
                if (rows.isEmpty()) continue
                val identity = database.dialect.type == DatabaseType.SQL_SERVER && table in PunisherDatabaseSchema.identityTables
                if (identity) execute("SET IDENTITY_INSERT $table ON")
                var failure: Throwable? = null
                try {
                    for (row in rows) {
                        val names = row.keys.toList()
                        val values = row.map { (name, value) ->
                            if (name.equals("filedAt", true) && value is String && database.dialect.type != DatabaseType.SQLITE)
                                java.sql.Timestamp.valueOf(LocalDateTime.parse(value.replace(' ', 'T')))
                            else if (value is String && name.lowercase(Locale.ROOT) in setOf(
                                "id", "historyid", "reportid", "start", "endtime", "processed", "processedat", "undoneat", "handledat"
                            )) value.toLong() // Compatibility with old exports that quoted every scalar.
                            else value
                        }
                        execute("INSERT INTO $table (${names.joinToString(", ")}) VALUES (${names.joinToString(", ") { "?" }})", values)
                    }
                    if (database.dialect.type == DatabaseType.POSTGRESQL && table in PunisherDatabaseSchema.identityTables) {
                        query("SELECT setval(pg_get_serial_sequence(?, 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM $table", listOf(table))
                    }
                } catch (error: Throwable) { failure = error; throw error }
                finally {
                    if (identity) try { execute("SET IDENTITY_INSERT $table OFF") }
                    catch (cleanup: Throwable) { if (failure != null) failure.addSuppressed(cleanup) else throw cleanup }
                }
            }
        }
    }

    private fun validate(table: String, names: Set<String>) {
        val allowed = requireNotNull(columns[table]) { "Unknown backup table: $table" }.map { it.lowercase(Locale.ROOT) }.toSet()
        require(names.isNotEmpty() && names.map { it.lowercase(Locale.ROOT) }.distinct().size == names.size &&
            names.all { it.lowercase(Locale.ROOT) in allowed }) { "Unknown or duplicate backup columns in $table" }
    }

    fun read(file: File): DatabaseSnapshot {
        val parser = InsertParser(file.readText(Charsets.UTF_8))
        val rows = linkedMapOf<String, MutableList<Map<String, Any?>>>()
        while (!parser.finished()) {
            parser.keyword("INSERT"); parser.keyword("INTO")
            val table = parser.identifier().lowercase(Locale.ROOT)
            val names = if (parser.take('(')) buildList {
                do { add(parser.identifier()) } while (parser.take(','))
                parser.expect(')')
            } else requireNotNull(columns[table]) { "Unknown backup table: $table" }
            validate(table, names.toSet())
            require(names.distinct().size == names.size) { "Duplicate backup columns" }
            parser.keyword("VALUES")
            do {
                parser.expect('(')
                val values = buildList { do { add(parser.value()) } while (parser.take(',')) }
                parser.expect(')')
                require(values.size == names.size) { "Backup column/value count differs for $table" }
                rows.getOrPut(table) { mutableListOf() }.add(names.zip(values).toMap())
            } while (parser.take(','))
            parser.expect(';')
        }
        return rows
    }

    /** A small INSERT-only grammar handles quoted semicolons, newlines, apostrophes and NULL. */
    private class InsertParser(private val text: String) {
        private var position = 0
        private fun skip() {
            while (position < text.length) {
                if (text[position].isWhitespace()) position++
                else if (text.startsWith("--", position)) {
                    while (position < text.length && text[position] != '\n') position++
                } else break
            }
        }
        fun finished(): Boolean { skip(); return position == text.length }
        fun take(char: Char): Boolean {
            skip()
            if (position >= text.length || text[position] != char) return false
            position++; return true
        }
        fun expect(char: Char) { require(take(char)) { "Expected '$char' in backup at offset $position" } }
        fun keyword(word: String) { require(identifier().equals(word, true)) { "Expected $word in backup" } }
        fun identifier(): String {
            skip(); val start = position
            while (position < text.length && (text[position].isLetterOrDigit() || text[position] == '_')) position++
            require(position > start) { "Expected identifier in backup at offset $position" }
            return text.substring(start, position)
        }
        fun value(): Any? {
            skip()
            if (take('\'')) return buildString {
                var terminated = false
                while (position < text.length) {
                    val char = text[position++]
                    if (char == '\'') {
                        if (position < text.length && text[position] == '\'') { append('\''); position++ }
                        else { terminated = true; break }
                    } else append(char)
                }
                require(terminated) { "Unterminated string in backup" }
            }
            val start = position
            while (position < text.length && text[position] !in ",)" && !text[position].isWhitespace()) position++
            val token = text.substring(start, position)
            return if (token.equals("NULL", true)) null else token.toLongOrNull() ?: BigDecimal(token)
        }
    }
}
