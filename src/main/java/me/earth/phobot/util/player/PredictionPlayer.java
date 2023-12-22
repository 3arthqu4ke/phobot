package me.earth.phobot.util.player;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.player.AbstractClientPlayer;

@Getter
@Setter
public class PredictionPlayer extends DamageCalculatorPlayer {
    private long lastPrediction;

    public PredictionPlayer(AbstractClientPlayer player) {
        super(player);
    }

    @Override
    public float maxUpStep() {
        return 0.6f;
    }

}
