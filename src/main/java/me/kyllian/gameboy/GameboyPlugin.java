package me.kyllian.gameboy;

import me.kyllian.gameboy.commands.GameboyExecutor;
import me.kyllian.gameboy.data.Pocket;
import me.kyllian.gameboy.handlers.MessageHandler;
import me.kyllian.gameboy.handlers.PlayerHandler;
import me.kyllian.gameboy.handlers.RomHandler;
import me.kyllian.gameboy.handlers.map.MapHandler;
import me.kyllian.gameboy.handlers.map.MapHandlerFactory;
import me.kyllian.gameboy.listeners.*;
import me.kyllian.gameboy.listeners.packets.SteerVehicleListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class GameboyPlugin extends JavaPlugin {

    private int gamesEmulated = 0;

    private boolean protocolLib;

    private MapHandler mapHandler;
    private MessageHandler messageHandler;
    private PlayerHandler playerHandler;
    private RomHandler romHandler;

    @Override
    public void onEnable() {
        super.onEnable();

        protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        mapHandler = new MapHandlerFactory(this).getMapHandler();
        messageHandler = new MessageHandler(this);
        playerHandler = new PlayerHandler(this);
        romHandler = new RomHandler(this);

        Metrics metrics = new Metrics(this, 9592);
        metrics.addCustomChart(new Metrics.SingleLineChart("games_emulated", () ->
                gamesEmulated));
        metrics.addCustomChart(new Metrics.AdvancedPie("games_installed", () -> {
            Map<String, Integer> values = new HashMap<>();
            romHandler.getRoms().keySet().forEach(romName -> {
                values.put(romName, 1);
            });
            return values;
        }));


        mapHandler.loadData();

        getCommand("gameboy").setExecutor(new GameboyExecutor(this));

        new InventoryClickListener(this);
        new PlayerDropItemListener(this);
        new PlayerInteractEntityListener(this);
        new PlayerInteractListener(this);
        new PlayerItemHeldListener(this);
        if (!protocolLib) new PlayerMoveListener(this);
        new PlayerQuitListener(this);
        new PlayerSwapHandItemsListener(this);

        if (protocolLib) new SteerVehicleListener(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();

        Bukkit.getOnlinePlayers().forEach(player -> {
            Pocket pocket = playerHandler.getPocket(player);
            if (!pocket.isEmpty()) pocket.stopEmulator(player);
        });
    }

    public MapHandler getMapHandler() {
        return mapHandler;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public RomHandler getRomHandler() {
        return romHandler;
    }

    public void notifyEmulate() {
        gamesEmulated++;
    }

    public boolean isProtocolLib() {
        return protocolLib;
    }
}
