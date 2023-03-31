package me.kyllian.gameboy.handlers;

import me.kyllian.gameboy.GameboyPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RomHandler {

    private GameboyPlugin plugin;

    private Map<String, String> roms;
    private File romFolder;

    public RomHandler(GameboyPlugin plugin) {
        this.plugin = plugin;

        romFolder = new File(plugin.getDataFolder(), "roms");
        if (!romFolder.exists()) romFolder.mkdirs();

        try {
            loadRoms();
        } catch (IOException exception) {
            // TODO: Make this fail and throw a nice error
        }
    }

    public void loadRoms() throws IOException {
        roms = new HashMap<>();

        for (File rom : Objects.requireNonNull(romFolder.listFiles((dir, name) -> (name.endsWith(".gb") || name.endsWith(".gbc"))))) {
            String path = rom.getAbsolutePath();
            String name = rom.getName();
            roms.put(name, path);
        }
    }

    public Map<String, String> getRoms() {
        return roms;
    }
}
