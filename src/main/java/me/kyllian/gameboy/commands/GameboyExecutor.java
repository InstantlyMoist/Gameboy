package me.kyllian.gameboy.commands;

import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Pocket;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import nitrous.Cartridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;

public class GameboyExecutor implements CommandExecutor {

    private GameboyPlugin plugin;

    public GameboyExecutor(GameboyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorTranslate("&cThis command can only be run by players"));
            return true;
        }
        Player player = (Player) sender;
        Pocket pocket = plugin.getPlayerHandler().getPocket(player);
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("stop")) {
                if (pocket.isEmpty()) {
                    player.sendMessage(colorTranslate("&cNo running game!"));
                    return true;
                }
                pocket.stopEmulator(player);
                player.sendMessage(colorTranslate("&aStopped game successfully"));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                try {
                    plugin.getRomHandler().loadRoms();
                    sender.sendMessage(colorTranslate("&AReloading complete!"));
                } catch (IOException exception) {
                    sender.sendMessage(colorTranslate("&cReloading failed!"));
                }
                return true;
            }
        }
        if (args.length > 1) {
            if (args[0].equalsIgnoreCase("play")) {
                if (!pocket.isEmpty()) {
                    player.sendMessage(colorTranslate("&cAlready playing a game, stop by doing /gameboy stop"));
                    return true;
                }
                String gameName = "";
                for (int i = 1; i != args.length; i++) {
                    gameName += args[i] + " ";
                }
                gameName = gameName.trim().toUpperCase();
                Cartridge foundCartridge = plugin.getRomHandler().getRoms().get(gameName);
                if (foundCartridge == null) {
                    sender.sendMessage(colorTranslate("&cGame not found!"));
                    showHelp(sender);
                    return true;
                }
                if (plugin.isProtocolLib()) {
                    Entity entity = player.getLocation().getWorld().spawnEntity(player.getLocation(), EntityType.ARROW);
                    entity.addPassenger(player);
                    entity.setSilent(true);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);

                    pocket.setArrow(entity);
                }
                player.sendMessage(colorTranslate("&aNow playing: " + foundCartridge.gameTitle + (plugin.isProtocolLib() ? "\n&aSneak to stop playing!" :"\n&aType /gameboy stop to stop playing!")));
                plugin.getPlayerHandler().loadGame(player, foundCartridge);
                return true;
            }
        }
        showHelp(sender);
        return true;
    }

    public void showHelp(CommandSender sender) {
        BaseComponent component = new TextComponent(colorTranslate("&7The current games you can play are: "));
        if (plugin.getRomHandler().getRoms().isEmpty()) {
            sender.sendMessage(colorTranslate("&7No games found!\n\n&7Need help installing them? Join my discord: &7https://discord.gg/zgKr2Y"));
            return;
        }
        plugin.getRomHandler().getRoms().keySet().forEach(rom -> {
            TextComponent romClick = new TextComponent(colorTranslate("\n&7" + rom));
            romClick.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gameboy play " + rom));
            try {
                romClick.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(colorTranslate("&7Click here to play " + rom))));
            } catch (NoClassDefFoundError ignored) {
                // Hover not found, just don't add it
            }
            component.addExtra(romClick);
        });
        component.addExtra(colorTranslate("\n&7Type /gameboy play 'name' to play a game!\n&7Unsure how the plugin works? Join my discord: &7https://discord.gg/zgKr2YM"));
        sender.spigot().sendMessage(component);
        }

    public String colorTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
