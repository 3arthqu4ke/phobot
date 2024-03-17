package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

public class AlwaysOnModules extends Behaviour {
    public AlwaysOnModules(Bot bot) {
        super(bot, PRIORITY_FIRST);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        bot.getModules().getAutoEat().enable();
        // TODO: if we always have AC on we need smart detection for getting stuck!!!!
        bot.getModules().getAutoCrystal().enable();
    }

}
