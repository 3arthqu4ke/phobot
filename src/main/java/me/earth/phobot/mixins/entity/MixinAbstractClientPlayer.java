package me.earth.phobot.mixins.entity;

import me.earth.phobot.ducks.IAbstractClientPlayer;
import me.earth.phobot.util.player.PredictionPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends MixinLivingEntity implements IAbstractClientPlayer {
    @Unique
    private final PredictionPlayer[] phobot$predictionPlayers = new PredictionPlayer[5];

    @Override
    public PredictionPlayer[] phobot$getPredictions() {
        return phobot$predictionPlayers;
    }

}
