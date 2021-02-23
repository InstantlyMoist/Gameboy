package me.kyllian.gameboy.listeners.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.kyllian.gameboy.GameboyPlugin;
import me.kyllian.gameboy.data.Button;
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
                pocket.getButtonToggleHelper().press(Button.BUTTONLEFT, sideways > 0);
                pocket.getButtonToggleHelper().press(Button.BUTTONRIGHT, sideways < 0);
                pocket.getButtonToggleHelper().press(Button.BUTTONUP, forward > 0);
                pocket.getButtonToggleHelper().press(Button.BUTTONDOWN, forward < 0);
                pocket.getButtonToggleHelper().press(Button.BUTTONA, container.getBooleans().read(0));
                if (container.getBooleans().read(1)) {
                    pocket.stopEmulator(player);
                    player.sendMessage(gameboyPlugin.getMessageHandler().getMessage("stopped"));
                    return;
                }
            }
        });
    }
}
