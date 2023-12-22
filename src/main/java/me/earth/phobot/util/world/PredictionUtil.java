package me.earth.phobot.util.world;

import lombok.experimental.UtilityClass;
import me.earth.phobot.mixins.level.IClientLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;

import java.util.function.Consumer;

@UtilityClass
public class PredictionUtil {
    public static void predict(ClientLevel level, Consumer<Integer> action) {
        try (BlockStatePredictionHandler predictionHandler = ((IClientLevel) level).getBlockStatePredictionHandler().startPredicting()) {
            int seq = predictionHandler.currentSequence();
            action.accept(seq);
        }
    }

}
