package me.kyllian.gameboy.handlers.map;

import me.kyllian.gameboy.GameboyPlugin;
import org.bukkit.Bukkit;

public class MapHandlerFactory {

    private GameboyPlugin plugin;

    public MapHandlerFactory(GameboyPlugin plugin) {
        this.plugin = plugin;
    }

    public MapHandler getMapHandler() {
        String minecraftVersion = Bukkit.getVersion();
        int mainVer = Integer.parseInt(minecraftVersion.split("\\.")[1]);
        return mainVer >= 13 ? new MapHandlerNew(plugin) : new MapHandlerOld(plugin);
    }
}
