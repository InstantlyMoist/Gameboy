package me.kyllian.gameboy.data;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.tasks.MovementStopper;
import me.kyllian.gameboy.tasks.OtherStopper;
import nitrous.Cartridge;
import nitrous.cpu.Emulator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Pocket {

    private Emulator emulator;
    private MovementStopper movementStopper;
    private OtherStopper otherStopper;

    private ItemStack handItem = null;

    public void loadEmulator(GameboyPlugin plugin, Cartridge cartridge, Player player) {
        emulator = new Emulator(cartridge);

        emulator.savefile = new File(plugin.getDataFolder(), "saves/" + player.getUniqueId() + "/" + cartridge.gameTitle + ".sav");
        createSavesFolder(plugin, player);

        handItem = player.getInventory().getItemInMainHand();

        if (emulator.mmu.hasBattery()) {
            try {
                emulator.mmu.load(new FileInputStream(emulator.savefile));
            } catch (IOException exception) {
                // Do nothing
            }
        }

        emulator.codeExecutionThread.start();

        movementStopper = new MovementStopper(plugin, emulator);
        otherStopper = new OtherStopper(plugin, emulator);
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

        movementStopper.cancel();
        otherStopper.cancel();

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

    public MovementStopper getMovementStopper() {
        return movementStopper;
    }

    public OtherStopper getOtherStopper() {
        return otherStopper;
    }
}
