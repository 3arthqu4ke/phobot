package me.earth.phobot.services.inventory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.client.anticheat.AntiCheat;
import me.earth.phobot.util.NullabilityUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.ShutdownEvent;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Getter
public class InventoryService extends SubscriberImpl {
    private final AtomicBoolean lockedIntoEating = new AtomicBoolean();
    private final AtomicBoolean lockedIntoTotem = new AtomicBoolean();
    private final AtomicBoolean switchBack = new AtomicBoolean(true);
    private final AtomicBoolean mining = new AtomicBoolean();
    private final Lock lock = new ReentrantLock();
    private final AntiCheat antiCheat;
    private final Minecraft mc;

    private InventoryContext playerUpdateContext;

    public InventoryService(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
        this.mc = antiCheat.getPingBypass().getMinecraft();
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, Integer.MAX_VALUE) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (mc.isSameThread()) {
                    // locking the lock without a "finally" for the entire UpdateEvent is dangerous, but also the only way to lock around it.
                    // I hope the GameloopEvent and ShutDownEvents are enough to catch any exceptions happening.
                    lock.lock();
                    playerUpdateContext = new InventoryContext(InventoryService.this, player, gameMode);
                }
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc, Integer.MIN_VALUE) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                releasePlayerUpdateLock();
            }
        });

        listen(new SafeListener<GameloopEvent>(mc) {
            @Override
            public void onEvent(GameloopEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (playerUpdateContext != null) {
                    log.warn("PlayerUpdateLock has not been released until next GameloopEvent, has someone cancelled the PlayerUpdateEvent?");
                    releasePlayerUpdateLock();
                }
            }
        });

        listen(new Listener<ShutdownEvent>() {
            @Override
            public void onEvent(ShutdownEvent event) {
                if (playerUpdateContext != null) {
                    log.warn("PlayerUpdateLock has not been released until ShutDown, has someone cancelled the PlayerUpdateEvent?");
                    releasePlayerUpdateLock();
                }
            }
        });
    }

    public boolean isLockedIntoTotem() {
        return lockedIntoTotem.get();
    }

    public void use(Consumer<InventoryContext> action) {
        use(action, false);
    }

    public void use(Consumer<InventoryContext> action, boolean useNestedContext) {
        supply(ctx -> {
            action.accept(ctx);
            return null;
        }, useNestedContext, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T supply(Function<InventoryContext, T> action, boolean useNestedContext, T orElse) {
        Object[] result = new Object[1];
        NullabilityUtil.safe(mc, (player, level, gameMode) -> {
            if (playerUpdateContext != null && mc.isSameThread()) {
                if (useNestedContext) {
                    InventoryContext context = new InventoryContext(this, player, gameMode);
                    result[0] = action.apply(context);
                    context.end();
                } else {
                    result[0] = action.apply(playerUpdateContext);
                }
            } else {
                try {
                    lock.lock();
                    InventoryContext context = new InventoryContext(this, player, gameMode);
                    result[0] = action.apply(context);
                    context.end();
                } finally {
                    lock.unlock();
                }
            }
        });

        return result[0] == null ? orElse : (T) result[0];
    }

    public void releasePlayerUpdateLock() {
        if (playerUpdateContext != null && mc.isSameThread()) {
            playerUpdateContext.end();

            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.error("Failed to unlock lock unexpectedly", e);
            }

            playerUpdateContext = null;
        }
    }

}
