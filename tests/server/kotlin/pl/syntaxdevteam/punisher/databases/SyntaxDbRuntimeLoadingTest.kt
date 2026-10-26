package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.net.URLClassLoader
import java.lang.reflect.Proxy
import kotlin.test.*

class SyntaxDbRuntimeLoadingTest {
    @Test fun `runtime manifests explicitly declare the complete database runtime without unresolved versions`() {
        val manifest = listOf("paper-libraries.yml", "spigot-libraries.yml")
            .firstNotNullOf { name -> javaClass.classLoader.getResource(name) }
        val text = manifest.readText()
        assertFalse(text.contains("\${"))
        val config = YamlConfiguration.loadConfiguration(text.reader())
        val libraries = config.getList("libraries")!!.map { entry ->
            if (entry is String) entry else (entry as Map<*, *>)["coordinate"] as String
        }
        val required = listOf("pl.syntaxdevteam:syntaxdb-core", "com.zaxxer:HikariCP", "org.xerial:sqlite-jdbc",
            "com.mysql:mysql-connector-j", "com.google.protobuf:protobuf-java", "org.postgresql:postgresql", "com.microsoft.sqlserver:mssql-jdbc")
        for (coordinate in required) {
            assertEquals(1, libraries.count { it.startsWith("$coordinate:") }, coordinate)
            val artifact = coordinate.substringAfter(':')
            val version = libraries.single { it.startsWith("$coordinate:") }.substringAfterLast(':')
            val resolved = System.getProperty("punisherx.database.runtimeClasspath").split(File.pathSeparator)
            assertTrue(resolved.any { File(it).name == "$artifact-$version.jar" }, "Runtime manifest and tested dependency differ: $coordinate:$version")
        }
    }

    @Test fun `isolated loader starts SyntaxDB and JDBC using only declared runtime dependencies`() {
        val paths = System.getProperty("punisherx.database.runtimeClasspath")
            ?: error("Run through the configured Gradle test task")
        val urls = paths.split(File.pathSeparator).map { File(it).toURI().toURL() }.toTypedArray()
        URLClassLoader(urls, ClassLoader.getPlatformClassLoader()).use { loader ->
            val thread = Thread.currentThread()
            val previous = thread.contextClassLoader
            thread.contextClassLoader = loader
            try {
                for (name in listOf("pl.syntaxdevteam.syntaxdb.Database", "com.zaxxer.hikari.HikariDataSource",
                    "org.sqlite.JDBC", "com.mysql.cj.jdbc.Driver", "com.google.protobuf.Message",
                    "org.postgresql.Driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")) {
                    assertSame(loader, Class.forName(name, true, loader).classLoader, name)
                }
                assertFailsWith<ClassNotFoundException> { loader.loadClass("kotlinx.coroutines.CoroutineScope") }
                val databaseClass = loader.loadClass("pl.syntaxdevteam.syntaxdb.Database")
                val companion = databaseClass.getField("Companion").get(null)
                val function = loader.loadClass("kotlin.jvm.functions.Function1")
                val block = Proxy.newProxyInstance(loader, arrayOf(function)) { _, method, args ->
                    check(method.name == "invoke")
                    val builder = args!![0]
                    builder.javaClass.getMethod("setJdbcUrl", String::class.java).invoke(builder, "jdbc:sqlite::memory:")
                    loader.loadClass("kotlin.Unit").getField("INSTANCE").get(null)
                }
                val db = companion.javaClass.getMethod("connect", String::class.java, function).invoke(companion, "sqlite", block)
                try {
                    assertEquals(true, databaseClass.getMethod("healthCheck", Int::class.javaPrimitiveType).invoke(db, 2))
                    val optionsClass = loader.loadClass("pl.syntaxdevteam.syntaxdb.query.QueryOptions")
                    val options = optionsClass.getConstructor().newInstance()
                    val execute = databaseClass.getMethod("execute", String::class.java, List::class.java, optionsClass)
                    execute.invoke(db, "CREATE TABLE runtime_probe (id INTEGER PRIMARY KEY, reason TEXT)", emptyList<Any>(), options)
                    assertEquals(1, execute.invoke(db, "INSERT INTO runtime_probe (id, reason) VALUES (?, ?)", listOf(1, "Zażółć 🐉"), options))
                    val rows = databaseClass.getMethod("query", String::class.java, List::class.java, optionsClass)
                        .invoke(db, "SELECT reason FROM runtime_probe WHERE id = ?", listOf(1), options) as List<*>
                    assertEquals(1, rows.size)
                    assertEquals("Zażółć 🐉", rows.single()!!.javaClass.getMethod("string", String::class.java).invoke(rows.single(), "reason"))
                } finally { databaseClass.getMethod("close").invoke(db) }
                assertEquals(false, databaseClass.getMethod("healthCheck", Int::class.javaPrimitiveType).invoke(db, 2))
            } finally { thread.contextClassLoader = previous }
        }
    }
}
