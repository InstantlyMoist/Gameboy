package eu.rekawek.coffeegb.gui;

import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.GameboyOptions;
import eu.rekawek.coffeegb.controller.Controller;
import eu.rekawek.coffeegb.debug.Console;
import eu.rekawek.coffeegb.memory.cart.Cartridge;
import eu.rekawek.coffeegb.serial.SerialEndpoint;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Emulator {

    private static final int SCALE = 2;

    private final GameboyOptions options;

    private final Cartridge rom;

    private final AudioSystemSoundOutput sound;

    private final SwingDisplay display;

    private final SwingController controller;

    private final SerialEndpoint serialEndpoint;

    private final Gameboy gameboy;

    private final Optional<Console> console;

    public Emulator(String romName, File savePath) throws IOException {
        String[] args = Arrays.asList(romName).toArray(new String[0]);
        options = parseArgs(args);
        rom = new Cartridge(options, savePath);
        serialEndpoint = SerialEndpoint.NULL_ENDPOINT;
        console = options.isDebug() ? Optional.of(new Console()) : Optional.empty();
        console.map(Thread::new).ifPresent(Thread::start);


        sound = new AudioSystemSoundOutput();
        display = new SwingDisplay(SCALE, options.isGrayscale());
        controller = new SwingController();
        gameboy = new Gameboy(options, rom, display, controller, sound , serialEndpoint, console);
        console.ifPresent(c -> c.init(gameboy));
    }

    private static GameboyOptions parseArgs(String[] args) {
        if (args.length == 0) {
            GameboyOptions.printUsage(System.out);
            System.exit(0);
            return null;
        }
        try {
            return createGameboyOptions(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println();
            GameboyOptions.printUsage(System.err);
            System.exit(1);
            return null;
        }
    }

    private static GameboyOptions createGameboyOptions(String[] args) {
        Set<String> params = new HashSet<>();
        Set<String> shortParams = new HashSet<>();
        String romPath = null;
        for (String a : args) {
            if (a.startsWith("--")) {
                params.add(a.substring(2));
            } else if (a.startsWith("-")) {
                shortParams.add(a.substring(1));
            } else {
                romPath = a;
            }
        }
        if (romPath == null) {
            throw new IllegalArgumentException("ROM path hasn't been specified");
        }
        File romFile = new File(romPath);
        if (!romFile.exists()) {
            throw new IllegalArgumentException("The ROM path doesn't exist: " + romPath);
        }
        return new GameboyOptions(romFile, params, shortParams);
    }

    public void run() throws Exception {
        startGui();
    }

    private void startGui() {
        new Thread(display).start();
        new Thread(sound).start();
        new Thread(gameboy).start();
    }

    public void stop() {
        display.stop();
        sound.stopThread();
        gameboy.stop();
        rom.flushBattery();
    }

    public SwingDisplay getDisplay() {
        return display;
    }

    public SwingController getController() {
        return controller;
    }
}
