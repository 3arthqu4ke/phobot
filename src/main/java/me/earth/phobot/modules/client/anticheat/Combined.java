package me.earth.phobot.modules.client.anticheat;

import me.earth.phobot.util.math.PositionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class Combined implements StrictDirection {
    private final StrictDirection[] checks;

    public Combined(StrictDirection... checks) {
        this.checks = checks;
    }

    @Override
    public @Nullable Direction getStrictDirection(BlockPos pos, Player player, ClientLevel level) {
        for (Direction direction : PositionUtil.DIRECTIONS) {
            if (strictDirectionCheck(pos, direction, level, player)) {
                return direction;
            }
        }

        return null;
    }

    @Override
    public boolean strictDirectionCheck(BlockPos pos, Direction direction, ClientLevel level, Player player) {
        for (StrictDirection check : checks) {
            if (!check.strictDirectionCheck(pos, direction, level, player)) {
                return false;
            }
        }

        return true;
    }

}
