package me.kyllian.gameboy.data;

import eu.rekawek.coffeegb.gui.Emulator;
import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.helpers.ButtonToggleHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Arrays;

public class Pocket {

    private BukkitTask arrowDespawnHandler;

    private GameboyPlugin plugin;

    private Emulator emulator;

    private ButtonToggleHelper buttonToggleHelper;

    private ItemStack handItem = null;
    private Entity arrow;

    public void loadEmulator(GameboyPlugin plugin, Player player, String gameFile) {
        this.plugin = plugin;

        createSavesFolder(plugin, player);
        File saveFile = new File(plugin.getDataFolder(), "saves/" + player.getUniqueId());

        try {
            emulator = new Emulator(gameFile, saveFile);
            emulator.run();
        } catch (Exception e) {
            Bukkit.getLogger().info("GAMEBOY: error");
            e.printStackTrace();
        }

        //emulator.savefile = new File(plugin.getDataFolder(), "saves/" + player.getUniqueId() + "/" + cartridge.gameTitle + ".sav");

        buttonToggleHelper = new ButtonToggleHelper(plugin, emulator);

        handItem = player.getInventory().getItemInMainHand();

//        if (emulator.mmu.hasBattery()) {
//            try {
//                emulator.mmu.load(new FileInputStream(emulator.savefile));
//            } catch (IOException exception) {
//                // Assume file either failed or doesn't exist.
//            }
//        }

//        emulator.codeExecutionThread.start();

        arrowDespawnHandler = new BukkitRunnable() {
            @Override
            public void run() {
                arrow.setTicksLived(1);
            }
        }.runTaskTimerAsynchronously(plugin, 20, 20);
    }

    public void stopEmulator(Player player) {
//        try (FileOutputStream f = new FileOutputStream(emulator.savefile)) {
//            if (emulator.mmu.hasBattery()) {
//                emulator.mmu.save(f);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if (plugin.isProtocolLib()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    arrow.remove();
                    arrow = null;
                }
            }.runTask(plugin);
        }
        player.getInventory().setItemInMainHand(handItem);
        handItem = null;

        emulator.stop();

//        emulator.codeExecutionThread.interrupt();

        buttonToggleHelper.cancel();

        emulator = null;

        arrowDespawnHandler.cancel();
        arrowDespawnHandler = null;
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

    public Entity getArrow() {
        return arrow;
    }

    public void setArrow(Entity arrow) {
        this.arrow = arrow;
    }
}
