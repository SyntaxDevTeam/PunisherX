package pl.syntaxdevteam.punisher.inits.loader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class SpigotBootstrapPreloader {

    private SpigotBootstrapPreloader() {
    }

    public static boolean shouldUseSpigotLoader(final JavaPlugin plugin) {
        if (plugin == null) {
            return false;
        }

        final String serverName = plugin.getServer().getName();

        final String lowerServerName = serverName.toLowerCase(Locale.ROOT);
        final boolean isPaperOrFolia = lowerServerName.contains("paper")
            || lowerServerName.contains("purpur")
            || lowerServerName.contains("folia");
        final boolean isSpigotFamily = lowerServerName.contains("spigot")
            || lowerServerName.contains("craftbukkit")
            || lowerServerName.contains("bukkit");

        return isSpigotFamily && !isPaperOrFolia;
    }
}
