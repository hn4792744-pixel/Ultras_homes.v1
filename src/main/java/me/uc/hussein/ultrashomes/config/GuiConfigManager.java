package me.uc.hussein.ultrashomes.config;

import me.uc.hussein.ultrashomes.UltrasHomesPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Loads the four gui/*.yml files so every GUI's layout, materials, text and sounds are data-driven. */
public final class GuiConfigManager {
    public static final String HOMES = "homes.yml";
    public static final String HOME_MANAGE = "home_manage.yml";
    public static final String PLAYER_HOMES = "player_homes.yml";
    public static final String ADMIN_HOMES = "admin_homes.yml";

    private final UltrasHomesPlugin plugin;
    private final Map<String, YamlConfiguration> cache = new HashMap<>();

    public GuiConfigManager(UltrasHomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        cache.clear();
        for (String f : new String[]{HOMES, HOME_MANAGE, PLAYER_HOMES, ADMIN_HOMES}) {
            cache.put(f, loadOne(f));
        }
    }

    private YamlConfiguration loadOne(String name) {
        String resourcePath = "gui/" + name;
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                y.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read bundled " + resourcePath + ": " + e.getMessage());
        }
        return y;
    }

    public YamlConfiguration homes() { return cache.get(HOMES); }
    public YamlConfiguration homeManage() { return cache.get(HOME_MANAGE); }
    public YamlConfiguration playerHomes() { return cache.get(PLAYER_HOMES); }
    public YamlConfiguration adminHomes() { return cache.get(ADMIN_HOMES); }
}
