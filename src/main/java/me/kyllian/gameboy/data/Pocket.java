package me.kyllian.gameboy.data;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.helpers.ButtonToggleHelper;
import nitrous.Cartridge;
import nitrous.cpu.Emulator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Pocket {

    private Emulator emulator;

    private ButtonToggleHelper buttonToggleHelper;

    private ItemStack handItem = null;

    public void loadEmulator(GameboyPlugin plugin, Cartridge cartridge, Player player) {
        emulator = new Emulator(cartridge);

        emulator.savefile = new File(plugin.getDataFolder(), "saves/" + player.getUniqueId() + "/" + cartridge.gameTitle + ".sav");
        createSavesFolder(plugin, player);

        buttonToggleHelper = new ButtonToggleHelper(plugin, emulator);

        handItem = player.getInventory().getItemInMainHand();

        if (emulator.mmu.hasBattery()) {
            try {
                emulator.mmu.load(new FileInputStream(emulator.savefile));
            } catch (IOException exception) {
                // Assume file either failed or doesn't exist.
            }
        }

        emulator.codeExecutionThread.start();
    }

    public void stopEmulator(Player player) {
        try (FileOutputStream f = new FileOutputStream(emulator.savefile)) {
            if (emulator.mmu.hasBattery()) {
                emulator.mmu.save(f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.getInventory().setItemInMainHand(handItem);
        handItem = null;

        emulator.codeExecutionThread.stop();

        buttonToggleHelper.cancel();

        emulator = null;
    }

    public void createSavesFolder(GameboyPlugin plugin, Player player) {
        File savesFolder = new File(plugin.getDataFolder(), "saves/" + player.getUniqueId().toString());
        savesFolder.mkdirs();
    }

    public boolean isEmpty() {
        return emulator == null;
    }

    public Emulator getEmulator() {
        return emulator;
    }

    public ButtonToggleHelper getButtonToggleHelper() {
        return buttonToggleHelper;
    }
}
