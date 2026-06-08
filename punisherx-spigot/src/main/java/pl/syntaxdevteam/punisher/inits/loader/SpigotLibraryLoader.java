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
import java.util.List;
import java.util.Map;

/**
 * Handles runtime dependency downloads using Libby.
 *
 * This loader ensures that libraries which are marked as compileOnly in the build script
 * are downloaded and available before the rest of the plugin is initialised.
 */
public class SpigotLibraryLoader {

    private final JavaPlugin plugin;
    private final BukkitLibraryManager libraryManager;

    private static final String LIBRARIES_RESOURCE = "spigot-libraries.yml";

    public SpigotLibraryLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.libraryManager = new BukkitLibraryManager(plugin);
    }

    /**
     * Downloads and loads all runtime libraries required by the plugin.
     */
    public void loadRuntimeLibraries() {
        libraryManager.addMavenCentral();
        libraryManager.addRepository("https://repo1.maven.org/maven2/");
        libraryManager.addRepository("https://nexus.syntaxdevteam.pl/repository/maven-releases/");
        libraryManager.addRepository("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/");

        List<Map.Entry<String, Library>> runtimeLibraries = loadLibrariesFromConfig();

        if (runtimeLibraries.isEmpty()) {
            plugin.getLogger().warning("[GraveDiggerX] No runtime libraries configured in " + LIBRARIES_RESOURCE);
            return;
        }

        for (Map.Entry<String, Library> entry : runtimeLibraries) {
            String coordinates = entry.getKey();
            Library library = entry.getValue();

            try {
                loadLibraryWithFallbacks(coordinates, library);
            } catch (Exception exception) {
                plugin.getLogger().severe("[GraveDiggerX] Failed to load runtime library " + coordinates + ": " + exception.getMessage());
                throw new IllegalStateException("Unable to load runtime library " + coordinates, exception);
            }
        }
    }

    private void loadLibraryWithFallbacks(String coordinates, Library library) {
        try {
            libraryManager.loadLibrary(library);
            plugin.getLogger().info("[GraveDiggerX] Loaded runtime library: " + coordinates);
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
                    plugin.getLogger().warning("[GraveDiggerX] Retry runtime library with fallback version: " + fallbackCoordinates);
                    libraryManager.loadLibrary(fallbackLibrary);
                    plugin.getLogger().info("[GraveDiggerX] Loaded runtime library using fallback: " + fallbackCoordinates);
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

    private List<Map.Entry<String, Library>> loadLibrariesFromConfig() {

        InputStream resourceStream = plugin.getResource(LIBRARIES_RESOURCE);
        if (resourceStream == null) {
            throw new IllegalStateException("Missing " + LIBRARIES_RESOURCE + " resource in plugin jar");
        }

        List<String> configuredLibraries;

        try (InputStream inputStream = resourceStream) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            );

            configuredLibraries = config.getStringList("libraries");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load " + LIBRARIES_RESOURCE, exception);
        }

        if (configuredLibraries.isEmpty()) {
            plugin.getLogger().warning("[GraveDiggerX] No libraries defined under 'libraries' in " + LIBRARIES_RESOURCE);
            return List.of();
        }

        List<Map.Entry<String, Library>> result = new ArrayList<>();

        for (String coordinates : configuredLibraries) {
            Map.Entry<String, Library> parsed = parseLibrary(coordinates);
            if (parsed != null) {
                result.add(parsed);
            }
        }

        return result;
    }

    private Map.Entry<String, Library> parseLibrary(String coordinates) {

        ParsedCoordinates parsedCoordinates = parseCoordinates(coordinates);

        if (parsedCoordinates == null) {
            plugin.getLogger().severe(
                "[GraveDiggerX] Invalid library coordinates '" + coordinates +
                    "' in " + LIBRARIES_RESOURCE +
                    ". Expected format group:artifact:version"
            );
            return null;
        }

        Library library = Library.builder()
            .groupId(parsedCoordinates.groupId())
            .artifactId(parsedCoordinates.artifactId())
            .version(parsedCoordinates.version())
            .build();

        return new AbstractMap.SimpleEntry<>(coordinates, library);
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
}
