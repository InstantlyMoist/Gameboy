package me.kyllian.gameboy;

import me.kyllian.gameboy.commands.GameboyExecutor;
import me.kyllian.gameboy.handlers.PlayerHandler;
import me.kyllian.gameboy.handlers.RomHandler;
import me.kyllian.gameboy.handlers.map.MapHandler;
import me.kyllian.gameboy.handlers.map.MapHandlerFactory;
import me.kyllian.gameboy.listeners.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class GameboyPlugin extends JavaPlugin {

    private MapHandler mapHandler;
    private PlayerHandler playerHandler;
    private RomHandler romHandler;

    @Override
    public void onEnable() {
        super.onEnable();

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        mapHandler = new MapHandlerFactory(this).getMapHandler();
        playerHandler = new PlayerHandler(this);
        romHandler = new RomHandler(this);

        new Metrics(this, 9592);

        mapHandler.loadData();

        getCommand("gameboy").setExecutor(new GameboyExecutor(this));

        new PlayerDropItemListener(this);
        new PlayerInteractListener(this);
        new PlayerItemHeldListener(this);
        new PlayerMoveListener(this);
        new PlayerToggleSneakListener(this);
    }

    public MapHandler getMapHandler() {
        return mapHandler;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public RomHandler getRomHandler() {
        return romHandler;
    }
}
