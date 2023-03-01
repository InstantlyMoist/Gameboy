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
        String mainVerString = minecraftVersion.split("\\.")[1];
        mainVerString = mainVerString.replace(")", "");
        mainVerString = mainVerString.replace("(", "");
        int mainVer = Integer.parseInt(mainVerString);
        return mainVer >= 13 ? new MapHandlerNew(plugin) : new MapHandlerOld(plugin);
    }
}
