package me.earth.phobot.util;

import lombok.experimental.UtilityClass;
import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.pingbypass.api.event.api.Subscriber;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;

@UtilityClass
public class ResetUtil {
    public static void disableOnRespawnAndWorldChange(Module module, Minecraft mc) {
        onRespawnOrWorldChange(module, mc, module::disable);
    }

    public static void onRespawnOrWorldChange(Subscriber module, Minecraft mc, Runnable runnable) {
        module.getListeners().add(new Listener<ChangeWorldEvent>() {
            @Override
            public void onEvent(ChangeWorldEvent event) {
                runnable.run();
            }
        });

        module.getListeners().add(new Listener<PacketEvent.Receive<ClientboundRespawnPacket>>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundRespawnPacket> event) {
                mc.submit(runnable);
            }
        });
    }

}
