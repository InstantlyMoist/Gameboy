package me.kyllian.gameboy.handlers.map;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.kyllian.gameboy.GameboyPlugin;

import me.kyllian.gameboy.data.Pocket;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MapHandlerNew implements MapHandler {

    private final GameboyPlugin plugin;

    private Map<ItemStack, Boolean> mapsUsing;

    BufferedImage image;


    public MapHandlerNew(GameboyPlugin plugin) {
        this.plugin = plugin;
        loadData();

        image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 128, 128);
        graphics.dispose();
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
                maps.add((int) mapView.getId());
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
            ItemStack map = new ItemStack(Material.valueOf("FILLED_MAP"));
            MapMeta meta = (MapMeta) map.getItemMeta();
            meta.setMapId((int) mapID.shortValue());
            map.setItemMeta(meta);
            //map.setDurability(mapID.shortValue());
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

        MapMeta mapMeta = (MapMeta) map.getItemMeta();

        MapView mapView = mapMeta.getMapView();
        if (mapView.getRenderers() != null) mapView.getRenderers().clear();

        MapRenderer renderer = new MapRenderer() {
            final Pocket pocket = plugin.getPlayerHandler().getPocket(player);

            @Override
            public void render(@NonNull MapView mapView, @NonNull MapCanvas mapCanvas, @NonNull Player player) {
                int height = pocket.getEmulator().lcd.freeBufferArrayByte.length / 128;

                byte[] pixels = pocket.getEmulator().lcd.freeBufferArrayByte.clone();

                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < height; y++) {
                        mapCanvas.setPixel(x, y, pixels[x + (y * 128)]);
                    }
                }
            }
        };

        mapView.addRenderer(renderer);

        int tickDelay = 1;
        if (plugin.getConfig().get("tick_update_delay") != null) tickDelay = plugin.getConfig().getInt("tick_update_delay");
        new BukkitRunnable() {
            final Pocket pocket = plugin.getPlayerHandler().getPocket(player);
            @Override
            public void run() {
                if (pocket.getEmulator() == null) {
                    mapView.removeRenderer(renderer);
                    cancel();
                    return;
                }
                player.sendMap(mapView);
            }
        }.runTaskTimer(plugin, tickDelay, tickDelay);


        map.setItemMeta(mapMeta);
        player.getInventory().setItemInMainHand(map);
    }

    public void resetMap(ItemStack map) {
        mapsUsing.put(map, false);
    }
}
