package eu.rekawek.coffeegb.gui;

import eu.rekawek.coffeegb.controller.ButtonListener;
import eu.rekawek.coffeegb.controller.ButtonListener.Button;
import eu.rekawek.coffeegb.controller.Controller;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class SwingController implements Controller {

    private ButtonListener listener;

    @Override
    public void setButtonListener(ButtonListener listener) {
        this.listener = listener;
    }

    public void buttonState(Button button, boolean state) {
        if (state) {
            press(button);
        } else {
            release(button);
        }
    }

    public void press(Button button) {
        listener.onButtonPress(button);
    }

    public void release(Button button) {
        listener.onButtonRelease(button);
    }


//    @Override
//    public void keyPressed(KeyEvent e) {
//        if (listener == null) {
//            return;
//        }
//        Button b = getButton(e);
//        if (b != null) {
//            listener.onButtonPress(b);
//        }
//    }
//
//    @Override
//    public void keyReleased(KeyEvent e) {
//        if (listener == null) {
//            return;
//        }
//        Button b = getButton(e);
//        if (b != null) {
//            listener.onButtonRelease(b);
//        }
//    }
}
