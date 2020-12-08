package me.kyllian.gameboy.listeners;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Pocket;
import nitrous.cpu.Emulator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {

    private GameboyPlugin plugin;

    public PlayerMoveListener(GameboyPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Pocket pocket = plugin.getPlayerHandler().getPocket(player);
        if (pocket.isEmpty()) return;
        double diffX = event.getTo().getX() - event.getFrom().getX();
        double diffZ = event.getTo().getZ() - event.getFrom().getZ();
        pocket.getEmulator().buttonLeft = diffX > 0.1;
        pocket.getEmulator().buttonRight = diffX < -0.1;
        pocket.getEmulator().buttonUp = diffZ > 0.1;
        pocket.getEmulator().buttonDown = diffZ < -0.1;
        pocket.getMovementStopper().update();
        event.setTo(event.getFrom());
    }

}
