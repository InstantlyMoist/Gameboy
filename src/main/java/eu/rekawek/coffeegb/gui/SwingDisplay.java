package eu.rekawek.coffeegb.gui;

import eu.rekawek.coffeegb.gpu.Display;
import net.coobird.thumbnailator.makers.FixedSizeThumbnailMaker;
import net.coobird.thumbnailator.makers.ThumbnailMaker;
import net.coobird.thumbnailator.resizers.DefaultResizerFactory;
import net.coobird.thumbnailator.resizers.Resizer;
import org.bukkit.Bukkit;
import org.bukkit.map.MapPalette;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SwingDisplay implements Display, Runnable {

    private final BufferedImage img;

    public byte[] freeBufferArrayByte = new byte[23040];

    public static final int[] COLORS = new int[]{0xe6f8da, 0x99c886, 0x437969, 0x051f2a};

    public static final int[] COLORS_GRAYSCALE = new int[]{0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000};

    private final int[] rgb;

    private final int[] waitingFrame;

    private boolean enabled;

    private int scale;

    private boolean doStop;

    private boolean frameIsWaiting;

    private boolean grayscale;

    private int pos;

    public SwingDisplay(int scale, boolean grayscale) {
        super();
        GraphicsConfiguration gfxConfig = GraphicsEnvironment.
                getLocalGraphicsEnvironment().getDefaultScreenDevice().
                getDefaultConfiguration();
        img = gfxConfig.createCompatibleImage(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        rgb = new int[DISPLAY_WIDTH * DISPLAY_HEIGHT];
        waitingFrame = new int[rgb.length];
        this.scale = scale;
        this.grayscale = grayscale;
    }

    @Override
    public void putDmgPixel(int color) {
        rgb[pos++] = grayscale ? COLORS_GRAYSCALE[color] : COLORS[color];
        pos = pos % rgb.length;
    }

    @Override
    public void putColorPixel(int gbcRgb) {
        rgb[pos++] = Display.translateGbcRgb(gbcRgb);
    }

    @Override
    public synchronized void frameIsReady() {
        pos = 0;
        if (frameIsWaiting) {
            return;
        }
        frameIsWaiting = true;
        for (int i = 0; i < rgb.length; i++) {
            waitingFrame[i] = rgb[i];
        }
        notify();
    }

    @Override
    public void enableLcd() {
        enabled = true;
    }

    @Override
    public void disableLcd() {
        enabled = false;
    }

    @Override
    public void run() {
        doStop = false;
        frameIsWaiting = false;
        enabled = true;
        while (!doStop) {
            synchronized (this) {
                if (frameIsWaiting) {
                    img.setRGB(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, waitingFrame, 0, DISPLAY_WIDTH);

                    Resizer resizer = DefaultResizerFactory.getInstance().getResizer(new Dimension(160, 144), new Dimension(128, 128));
                    ThumbnailMaker thumbnailMaker = (new FixedSizeThumbnailMaker(128, 128, true, true)).resizer(resizer);
                    BufferedImage scaled = thumbnailMaker.make(img);

                    // Do the bukkit map magic
                    for (int x = 0; x < scaled.getWidth(); x++) {
                        for (int y = 0; y < scaled.getHeight(); y++) {
                            Color color = new Color(scaled.getRGB(x, y));
                            freeBufferArrayByte[x + y * 128] = MapPalette.matchColor(color);
                        }
                    }

                    frameIsWaiting = false;
                } else {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public BufferedImage getImg() {
        return img;
    }

    public void stop() {
        doStop = true;
    }
}
