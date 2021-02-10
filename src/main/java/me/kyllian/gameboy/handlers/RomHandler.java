package me.kyllian.gameboy.handlers;

import me.kyllian.gameboy.GameboyPlugin;
import nitrous.Cartridge;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class RomHandler {

    private GameboyPlugin plugin;

    private Map<String, Cartridge> roms;
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

        for (File rom : romFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(".gb") || name.endsWith(".gbc"));
            }
        })) {
            Cartridge cartridge = new Cartridge(Files.readAllBytes(rom.toPath()));
            roms.put(roms.get(cartridge.gameTitle) != null ? cartridge.gameTitle + "_1" : cartridge.gameTitle, cartridge);
        }
    }

    public Map<String, Cartridge> getRoms() {
        return roms;
    }
}
