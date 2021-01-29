package me.kyllian.gameboy.handlers;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Pocket;
import nitrous.Cartridge;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerHandler {

    private GameboyPlugin plugin;

    private Map<Player, Pocket> pockets;

    public PlayerHandler(GameboyPlugin plugin) {
        this.plugin = plugin;

        pockets =  new HashMap<>();
    }

    public void loadGame(Player player, Cartridge cartridge) {
        try {
            plugin.notifyEmulate();
            getPocket(player).loadEmulator(plugin, cartridge, player);
            plugin.getMapHandler().sendMap(player);
            if (plugin.isProtocolLib()) return;
            Location playerLocation = player.getLocation();
            playerLocation.setYaw(0);
            playerLocation.setPitch(40);
            playerLocation.setX(playerLocation.getBlockX() + 0.5);
            playerLocation.setY(playerLocation.getBlockY());
            playerLocation.setZ(playerLocation.getBlockZ() + 0.5);
            player.teleport(playerLocation);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public Pocket getPocket(Player player) {
        return pockets.computeIfAbsent(player, f -> new Pocket());
    }

    public void removePocket(Player player) {
        pockets.remove(player);
    }
}
