package me.kyllian.gameboy.listeners;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Button;
import me.kyllian.gameboy.data.Pocket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerSwapHandItemsListener implements Listener {

    private GameboyPlugin plugin;

    public PlayerSwapHandItemsListener(GameboyPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Pocket pocket = plugin.getPlayerHandler().getPocket(player);
        if (pocket.isEmpty()) return;
        event.setCancelled(true);
        pocket.getButtonToggleHelper().press(Button.BUTTONSTART, true);
    }
}
