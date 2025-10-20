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
                try {
                    Object handle = event.getPacket().getHandle();

                    Object input = handle.getClass().getMethod("input").invoke(handle);

                    boolean forward  = (boolean) input.getClass().getMethod("forward").invoke(input);
                    boolean backward = (boolean) input.getClass().getMethod("backward").invoke(input);
                    boolean left     = (boolean) input.getClass().getMethod("left").invoke(input);
                    boolean right    = (boolean) input.getClass().getMethod("right").invoke(input);
                    boolean jump     = (boolean) input.getClass().getMethod("jump").invoke(input);
                    boolean shift    = (boolean) input.getClass().getMethod("shift").invoke(input);
                    boolean sprint   = (boolean) input.getClass().getMethod("sprint").invoke(input);

                    pocket.getButtonToggleHelper().press(Button.BUTTONLEFT, left);
                    pocket.getButtonToggleHelper().press(Button.BUTTONRIGHT, right);
                    pocket.getButtonToggleHelper().press(Button.BUTTONUP, forward);
                    pocket.getButtonToggleHelper().press(Button.BUTTONDOWN, backward);
                    pocket.getButtonToggleHelper().press(Button.BUTTONA, jump);

                    if (shift) {
                        pocket.stopEmulator(player);
                        player.sendMessage(gameboyPlugin.getMessageHandler().getMessage("stopped"));
                        return;
                    }
                } catch (ReflectiveOperationException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
