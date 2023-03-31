package eu.rekawek.coffeegb.gui;

import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.sound.SoundOutput;
import org.bukkit.Bukkit;

public class AudioSystemSoundOutput implements SoundOutput, Runnable {

    private final int N = 65536;
    private final long millis = (N * 1000) / Gameboy.TICKS_PER_SEC;

    private volatile boolean doStop;
    private volatile boolean isPlaying;

    private long startTime = System.currentTimeMillis();
    private long endTime = startTime;

    @Override
    public void run() {

    }

    public void stopThread() {
        doStop = true;
    }

    @Override
    public void start() {
        isPlaying = true;
    }

    @Override
    public void stop() {
        isPlaying = false;
    }

    private int ticks;

    @Override
    public void play(int left, int right) {
        ticks++;
        if (ticks < N) {
            return;
        }
        long end = System.currentTimeMillis();
        long diff = end - startTime;
        long timeToSleep = millis - diff;

        if (timeToSleep > 0) {
            try {
                Thread.sleep(timeToSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ticks = 0;
        startTime = System.currentTimeMillis();
    }
}
