package me.earth.phobot.modules.combat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;

import static me.earth.phobot.util.math.MathUtil.distance2dSq;

@Slf4j
@Getter
public class Surround extends SurroundModuleBase {
    private final Setting<Boolean> center = bool("Center", false, "Centers you in the middle of the block you are standing on if necessary.");
    private boolean centered;

    public Surround(Phobot phobot) {
        super(phobot, phobot.getBlockPlacer(), "Surround", Categories.COMBAT, "Surrounds you with obsidian.", BlockPlacer.PRIORITY - 1);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        centered = false;
    }

    @Override
    protected void onMotionPlayerUpdate(LocalPlayer player, ClientLevel level) {
        if (isEnabled() && !centered && center.getValue()) {
            center(player, level);
            centered = true;
        }

        super.onMotionPlayerUpdate(player, level);
    }

    // TODO: also center smartly to place the minimum amount of blocks?
    private void center(LocalPlayer player, ClientLevel level) {
        Set<BlockPos> positions = PositionUtil.getPositionsUnderEntity(player, 0.5);
        if (positions.size() != 1) {
            MovementPlayer firstMove = centerOnce(player, level, positions);
            positions = PositionUtil.getPositionsUnderEntity(player, 0.5);
            if (positions.size() != 1) {
                // center twice, if we are in the middle of a 2x2 its possible that moving into the direction of the center once is not enough
                centerOnce(player, level, positions);
                // If we center twice we need to send a position packet, we can use that packet to do our first rotation
                if (phobot.getAntiCheat().getBlockRotations().getValue()) {
                    BlockPlacer.Action[] firstAction = new BlockPlacer.Action[1];
                    Block block = phobot.getInventoryService().supply(ctx -> ctx.findBlock(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ENDER_CHEST), true, Blocks.OBSIDIAN);
                    PlaceFunction placeFunction = (pos, b, p, l) -> {
                        if (firstAction[0] == null) {
                            firstAction[0] = createPlaceAction(pos, b, p, l, new CenteringBlockPlacingValidityCheck());
                        }

                        return firstAction[0] != null;
                    };
                    // call our placeFunction to find out which will be the first action
                    getAllSurroundingPositions(player, firstMove, level, block, true, placeFunction, true);
                    if (firstAction[0] != null) {
                        float[] rotations = RotationUtil.getRotations(firstMove, level, firstAction[0].getPlaceOn(), firstAction[0].getDirection());
                        // TODO: verify that we have actually send a new yaw and pitch to the server?
                        player.connection.send(new ServerboundMovePlayerPacket.PosRot(firstMove.getX(), firstMove.getY(), firstMove.getZ(), rotations[0], rotations[1], firstMove.onGround()));
                    } else {
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(firstMove.getX(), firstMove.getY(), firstMove.getZ(), firstMove.onGround()));
                    }
                } else {
                    player.connection.send(new ServerboundMovePlayerPacket.Pos(firstMove.getX(), firstMove.getY(), firstMove.getZ(), firstMove.onGround()));
                }
            }
        }
    }

    private MovementPlayer centerOnce(LocalPlayer player, ClientLevel level, Collection<BlockPos> positions) {
        BlockPos best = positions.stream()
                .min(Comparator.comparingDouble(pos -> distance2dSq(pos.getX() + 0.5, pos.getZ() + 0.5, player.getX(), player.getZ())))
                .orElseThrow();
        Vec3 delta =
                new Vec3(best.getX() + 0.5 - player.getX(), 0.0, best.getZ() + 0.5 - player.getZ())
                        .normalize()
                        .scale(phobot.getMovementService().getMovement().getSpeed(player))
                        .add(0.0, player.getDeltaMovement().y, 0.0);
        // TODO: not perfect yet, what if we have already moved this tick?
        MovementPlayer movementPlayer = new MovementPlayer(level);
        movementPlayer.copyPosition(player);
        movementPlayer.setDeltaMovement(delta);
        movementPlayer.move(MoverType.SELF, delta);
        player.setPos(movementPlayer.position());
        player.setOnGround(movementPlayer.onGround());
        startPos = player.blockPosition();
        startY = player.getY();
        return movementPlayer;
    }

    private class CenteringBlockPlacingValidityCheck implements ChecksBlockPlacingValidity {
        @Override
        public boolean isBlockedByEntity(BlockPos pos, VoxelShape shapeToPlace, Player player, ClientLevel level, Predicate<Entity> consumesBlockingEntities) {
            return isBlockedByEntity(pos, shapeToPlace, player, level, consumesBlockingEntities, (blockPlacer, endCrystal) -> {/* do not set crystal*/});
        }

        @Override
        public BlockPlacer getBlockPlacer() {
            return Surround.this.getBlockPlacer();
        }
    }

}