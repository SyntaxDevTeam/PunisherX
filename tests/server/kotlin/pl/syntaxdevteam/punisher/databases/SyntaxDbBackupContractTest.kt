package pl.syntaxdevteam.punisher.databases

import pl.syntaxdevteam.syntaxdb.Database
import kotlin.test.*

class SyntaxDbBackupContractTest : SyntaxDbTestFixture() {
    @Test fun `backup parser rejects unknown tables columns duplicate columns expressions and trailing SQL`() {
        val malformed = listOf(
            "INSERT INTO unknown (id) VALUES (1);",
            "INSERT INTO playercache (unknown) VALUES (1);",
            "INSERT INTO playercache (id,id) VALUES (1,2);",
            "INSERT INTO playercache (id,ID) VALUES (1,2);",
            "INSERT INTO playercache (id,data) VALUES (1);",
            "INSERT INTO playercache (id) VALUES (1,2);",
            "INSERT INTO playercache (id,data) VALUES (1,NOW());",
            "INSERT INTO playercache (id,data) VALUES (1,'unclosed);",
            "INSERT INTO playercache (id) VALUES (1)",
            "INSERT INTO playercache (id) VALUES (1); DELETE FROM punishments;",
            "DROP TABLE punishments;",
            "UPDATE playercache SET data='overwrite';"
        )
        val file = folder.resolve("bad.sql")
        for (sql in malformed) {
            file.writeText(sql)
            assertFails(sql) { DatabaseBackup.read(file) }
        }
    }

    @Test fun `failed export keeps previous backup and removes temporary file`() {
        val file = folder.resolve("dump/backup.sql")
        file.parentFile.mkdirs(); file.writeText("previous backup")
        assertFails { DatabaseBackup.write(linkedMapOf(
            "playercache" to listOf(mapOf("id" to 1, "data" to reason)),
            "unknown" to listOf(mapOf("id" to 1))
        ), file) }
        assertEquals("previous backup", file.readText())
        assertEquals(listOf("backup.sql"), file.parentFile.list()!!.toList())
    }

    @Test fun `restore validates all tables before writing the first row`() {
        Database.connect("sqlite") { file = folder.resolve("restore.sqlite").path }.use { db ->
            PunisherDatabaseSchema.create(db)
            assertFails { DatabaseBackup.restore(db, linkedMapOf(
                "playercache" to listOf(mapOf("id" to 1, "data" to "first")),
                "punishments" to listOf(mapOf("id" to 2, "unsupported" to "bad"))
            )) }
            assertEquals(0L, db.count("playercache"))
        }
    }

    @Test fun `parser preserves comments inside strings null empty values and explicit column order`() {
        val file = folder.resolve("valid.sql").apply { writeText("""
            -- legacy export
            insert into PLAYERCACHE (data, id) values ('--not a comment; O''Brien', 7), ('', 8), (NULL, 9);
            -- end
        """.trimIndent()) }
        val rows = DatabaseBackup.read(file).getValue("playercache")
        assertEquals(listOf("--not a comment; O'Brien", "", null), rows.map { it["data"] })
        assertEquals(listOf(7L, 8L, 9L), rows.map { it["id"] })
    }

    @Test fun `empty and comment only imports produce empty snapshots`() {
        val file = folder.resolve("empty.sql")
        for (contents in listOf("", "  \n\t", "-- no data\n-- another comment")) {
            file.writeText(contents); assertTrue(DatabaseBackup.read(file).isEmpty())
        }
    }

    @Test fun `large backup preserves one hundred thousand records and boundary values`() {
        start()
        val rows = 100_000
        val payload = reason + "x".repeat(128)
        jdbc { connection ->
            connection.autoCommit = false
            connection.prepareStatement("INSERT INTO playercache (id, data) VALUES (?, ?)").use { insert ->
                for (id in 1..rows) {
                    insert.setInt(1, id)
                    insert.setString(2, "$id:$payload")
                    insert.addBatch()
                    if (id % 1000 == 0) insert.executeBatch()
                }
            }
            connection.commit()
        }
        val started = System.nanoTime()
        handler.exportDatabase()
        val exportMs = (System.nanoTime() - started) / 1_000_000
        val file = folder.resolve("dump/backup.sql")
        assertTrue(file.length() > 10_000_000)
        sql("DELETE FROM playercache")
        val importStarted = System.nanoTime()
        handler.importDatabase()
        val importMs = (System.nanoTime() - importStarted) / 1_000_000
        assertEquals(rows, count("playercache"))
        jdbc { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id, data FROM playercache ORDER BY id").use { result ->
                    for (id in 1..rows) {
                        assertTrue(result.next())
                        assertEquals(id, result.getInt("id"))
                        assertEquals("$id:$payload", result.getString("data"))
                    }
                    assertFalse(result.next())
                }
            }
        }
        println("Large SQLite backup: rows=$rows, bytes=${file.length()}, exportMs=$exportMs, importMs=$importMs")
    }

    @Test fun `empty database backup is a valid no op restore`() {
        start(); handler.exportDatabase()
        assertTrue(DatabaseBackup.read(folder.resolve("dump/backup.sql")).isEmpty())
        handler.importDatabase(); assertTrue(handler.isReady()); assertTrue(handler.runHealthCheck().ok)
    }
}
