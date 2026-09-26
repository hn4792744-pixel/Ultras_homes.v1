package me.uc.hussein.ultrashomes.gui;

import me.uc.hussein.ultrashomes.UltrasHomesPlugin;
import me.uc.hussein.ultrashomes.model.Home;
import me.uc.hussein.ultrashomes.model.PlayerHomeData;
import me.uc.hussein.ultrashomes.util.ItemBuilder;
import me.uc.hussein.ultrashomes.util.LocationUtil;
import me.uc.hussein.ultrashomes.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/** Admin view of a target player's homes: /home_admin tp <player>. Never lets the admin save a home. */
public final class PlayerHomesGui extends Menu {
    private final java.util.UUID target;
    private final PlayerHomeData data;
    private final int page;
    private final YamlConfiguration cfg;
    private final int limit;
    private final int totalSlots;
    private final int totalPages;

    public PlayerHomesGui(UltrasHomesPlugin plugin, Player admin, java.util.UUID target, PlayerHomeData data, int page) {
        super(plugin, admin);
        this.target = target;
        this.data = data;
        this.cfg = plugin.guiConfig().playerHomes();
        if (data.limitOverride != null) {
            this.limit = Math.min(data.limitOverride, plugin.cfg().maximumHomes());
        } else {
            Player onlineTarget = Bukkit.getPlayer(target);
            this.limit = onlineTarget != null ? plugin.limits().permissionLimit(onlineTarget) : plugin.cfg().defaultLimit();
        }
        int highest = data.homes.keySet().stream().max(Integer::compareTo).orElse(0);
        this.totalSlots = Math.min(plugin.cfg().maximumHomes(), Math.max(limit, highest));
        int perPage = Math.max(1, cfg.getInt("homes-per-page", 7));
        this.totalPages = Math.max(1, (int) Math.ceil(totalSlots / (double) perPage));
        this.page = Math.max(0, Math.min(page, totalPages - 1));
    }

    @Override
    protected Component title() {
        return Text.mm(Text.fill(cfg.getString("title", "ULTRAS HOMES"),
                Text.map("target", data.name, "page", String.valueOf(page + 1))));
    }

    @Override
    protected int size() {
        return Math.max(9, cfg.getInt("rows", 4) * 9);
    }

    @Override
    protected String openSoundKey() {
        return cfg.getString("open-sound");
    }

    @Override
    protected void build() {
        var gui = plugin.gui();
        gui.fillBackground(inventory, cfg);

        ConfigurationSection headSec = cfg.getConfigurationSection("player-head");
        if (headSec != null && plugin.cfg().playerInfoEnabled()) {
            Player online = Bukkit.getPlayer(target);
            Map<String, String> hph = Text.map(
                    "target", data.name,
                    "online_status", online != null ? "Online" : "Offline",
                    "world", plugin.cfg().playerInfoShowWorld() && online != null ? online.getWorld().getName() : "-",
                    "x", plugin.cfg().playerInfoShowCoordinates() && online != null ? String.valueOf(Math.round(online.getLocation().getX())) : "-",
                    "y", plugin.cfg().playerInfoShowCoordinates() && online != null ? String.valueOf(Math.round(online.getLocation().getY())) : "-",
                    "z", plugin.cfg().playerInfoShowCoordinates() && online != null ? String.valueOf(Math.round(online.getLocation().getZ())) : "-",
                    "current", String.valueOf(data.homes.size()), "limit", String.valueOf(limit));
            var headItem = ItemBuilder.head(Bukkit.getOfflinePlayer(target))
                    .name(Text.fill(headSec.getString("name", "{target}"), hph))
                    .lore(headSec.getStringList("lore").stream().map(l -> Text.fill(l, hph)).toList())
                    .build();
            set(headSec.getInt("slot", -1), headItem);
        }

        List<Integer> bedSlots = cfg.getIntegerList("bed-slots");
        List<Integer> dyeSlots = cfg.getIntegerList("dye-slots");
        int perPage = Math.max(1, cfg.getInt("homes-per-page", 7));
        int from = page * perPage + 1;
        for (int i = 0; i < perPage && i < bedSlots.size() && i < dyeSlots.size(); i++) {
            int number = from + i;
            if (number > totalSlots) continue;
            renderHome(number, bedSlots.get(i), dyeSlots.get(i));
        }

        ConfigurationSection nav = cfg.getConfigurationSection("navigation");
        if (nav != null) {
            Map<String, String> ph = Text.map("page", String.valueOf(page + 1));
            if (page > 0) {
                set(nav.getInt("previous.slot", -1), gui.item(nav, "previous", ph), e -> {
                    gui.playSound(viewer, gui.soundOf(nav, "previous"));
                    plugin.gui().openPlayerHomes(viewer, target, data, page - 1);
                });
            }
            if (page < totalPages - 1) {
                set(nav.getInt("next.slot", -1), gui.item(nav, "next", ph), e -> {
                    gui.playSound(viewer, gui.soundOf(nav, "next"));
                    plugin.gui().openPlayerHomes(viewer, target, data, page + 1);
                });
            }
            set(nav.getInt("close.slot", -1), gui.item(nav, "close", null), e -> viewer.closeInventory());
        }
    }

