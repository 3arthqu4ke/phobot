package me.earth.phobot;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import me.earth.pingbypass.PingBypass;
import net.minecraft.client.Minecraft;

@RequiredArgsConstructor
public class DelegatingPingBypass implements PingBypass {
    @Delegate
    private final PingBypass pingBypass;

    public static class WithMc extends DelegatingPingBypass {
        private final Minecraft mc;

        public WithMc(PingBypass pingBypass, Minecraft mc) {
            super(pingBypass);
            this.mc = mc;
        }

        @Override
        public Minecraft getMinecraft() {
            return mc;
        }
    }

}
