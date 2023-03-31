package me.kyllian.gameboy.helpers;

import eu.rekawek.coffeegb.controller.ButtonListener;
import eu.rekawek.coffeegb.gui.Emulator;
import me.kyllian.gameboy.GameboyPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ButtonToggleHelper extends BukkitRunnable {

    private GameboyPlugin plugin;
    private Emulator emulator;

    private Map<ButtonListener.Button, Long> buttonLastPressTimes;
    private int buttonDebounce;

    public ButtonToggleHelper(GameboyPlugin plugin, Emulator emulator) {
        this.plugin = plugin;
        this.emulator = emulator;
        buttonLastPressTimes = new HashMap<>();

        buttonDebounce = plugin.getConfig().getInt("button_debounce");

        runTaskTimerAsynchronously(plugin, 5, 5);
    }

    public void press(ButtonListener.Button button, Boolean state) {
        if (state) buttonLastPressTimes.put(button, System.currentTimeMillis());


        switch (button) {
            case A:
                emulator.getController().buttonState(ButtonListener.Button.A, state);
                break;
            case B:
                emulator.getController().buttonState(ButtonListener.Button.B, state);
                break;
            case DOWN:
                emulator.getController().buttonState(ButtonListener.Button.DOWN, state);
                break;
            case LEFT:
                emulator.getController().buttonState(ButtonListener.Button.LEFT, state);
                break;
            case RIGHT:
                emulator.getController().buttonState(ButtonListener.Button.RIGHT, state);
                break;
            case UP:
                emulator.getController().buttonState(ButtonListener.Button.UP, state);
                break;
            case START:
                emulator.getController().buttonState(ButtonListener.Button.START, state);
                break;
            case SELECT:
                emulator.getController().buttonState(ButtonListener.Button.SELECT, state);
                break;
        }
    }

    @Override
    public void run() {
        Iterator buttonIterator = buttonLastPressTimes.keySet().iterator();
        while (buttonIterator.hasNext()) {
            ButtonListener.Button button = (ButtonListener.Button) buttonIterator.next();
            long lastPressTime = buttonLastPressTimes.get(button);
            if (System.currentTimeMillis() - lastPressTime > buttonDebounce) {
                press(button, false);
                buttonIterator.remove();
            }
        }
    }
}
