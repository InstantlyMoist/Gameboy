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
            sender.sendMessage(plugin.getMessageHandler().getMessage("player-only"));
            return true;
        }
        Player player = (Player) sender;
        Pocket pocket = plugin.getPlayerHandler().getPocket(player);
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("stop")) {
                if (pocket.isEmpty()) {
                    player.sendMessage(plugin.getMessageHandler().getMessage("no-game"));
                    return true;
                }
                pocket.stopEmulator(player);
                player.sendMessage(plugin.getMessageHandler().getMessage("stopped"));
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
                    player.sendMessage(plugin.getMessageHandler().getMessage("already-running"));
                    return true;
                }
                String gameName = "";
                for (int i = 1; i != args.length; i++) {
                    gameName += args[i] + " ";
                }
                gameName = gameName.trim().toUpperCase();
                Cartridge foundCartridge = plugin.getRomHandler().getRoms().get(gameName);
                if (foundCartridge == null) {
                    sender.sendMessage(plugin.getMessageHandler().getMessage("not-found"));
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
                player.sendMessage(plugin.getMessageHandler().getMessage(plugin.isProtocolLib() ? "now-playing-protocollib" : "now-playing-normal").replace("%gamename%", foundCartridge.gameTitle));
                plugin.getPlayerHandler().loadGame(player, foundCartridge);
                return true;
            }
        }
        showHelp(sender);
        return true;
    }

    public void showHelp(CommandSender sender) {
        BaseComponent component = new TextComponent(plugin.getMessageHandler().getMessage("playable"));
        if (plugin.getRomHandler().getRoms().isEmpty()) {
            sender.sendMessage(plugin.getMessageHandler().getMessage("no-games"));
            return;
        }
        plugin.getRomHandler().getRoms().keySet().forEach(rom -> {
            TextComponent romClick = new TextComponent(colorTranslate("\n" + plugin.getMessageHandler().getMessage("gamename-prefix") + rom));
            romClick.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gameboy play " + rom));
            try {
                romClick.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getMessageHandler().getMessage("click-to-play").replace("%gamename%", rom))));
            } catch (NoClassDefFoundError ignored) {
                // Hover not found, just don't add it
            }
            component.addExtra(romClick);
        });
        component.addExtra(plugin.getMessageHandler().getMessage("instructions"));
        sender.spigot().sendMessage(component);
        }

    public String colorTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
