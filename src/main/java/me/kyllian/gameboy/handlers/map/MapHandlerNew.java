package me.kyllian.gameboy.handlers.map;

import eu.decentsoftware.holograms.api.DHAPI;
import me.kyllian.gameboy.GameboyPlugin;

import me.kyllian.gameboy.data.Pocket;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.makers.FixedSizeThumbnailMaker;
import net.coobird.thumbnailator.resizers.DefaultResizerFactory;
import net.coobird.thumbnailator.resizers.Resizer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.ChatColor.COLOR_CHAR;

public class MapHandlerNew implements MapHandler {

    private GameboyPlugin plugin;

    private File file;
    private FileConfiguration fileConfiguration;

    private Map<ItemStack, Boolean> mapsUsing;


    public MapHandlerNew(GameboyPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void loadData() {
        mapsUsing = new HashMap<>();
        file = new File(plugin.getDataFolder(), "maps.yml");
        if (!file.exists()) plugin.saveResource("maps.yml", false);
        fileConfiguration = YamlConfiguration.loadConfiguration(file);
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

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    public static String translateHexColorCodes(String message) {
        //Sourced from this post by imDaniX: https://github.com/SpigotMC/BungeeCord/pull/2883#issuecomment-653955600
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
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

        mapView.addRenderer(new MapRenderer() {
            Pocket pocket = plugin.getPlayerHandler().getPocket(player);

            @Override
            public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
                if (pocket.getEmulator() == null) return;
                BufferedImage image = pocket.getEmulator().lcd.freeBufferFrame;
                for (int y = 0; y < image.getHeight(); y++) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int x = 0; x < image.getWidth(); x++) {
                        String hex = Integer.toHexString(image.getRGB(x, y));

                        stringBuilder.append(translateHexColorCodes("&#" + hex.substring(2, 8))).append("█");
                    }
                    DHAPI.setHologramLine(plugin.hologram, y, stringBuilder.toString());
                }
//                try {
//                    Resizer resizer = DefaultResizerFactory.getInstance().getResizer(new Dimension(160, 144), new Dimension(128, 128));
//                    BufferedImage scaled = new FixedSizeThumbnailMaker(128, 128, true, true).resizer(resizer).make(pocket.getEmulator().lcd.freeBufferFrame);
//                    BufferedImage newImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
//                    Graphics graphics = newImage.getGraphics();
//                    graphics.setColor(Color.white);
//                    graphics.fillRect(0, 0, 128, 128);
//                    graphics.drawImage(scaled, 0, 6, null);
//                    mapCanvas.drawImage(0, 0, newImage);
//                } catch (Exception exception) {
//                    exception.printStackTrace();
//                }
            }
        });
        map.setItemMeta(mapMeta);
        player.getInventory().setItemInMainHand(map);
    }

    public void resetMap(ItemStack map) {
        mapsUsing.put(map, false);
    }
}
