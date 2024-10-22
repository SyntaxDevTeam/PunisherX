package pl.syntaxdevteam.loader

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.io.InputStream

@Suppress("UnstableApiUsage")
class SyntaxLoader : PluginLoader {

    override fun classloader(pluginClasspath: PluginClasspathBuilder) {
        val resolver = MavenLibraryResolver()

        resolveLibraries().map { DefaultArtifact(it) }
            .forEach { artifact -> resolver.addDependency(Dependency(artifact, null)) }

        resolver.addRepository(RemoteRepository.Builder("paper", "default", "https://repo.papermc.io/repository/maven-public/").build())
        pluginClasspath.addLibrary(resolver)
    }

    private fun resolveLibraries(): List<String> {
        return try {
            readLibraryListFromYaml()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun readLibraryListFromYaml(): List<String> {
        val yaml = Yaml()
        val inputStream: InputStream? = SyntaxLoader::class.java.classLoader.getResourceAsStream("paper-libraries.yml")

        if (inputStream == null) {
            System.err.println("paper-libraries.yml not found in the classpath.")
            return emptyList()
        }

        val data: Map<String, List<String>> = yaml.load(inputStream)
        return data["libraries"] ?: emptyList()
    }
}