    private void renderHome(int number, int bedSlot, int dyeSlot) {
        var gui = plugin.gui();
        Home home = data.homes.get(number);
        String homeName = plugin.cfg().homeName(number);

        if (home != null) {
            Map<String, String> ph = Text.map("home_number", String.valueOf(number), "home", home.name(),
                    "world", home.world(), "x", String.valueOf(Math.round(home.x())),
                    "y", String.valueOf(Math.round(home.y())), "z", String.valueOf(Math.round(home.z())));
            ConfigurationSection sec = cfg.getConfigurationSection("home.saved");
            set(bedSlot, gui.item(sec, "bed", ph), e -> {
                gui.playSound(viewer, gui.soundOf(sec, "bed"));
                startTeleport(home);
            });
            set(dyeSlot, gui.item(sec, "dye", ph), e -> {
                gui.playSound(viewer, gui.soundOf(sec, "dye"));
                plugin.gui().openAdminHomeManage(viewer, target, data, number, page);
            });
        } else if (number <= limit) {
            Map<String, String> ph = Text.map("home_number", String.valueOf(number));
            ConfigurationSection sec = cfg.getConfigurationSection("home.available");
            set(bedSlot, gui.item(sec, "bed", ph));
            set(dyeSlot, gui.item(sec, "dye", ph), e -> plugin.messages().send(viewer, "gui.admin-save-disabled"));
        } else {
            Map<String, String> ph = Text.map("home_number", String.valueOf(number), "required_limit", String.valueOf(number));
            ConfigurationSection sec = cfg.getConfigurationSection("home.locked");
            set(bedSlot, gui.item(sec, "bed", ph));
            set(dyeSlot, gui.item(sec, "dye", ph));
        }
    }

    private void startTeleport(Home h) {
        if (plugin.cfg().worldBlacklisted(h.world()) && !viewer.hasPermission("ultras.homes.bypass.world")) {
            plugin.messages().send(viewer, "teleport.world-blocked");
            return;
        }
        viewer.closeInventory();
        Location dest = LocationUtil.build(h.world(), h.x(), h.y(), h.z(), h.yaw(), h.pitch());
        if (dest == null) {
            plugin.messages().send(viewer, "teleport.invalid-location");
            return;
        }
        if (plugin.cfg().logAdminActions()) {
            plugin.getLogger().info("[ADMIN_TELEPORT] " + viewer.getName() + " -> " + data.name + ":" + h.name());
        }
        plugin.teleport().start(viewer, dest, data.name + ": " + h.name(),
                () -> LocationUtil.isValid(h.world(), h.x(), h.y(), h.z()));
    }
}
