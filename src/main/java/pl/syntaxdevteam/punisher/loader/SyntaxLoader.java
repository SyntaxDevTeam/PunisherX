package pl.syntaxdevteam.punisher.loader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class SyntaxLoader implements PluginLoader {

    @Override
    public void classloader(final PluginClasspathBuilder pluginClasspath) {
        final LibraryConfig libraryConfig = resolveLibraries();
        final Map<String, MavenLibraryResolver> resolvers = new HashMap<>();

        for (final LibrarySpec library : libraryConfig.libraries()) {
            final String repositoryId = selectRepository(library, libraryConfig.repositories());
            final MavenLibraryResolver resolver = resolvers.computeIfAbsent(repositoryId, id -> {
                final RemoteRepository repository = libraryConfig.repositories().get(id);
                final MavenLibraryResolver createdResolver = new MavenLibraryResolver();
                if (repository != null) {
                    createdResolver.addRepository(repository);
                }
                return createdResolver;
            });
            resolver.addDependency(new Dependency(new DefaultArtifact(library.coordinate()), null));
        }

        for (final MavenLibraryResolver resolver : resolvers.values()) {
            pluginClasspath.addLibrary(resolver);
        }
    }

    private LibraryConfig resolveLibraries() {
        try {
            return readLibraryListFromYaml();
        } catch (IOException e) {
            e.printStackTrace();
            return LibraryConfig.empty();
        }
    }

    private LibraryConfig readLibraryListFromYaml() throws IOException {
        final Yaml yaml = new Yaml();
        try (InputStream inputStream = SyntaxLoader.class.getClassLoader().getResourceAsStream("paper-libraries.yml")) {
            if (inputStream == null) {
                System.err.println("paper-libraries.yml not found in the classpath.");
                return LibraryConfig.empty();
            }

            @SuppressWarnings("unchecked")
            final Map<String, Object> data = yaml.load(inputStream);
            if (data == null) {
                return LibraryConfig.empty();
            }
            final Map<String, RemoteRepository> repositories = resolveRepositories(data);
            final List<LibrarySpec> libraries = resolveLibrarySpecs(data);
            return new LibraryConfig(repositories, libraries);
        }
    }

    private Map<String, RemoteRepository> resolveRepositories(final Map<String, Object> data) {
        final Map<String, RemoteRepository> repositories = new HashMap<>();
        final Object repositoryData = data.get("repositories");
        if (repositoryData instanceof Map<?, ?> repoMap) {
            for (final Map.Entry<?, ?> entry : repoMap.entrySet()) {
                if (entry.getKey() instanceof String id && entry.getValue() instanceof String url) {
                    repositories.put(id, new RemoteRepository.Builder(id, "default", url).build());
                }
            }
        }
        return repositories;
    }

    private List<LibrarySpec> resolveLibrarySpecs(final Map<String, Object> data) {
        final Object librariesData = data.get("libraries");
        if (!(librariesData instanceof List<?> libraries)) {
            return Collections.emptyList();
        }

        final List<LibrarySpec> resolved = new ArrayList<>();
        for (final Object entry : libraries) {
            if (entry instanceof String coordinate) {
                resolved.add(new LibrarySpec(coordinate, null));
                continue;
            }
            if (entry instanceof Map<?, ?> mapEntry) {
                final Object coordinateValue = mapEntry.get("coordinate");
                if (!(coordinateValue instanceof String coordinate)) {
                    continue;
                }
                final Object repositoryValue = mapEntry.get("repository");
                final String repository = repositoryValue instanceof String ? (String) repositoryValue : null;
                resolved.add(new LibrarySpec(coordinate, repository));
            }
        }
        return resolved;
    }

    private String selectRepository(final LibrarySpec library, final Map<String, RemoteRepository> repositories) {
        if (library.repository() != null) {
            return library.repository();
        }
        if (library.coordinate().endsWith("-SNAPSHOT") && repositories.containsKey("syntaxdevteam-snapshots")) {
            return "syntaxdevteam-snapshots";
        }
        if (repositories.containsKey("paper")) {
            return "paper";
        }
        return repositories.keySet().stream().findFirst().orElse("default");
    }

    private record LibraryConfig(Map<String, RemoteRepository> repositories, List<LibrarySpec> libraries) {
        private static LibraryConfig empty() {
            return new LibraryConfig(Collections.emptyMap(), Collections.emptyList());
        }
    }

    private record LibrarySpec(String coordinate, String repository) {
    }
}
