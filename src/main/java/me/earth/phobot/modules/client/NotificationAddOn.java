package me.earth.phobot.modules.client;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.DeathEvent;
import me.earth.phobot.event.TotemPopEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.client.Notifications;
import me.earth.pingbypass.api.plugin.impl.ModuleAddOn;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class NotificationAddOn extends ModuleAddOn<Notifications> {
    private final Setting<Boolean> totems = bool("Totems", true, "Announces Totem Pops.");

    public NotificationAddOn(Phobot phobot) {
        super(phobot.getPingBypass(), Notifications.class);
        listen(new Listener<TotemPopEvent>() {
            @Override
            public void onEvent(TotemPopEvent event) {
                if (totems.getValue() && event.entity() instanceof Player && !Objects.equals(event.entity(), mc.player)) {
                    int pops = phobot.getTotemPopService().getPops(event.entity());
                    getPingBypass().getChat().send(Component.literal("")
                            .append(event.entity().getName().copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .append(Component.literal(" popped ").withStyle(ChatFormatting.RED))
                            .append(Component.literal(String.valueOf(pops)).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" totems.").withStyle(ChatFormatting.RED)), "TotemPops" + event.entity().getId());
                }
            }
        });

        listen(new Listener<DeathEvent>(10) {
            @Override
            public void onEvent(DeathEvent event) {
                if (totems.getValue() && event.entity() instanceof Player && !Objects.equals(event.entity(), mc.player)) {
                    int pops = phobot.getTotemPopService().getPops(event.entity());
                    getPingBypass().getChat().send(Component.literal("")
                            .append(event.entity().getName().copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .append(Component.literal(" died after popping ").withStyle(ChatFormatting.RED))
                            .append(Component.literal(String.valueOf(pops)).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" totems.").withStyle(ChatFormatting.RED)), "TotemPops" + event.entity().getId());
                }
            }
        });
    }

}
