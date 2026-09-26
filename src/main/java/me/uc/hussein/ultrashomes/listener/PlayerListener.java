package me.uc.hussein.ultrashomes.listener;

import me.uc.hussein.ultrashomes.UltrasHomesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Saves and schedules unload of a player's home data when they leave. Nothing is loaded on join. */
public final class PlayerListener implements Listener {
    private final UltrasHomesPlugin plugin;

    public PlayerListener(UltrasHomesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.homes().onQuit(p);
        plugin.teleport().cancel(p.getUniqueId(), null);
        plugin.teleport().protectionManager().clear(p.getUniqueId());
    }
}
