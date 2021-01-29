package me.kyllian.gameboy.utils;

import org.bukkit.ChatColor;

public class ColorTranslate {

    public static String colorTranslate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
