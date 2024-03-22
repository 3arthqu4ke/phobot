package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.PostInputTickEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Random;

/**
 * Starts crouching when lagged.
 */
public class AntiRubberBand extends PhobotModule {
    private final Setting<Integer> time = number("Time", 500, 0, 5_000, "Time in milliseconds to crouch for after lagging.");
    private final Setting<Integer> moveTime = number("Move-Time", 150, 0, 5_000, "Time in milliseconds to move for after lagging.");
    private final Setting<Integer> startTime = number("Start-Time", 100, 0, 5_000, "Time in milliseconds to start making anti rubber band movements after lagging.");
    private final Setting<Boolean> randomMovement = bool("Random", false, "When lagging twice while Pathfinding, makes a random sneaking movement to attempt to un-rubber-band us.");
    private final Setting<Boolean> alwaysRandom = bool("Always-Random", false, "Don't apply Random when we have lagged twice, but every single time.");
    private final Setting<Boolean> randomSneaking = bool("RandomSneaking", false, "Randomly decides whether to sneak or not.");
    private final Setting<Boolean> modifier = bool("Modifier", false, "Applies the sneaking slow down modifier if necessary.");
    // TODO: do something with Sprint?
    private final Random random = new Random();

    public AntiRubberBand(Phobot phobot) {
        super(phobot, "AntiRubberBand", Categories.MOVEMENT, "Starts crouching when you get rubber-banded.");
        listen(new SafeListener<PostInputTickEvent>(mc) {
            @Override
            public void onEvent(PostInputTickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.input instanceof KeyboardInput/* could be some other input like baritones?*/) {
                    boolean wasShiftKeyDown = player.input.shiftKeyDown;
                    if (!phobot.getLagbackService().passed(time.getValue())) {
                        player.input.shiftKeyDown = !randomSneaking.getValue() || random.nextBoolean();
                    }

                    if (phobot.getPathfinder().isFollowingPath()
                            && randomMovement.getValue()
                            && player.input.leftImpulse == 0.0f
                            && player.input.forwardImpulse == 0.0f
                            && !phobot.getLagbackService().passed(startTime.getValue() + moveTime.getValue())
                            && (alwaysRandom.getValue() || phobot.getLagbackService().getTimeSinceLastLag() < 500L) // we have lagged multiple times in a short time
                            && phobot.getLagbackService().passed(startTime.getValue())) {
                        while (player.input.leftImpulse == 0.0f && player.input.forwardImpulse == 0.0f) { // until we hit a move in some direction
                            player.input.leftImpulse = random.nextInt(3) - 1; // some random value of -1, 0, or 1
                            player.input.forwardImpulse = random.nextInt(3) - 1; // some random value of -1, 0, or 1
                        }

                        applySneakModifier(player);
                    } else if (!wasShiftKeyDown && modifier.getValue()) {
                        applySneakModifier(player);
                    }
                }
            }
        });
    }

    private void applySneakModifier(LocalPlayer player) {
        if (player.input.shiftKeyDown) {
            float sneakSpeedModifier = Mth.clamp(0.3f + EnchantmentHelper.getSneakingSpeedBonus(player), 0.0f, 1.0f);
            player.input.leftImpulse *= sneakSpeedModifier;
            player.input.forwardImpulse *= sneakSpeedModifier;
        }
    }

}
