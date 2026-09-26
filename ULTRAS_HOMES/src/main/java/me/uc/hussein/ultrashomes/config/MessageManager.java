package me.uc.hussein.ultrashomes.config;

import me.uc.hussein.ultrashomes.UltrasHomesPlugin;
import me.uc.hussein.ultrashomes.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads messages/en.yml and messages/ar.yml (selected by language.default in config.yml). */
public final class MessageManager {
    private final UltrasHomesPlugin plugin;
    private volatile YamlConfiguration cfg = new YamlConfiguration();
    private volatile String lang = "en";

    public MessageManager(UltrasHomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ensure("messages/en.yml");
        ensure("messages/ar.yml");
        lang = plugin.cfg().language();
        String path = "messages/" + lang + ".yml";
        File file = new File(plugin.getDataFolder(), path);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(path)) {
            if (in != null) {
                y.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read bundled " + path + ": " + e.getMessage());
        }
        cfg = y;
    }

    private void ensure(String resourcePath) {
        File f = new File(plugin.getDataFolder(), resourcePath);
        if (!f.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    public String language() {
        return lang;
    }

    public String raw(String key) {
        String s = cfg.getString(key);
        return s == null ? key : s;
    }

    public List<String> rawList(String key) {
        return cfg.getStringList(key);
    }

    private Map<String, String> withPrefix(Map<String, String> ph) {
        Map<String, String> m = ph == null ? new HashMap<>() : new HashMap<>(ph);
        m.putIfAbsent("prefix-mm", plugin.cfg().prefix());
        return m;
    }

    private String fillPrefix(String tpl, Map<String, String> ph) {
        String withPrefix = tpl.replace("{prefix}", "{prefix-mm}");
        return Text.fill(withPrefix, withPrefix(ph));
    }

    public Component c(String key, Map<String, String> ph) {
        return Text.mm(fillPrefix(raw(key), ph));
    }

    public Component c(String key) {
        return c(key, null);
    }

    public List<Component> list(String key, Map<String, String> ph) {
        List<Component> out = new ArrayList<>();
        for (String line : rawList(key)) {
            out.add(Text.mm(fillPrefix(line, ph)));
        }
        return out;
    }

    public String plain(String key, Map<String, String> ph) {
        return Text.strip(fillPrefix(raw(key), ph));
    }

    public String plain(String key) {
        return plain(key, null);
    }

    public void send(CommandSender to, String key, Map<String, String> ph) {
        to.sendMessage(c(key, ph));
    }

    public void send(CommandSender to, String key) {
        send(to, key, null);
    }

    public void sendList(CommandSender to, String key, Map<String, String> ph) {
        for (Component c : list(key, ph)) {
            to.sendMessage(c);
        }
    }
}
