package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

// TODO: Scaffolding Pathfinder?
// TODO: if we cannot reach a target because it scaffolded up, scaffold up underneath them
public class Scaffolding extends Behaviour {
    public Scaffolding(Bot bot) {
        super(bot, PRIORITY_SCAFFOLD);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        // TODO: detect if pathfinding process to nearest target failed?
        // TODO: Scaffolding A* + MovementPathfinder?
        // TODO: Parkouring A* + MovementPathfinder?
    }

}
