package pl.syntaxdevteam.punisher.inits.loader;

import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles runtime dependency downloads using Libby.
 *
 * This loader ensures that libraries which are marked as compileOnly in the build script
 * are downloaded and available before the rest of the plugin is initialised.
 */
public final class LibraryLoader {

    private final JavaPlugin plugin;
    private final BukkitLibraryManager libraryManager;

    private static final String LIBRARIES_RESOURCE = "spigot-libraries.yml";

    public LibraryLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.libraryManager = new BukkitLibraryManager(plugin);
    }

    /**
     * Downloads and loads all runtime libraries required by the plugin.
     */
    public void loadRuntimeLibraries() {
        libraryManager.addMavenCentral();
        LibraryConfig libraryConfig = loadLibrariesFromConfig();

        for (String repositoryUrl : libraryConfig.repositories().values()) {
            libraryManager.addRepository(repositoryUrl);
        }

        List<Map.Entry<String, Library>> runtimeLibraries = libraryConfig.libraries();

        if (runtimeLibraries.isEmpty()) {
            plugin.getLogger().warning("[PunisherX] No runtime libraries configured in " + LIBRARIES_RESOURCE);
            return;
        }

        for (Map.Entry<String, Library> entry : runtimeLibraries) {
            String coordinates = entry.getKey();
            Library library = entry.getValue();

            try {
                loadLibraryWithFallbacks(coordinates, library);
            } catch (Exception exception) {
                plugin.getLogger().severe("[PunisherX] Failed to load runtime library " + coordinates + ": " + exception.getMessage());
                throw new IllegalStateException("Unable to load runtime library " + coordinates, exception);
            }
        }
    }

    private void loadLibraryWithFallbacks(String coordinates, Library library) {
        try {
            libraryManager.loadLibrary(library);
            plugin.getLogger().info("[PunisherX] Loaded runtime library: " + coordinates);
            return;
        } catch (Exception primaryException) {
            ParsedCoordinates parsedCoordinates = parseCoordinates(coordinates);
            if (parsedCoordinates == null) {
                throw primaryException;
            }

            List<String> fallbackVersions = getFallbackVersions(parsedCoordinates.version());
            for (String fallbackVersion : fallbackVersions) {
                String fallbackCoordinates = parsedCoordinates.groupId() + ":" + parsedCoordinates.artifactId() + ":" + fallbackVersion;
                Library fallbackLibrary = Library.builder()
                    .groupId(parsedCoordinates.groupId())
                    .artifactId(parsedCoordinates.artifactId())
                    .version(fallbackVersion)
                    .build();

                try {
                    plugin.getLogger().warning("[PunisherX] Retry runtime library with fallback version: " + fallbackCoordinates);
                    libraryManager.loadLibrary(fallbackLibrary);
                    plugin.getLogger().info("[PunisherX] Loaded runtime library using fallback: " + fallbackCoordinates);
                    return;
                } catch (Exception ignored) {
                    // try next fallback if available
                }
            }

            throw primaryException;
        }
    }

    private List<String> getFallbackVersions(String version) {
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            return List.of(parts[0] + "." + parts[1]);
        }
        return List.of();
    }

    private LibraryConfig loadLibrariesFromConfig() {

        InputStream resourceStream = plugin.getResource(LIBRARIES_RESOURCE);
        if (resourceStream == null) {
            throw new IllegalStateException("Missing " + LIBRARIES_RESOURCE + " resource in plugin jar");
        }

        try (InputStream inputStream = resourceStream) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            );
            return parseLibraryConfig(config);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load " + LIBRARIES_RESOURCE, exception);
        }
    }

    private LibraryConfig parseLibraryConfig(YamlConfiguration config) {
        Map<String, String> repositories = new LinkedHashMap<>();
        if (config.isConfigurationSection("repositories")) {
            for (String key : config.getConfigurationSection("repositories").getKeys(false)) {
                String repositoryUrl = config.getString("repositories." + key);
                if (repositoryUrl != null && !repositoryUrl.isBlank()) {
                    repositories.put(key, repositoryUrl);
                }
            }
        }

        List<?> configuredLibraries = config.getList("libraries", List.of());
        if (configuredLibraries.isEmpty()) {
            plugin.getLogger().warning("[PunisherX] No libraries defined under 'libraries' in " + LIBRARIES_RESOURCE);
            return new LibraryConfig(repositories, List.of());
        }

        List<Map.Entry<String, Library>> result = new ArrayList<>();
        for (Object entry : configuredLibraries) {
            Map.Entry<String, Library> parsed = parseLibrary(entry, repositories);
            if (parsed != null) {
                result.add(parsed);
            }
        }

        return new LibraryConfig(repositories, result);
    }

    private Map.Entry<String, Library> parseLibrary(Object libraryEntry, Map<String, String> repositories) {
        if (libraryEntry instanceof String coordinates) {
            return createLibraryEntry(coordinates, null);
        }
        if (libraryEntry instanceof Map<?, ?> mapEntry) {
            Object coordinateValue = mapEntry.get("coordinate");
            if (!(coordinateValue instanceof String coordinates) || coordinates.isBlank()) {
                plugin.getLogger().severe("[PunisherX] Missing library coordinate in " + LIBRARIES_RESOURCE);
                return null;
            }
            Object repositoryValue = mapEntry.get("repository");
            String repositoryUrl = repositoryValue instanceof String repository
                ? repositories.getOrDefault(repository, repository)
                : null;
            return createLibraryEntry(coordinates, repositoryUrl);
        }
        plugin.getLogger().severe("[PunisherX] Unsupported library entry in " + LIBRARIES_RESOURCE);
        return null;
    }

    private Map.Entry<String, Library> createLibraryEntry(String coordinates, String repositoryUrl) {
        ParsedCoordinates parsedCoordinates = parseCoordinates(coordinates);

        if (parsedCoordinates == null) {
            plugin.getLogger().severe(
                "[PunisherX] Invalid library coordinates '" + coordinates +
                    "' in " + LIBRARIES_RESOURCE +
                    ". Expected format group:artifact:version"
            );
            return null;
        }

        Library.Builder builder = Library.builder()
            .groupId(parsedCoordinates.groupId())
            .artifactId(parsedCoordinates.artifactId())
            .version(parsedCoordinates.version());

        if (repositoryUrl != null && !repositoryUrl.isBlank()) {
            builder.repository(repositoryUrl);
        }

        return new AbstractMap.SimpleEntry<>(coordinates, builder.build());
    }

    private ParsedCoordinates parseCoordinates(String coordinates) {
        String[] parts = coordinates.split(":");
        if (parts.length != 3) {
            return null;
        }
        return new ParsedCoordinates(parts[0], parts[1], parts[2]);
    }

    private record ParsedCoordinates(String groupId, String artifactId, String version) {
    }

    private record LibraryConfig(
        Map<String, String> repositories,
        List<Map.Entry<String, Library>> libraries
    ) {
    }
}
