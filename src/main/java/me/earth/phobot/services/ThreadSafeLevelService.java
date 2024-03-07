package me.earth.phobot.services;

import me.earth.phobot.util.world.EntityCopyingLevel;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

public class ThreadSafeLevelService extends SubscriberImpl {
    private final Minecraft mc;
    private volatile @Nullable ClientLevel level;

    public ThreadSafeLevelService(Minecraft mc) {
        this.mc = mc;
        listen(new Listener<TickEvent>() {
            @Override
            public void onEvent(TickEvent event) {
                ClientLevel clientLevel = mc.level;
                if (clientLevel == null) {
                    level = null;
                } else {
                    level = new EntityCopyingLevel(clientLevel);
                }
            }
        });
    }

    public @Nullable ClientLevel getLevel() {
        ClientLevel result = level;
        if (result == null) {
            result = mc.level;
            if (result != null && mc.isSameThread()) {
                level = new EntityCopyingLevel(result);
                result = level;
            }
        }

        return result;
    }

}
