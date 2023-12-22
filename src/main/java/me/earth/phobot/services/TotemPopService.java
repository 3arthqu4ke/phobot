package me.earth.phobot.services;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.ducks.ITotemPoppingEntity;
import me.earth.phobot.event.DeathEvent;
import me.earth.phobot.event.LocalPlayerDeathEvent;
import me.earth.phobot.event.TotemPopEvent;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

// TODO: clear if player logs out
// TODO: listen for deathmessages?
@Slf4j
public class TotemPopService extends SubscriberImpl {
    private final StopWatch.ForMultipleThreads lastTotemPop = new StopWatch.ForMultipleThreads();
    private final Map<Integer, Integer> pops = new HashMap<>();

    public TotemPopService(Minecraft mc) {
        listen(new Listener<TotemPopEvent>(1000) {
            @Override
            public void onEvent(TotemPopEvent event) {
                if (event.entity().equals(mc.player)) {
                    log.info("Pop, last pop " + lastTotemPop.getPassedTime() + "ms ago.");
                    lastTotemPop.reset();
                }

                if (event.entity() instanceof ITotemPoppingEntity totemPoppingEntity) {
                    totemPoppingEntity.phobot$getLastTotemPop().reset();
                }

                Integer amountOfPops = pops.get(event.entity().getId());
                pops.put(event.entity().getId(), amountOfPops == null ? 1 : amountOfPops + 1);
            }
        });

        listen(new Listener<LocalPlayerDeathEvent>() {
            @Override
            public void onEvent(LocalPlayerDeathEvent event) {
                long time = lastTotemPop.getPassedTime();
                if (time < 450) {
                    log.warn("Death, due to potential anti totem: " + time + "ms since last pop.");
                } else {
                    log.info("Death, last pop " + time + "ms ago.");
                }
            }
        });

        listen(new Listener<DeathEvent>() {
            @Override
            public void onEvent(DeathEvent event) {
                pops.remove(event.entity().getId());
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, pops::clear);
    }

    public int getPops(Entity entity) {
        return pops.getOrDefault(entity.getId(), 0);
    }

}
