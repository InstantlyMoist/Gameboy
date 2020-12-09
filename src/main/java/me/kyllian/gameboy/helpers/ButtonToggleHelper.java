package me.kyllian.gameboy.helpers;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Button;
import nitrous.cpu.Emulator;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ButtonToggleHelper extends BukkitRunnable {

    private GameboyPlugin plugin;
    private Emulator emulator;

    private Map<Button, Long> buttonLastPressTimes;
    private int buttonDebounce;

    public ButtonToggleHelper(GameboyPlugin plugin, Emulator emulator) {
        this.plugin = plugin;
        this.emulator = emulator;
        buttonLastPressTimes = new HashMap<>();

        buttonDebounce = plugin.getConfig().getInt("button_debounce");

        runTaskTimer(plugin, 5,5);
    }

    public void press(Button button, Boolean state) {
        if (state) buttonLastPressTimes.put(button, System.currentTimeMillis());


        switch (button) {
            case BUTTONA:
                emulator.buttonA = state;
                break;
            case BUTTONB:
                emulator.buttonB = state;
                break;
            case BUTTONDOWN:
                emulator.buttonDown = state;
                break;
            case BUTTONLEFT:
                emulator.buttonLeft = state;
                break;
            case BUTTONRIGHT:
                emulator.buttonRight = state;
                break;
            case BUTTONUP:
                emulator.buttonUp = state;
                break;
            case BUTTONSTART:
                emulator.buttonStart = state;
                break;
            case BUTTONSELECT:
                emulator.buttonSelect = state;
                break;
        }
    }

    @Override
    public void run() {
        Iterator buttonIterator = buttonLastPressTimes.keySet().iterator();
        while (buttonIterator.hasNext()) {
            Button button = (Button) buttonIterator.next();
            long lastPressTime = buttonLastPressTimes.get(button);
            if (System.currentTimeMillis() - lastPressTime > buttonDebounce) {
                press(button, false);
                buttonIterator.remove();
            }
        }
    }
}
