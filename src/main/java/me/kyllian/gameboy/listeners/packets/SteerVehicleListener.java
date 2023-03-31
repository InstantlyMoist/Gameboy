package me.kyllian.gameboy.listeners.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import eu.rekawek.coffeegb.controller.ButtonListener;
import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Pocket;
import org.bukkit.entity.Player;

public class SteerVehicleListener {

    public SteerVehicleListener(GameboyPlugin gameboyPlugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(gameboyPlugin, PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                Pocket pocket = gameboyPlugin.getPlayerHandler().getPocket(player);
                if (pocket.isEmpty()) return;
                PacketContainer container = event.getPacket();
                float sideways = container.getFloat().read(0);
                float forward = container.getFloat().read(1);
                pocket.getButtonToggleHelper().press(ButtonListener.Button.LEFT, sideways > 0);
                pocket.getButtonToggleHelper().press(ButtonListener.Button.RIGHT, sideways < 0);
                pocket.getButtonToggleHelper().press(ButtonListener.Button.UP, forward > 0);
                pocket.getButtonToggleHelper().press(ButtonListener.Button.DOWN, forward < 0);
                pocket.getButtonToggleHelper().press(ButtonListener.Button.A, container.getBooleans().read(0));
                if (container.getBooleans().read(1)) {
                    pocket.stopEmulator(player);
                    player.sendMessage(gameboyPlugin.getMessageHandler().getMessage("stopped"));
                    return;
                }
            }
        });
    }
}
