package pl.syntaxdevteam.punisher.bridge.velocity

import com.velocitypowered.api.plugin.PluginManager
import net.byteflux.libby.Library
import net.byteflux.libby.VelocityLibraryManager
import org.slf4j.Logger
import java.nio.file.Path

internal class VelocityLibraryLoader(logger: Logger, dataDirectory: Path, plugins: PluginManager, plugin: Any) {
    private val manager = VelocityLibraryManager(logger, dataDirectory, plugins, plugin).apply {
        addMavenCentral()
    }
    private val pending = ArrayDeque(
        checkNotNull(javaClass.getResourceAsStream("/velocity-libraries.txt")) { "Missing velocity-libraries.txt" }
            .bufferedReader().use { it.readLines().filter(String::isNotBlank) }
    )

    fun load() {
        while (pending.isNotEmpty()) {
            val coordinates = pending.first()
            val (group, artifact, version) = coordinates.split(':')
            val library = Library.builder().groupId(group).artifactId(artifact).version(version)
            if (group == "pl.syntaxdevteam") {
                library.repository("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")
            }
            try {
                manager.loadLibrary(library.build())
            } catch (error: RuntimeException) {
                throw IllegalStateException("Unable to load Velocity runtime library $coordinates", error)
            }
            pending.removeFirst()
        }
    }
}
