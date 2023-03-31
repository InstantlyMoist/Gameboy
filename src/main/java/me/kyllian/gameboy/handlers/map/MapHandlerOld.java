package me.kyllian.gameboy.handlers.map;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Pocket;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapHandlerOld implements MapHandler {

    private final GameboyPlugin plugin;

    private Map<ItemStack, Boolean> mapsUsing;

    public MapHandlerOld(GameboyPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void loadData() {
        mapsUsing = new HashMap<>();
        File file = new File(plugin.getDataFolder(), "maps.yml");
        if (!file.exists()) plugin.saveResource("maps.yml", false);
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
        List<Integer> maps = fileConfiguration.getIntegerList("maps");
        int mapAmount = plugin.getConfig().getInt("gameboys");
        int currentMapAmount = maps.size();
        if (mapAmount > currentMapAmount) {
            Bukkit.getLogger().info("Gameboy didn't find existing, predefined maps. Generating them, this may take some time...");
            World world = Bukkit.getWorlds().get(0);
            for (int i = 0; i != mapAmount - currentMapAmount; i++) {
                MapView mapView = Bukkit.createMap(world);
                try {
                    Method method = mapView.getClass().getMethod("getId");
                    maps.add(((Short)method.invoke(mapView)).intValue());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            fileConfiguration.set("maps", maps);
            world.save();
            try {
                fileConfiguration.save(file);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        maps.forEach(mapID -> {
            ItemStack map = new ItemStack(Material.MAP);
            map.setDurability(mapID.shortValue());
            mapsUsing.put(map, false);
        });
    }

    public void sendMap(Player player) {
        ItemStack map = mapsUsing.entrySet()
                .stream()
                .filter(mapValue -> !mapValue.getValue())
                .findFirst()
                .get()
                .getKey();

        mapsUsing.put(map, true);

        MapView mapView = null;
        Class bukkitClass = null;

        try {
            bukkitClass = Class.forName("org.bukkit.Bukkit");
            Method getMapInt = bukkitClass.getMethod("getMap", int.class);
            mapView = (MapView) getMapInt.invoke(bukkitClass, new Object[] {map.getDurability()});
        } catch (Exception exception) {
            try {
                Method getMapShort = bukkitClass.getMethod("getMap", short.class);
                mapView = (MapView) getMapShort.invoke(bukkitClass, new Object[] {map.getDurability()});
            } catch (Exception otherException) {
                otherException.printStackTrace();
            }
        }
        if (mapView.getRenderers() != null) mapView.getRenderers().clear();

        MapRenderer renderer = new MapRenderer() {
            final Pocket pocket = plugin.getPlayerHandler().getPocket(player);

            @Override
            public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
//                int height = pocket.getEmulator().lcd.freeBufferArrayByte.length / 128;
//
//                byte[] pixels = pocket.getEmulator().lcd.freeBufferArrayByte.clone();
//
//                for (int x = 0; x < 128; x++) {
//                    for (int y = 0; y < height; y++) {
//                        mapCanvas.setPixel(x, y, pixels[x + (y * 128)]);
//                    }
//                }
            }
        };

        mapView.addRenderer(renderer);
        MapView finalMapView = mapView;

        int tickDelay = 1;
        if (plugin.getConfig().get("tick_update_delay") != null) tickDelay = plugin.getConfig().getInt("tick_update_delay");
        new BukkitRunnable() {
            final Pocket pocket = plugin.getPlayerHandler().getPocket(player);
            @Override
            public void run() {
                if (pocket.getEmulator() == null) {
                    finalMapView.removeRenderer(renderer);
                    cancel();
                    return;
                }
                player.sendMap(finalMapView);
            }
        }.runTaskTimer(plugin, tickDelay, tickDelay);

        player.getInventory().setItemInMainHand(map);
    }

    public void resetMap(ItemStack map) {
        mapsUsing.put(map, false);
    }
}
