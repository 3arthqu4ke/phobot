package me.earth.phobot.services;

import me.earth.phobot.util.ResetUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import net.minecraft.client.Minecraft;

public class ResetService extends SubscriberImpl {
    public ResetService(Minecraft mc) {
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> {
            // TODO: remove all references to ClientLevel and Entities so that it can get garbage collected!
        });
    }

}
