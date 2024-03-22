package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

// TODO: if no players close, stand in loot
public class Looting extends Behaviour {
    public Looting(Bot bot, int priority) {
        super(bot, priority);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {

    }

}
