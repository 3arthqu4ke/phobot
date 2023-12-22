package me.earth.phobot.ducks;

import me.earth.phobot.mixins.entity.ILivingEntity;
import me.earth.phobot.util.player.PredictionPlayer;

public interface IAbstractClientPlayer extends ILivingEntity {
    PredictionPlayer[] phobot$getPredictions();

}
