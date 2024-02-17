package me.earth.phobot.modules.combat.autocrystal;

import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.damagecalc.CrystalPosition;
import me.earth.phobot.modules.client.anticheat.StrictDirection;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.math.RaytraceUtil;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Getter
public class CrystalPlacingAction extends BlockPlacer.Action {
    private final CrystalPlacingModule module;
    private final CrystalPosition crystalPosition;
    private final CrystalPlacer crystalPlacer;
    private final Phobot phobot;
    private boolean failedDueToRotations;
    private boolean successful;
    private boolean executed;

    public CrystalPlacingAction(CrystalPosition crystalPosition, CrystalPlacer crystalPlacer, boolean packetRotations) {
        this(crystalPosition.immutable(), crystalPosition.copy(), crystalPlacer, packetRotations);
    }

    private CrystalPlacingAction(BlockPos immutable, CrystalPosition copy, CrystalPlacer crystalPlacer, boolean packetRotations) {
        this(crystalPlacer.getModule(), immutable, immutable, Direction.UP, Items.END_CRYSTAL, false, false,
                packetRotations, crystalPlacer.getModule().getBlockPlacer(), false, copy, crystalPlacer);
    }

    protected CrystalPlacingAction(Object module, BlockPos placeOn, BlockPos pos, Direction direction, Item item, boolean shouldSetBlock, boolean usingSetCarried,
                                boolean packetRotations, BlockPlacer blockPlacer, boolean requiresExactDirection, CrystalPosition crystalPosition, CrystalPlacer crystalPlacer) {
        super(module, placeOn, pos, direction, item, shouldSetBlock, usingSetCarried, packetRotations, blockPlacer, requiresExactDirection);
        this.crystalPosition = crystalPosition;
        this.crystalPlacer = crystalPlacer;
        this.module = crystalPlacer.getModule();
        this.phobot = crystalPlacer.getPhobot();
    }

    @Override
    public boolean isCrystalAction() {
        return true;
    }

    @Override
    public boolean execute(Set<BlockPlacer.Action> completedActions, InventoryContext context, LocalPlayer player, ClientLevel clientLevel) {
        executed = true;
        failedDueToRotations = false;
        successful = false;
        BlockPos pos = getPos();
        HitResult hitResult = getHitResult(pos, player, clientLevel);
        if (hitResult == null) {
            return false;
        }

        boolean packetRotate = false;
        if (hitResult.rotations != null) {
            if (isPacketRotations() && !getBlockPlacer().getMotionUpdateService().isInPreUpdate()) {
                packetRotate = true;
            } else {
                if (phobot.getMinecraft().isSameThread()
                        && !getBlockPlacer().getMotionUpdateService().isSpoofing()
                        && getBlockPlacer().getMotionUpdateService().isInPreUpdate()) {
                    getBlockPlacer().getMotionUpdateService().rotate(player, hitResult.rotations[0], hitResult.rotations[1]);
                } else {
                    failedDueToRotations = true;
                }

                return false;
            }
        }

        InventoryContext.SwitchResult switchResult = context.switchTo(Items.END_CRYSTAL, InventoryContext.DEFAULT_SWAP_SWITCH);
        if (switchResult == null) {
            return false;
        }

        if (packetRotate) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(hitResult.rotations[0], hitResult.rotations[1], player.onGround()));
        }

        handleModule(pos);
        // TODO: hitresult is offf
        ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(switchResult.hand(), hitResult.result, 0);
        player.connection.send(packet);
        player.connection.send(new ServerboundSwingPacket(switchResult.hand()));
        successful = true;
        return true;
    }

    private void handleModule(BlockPos pos) {
        module.lastCrystalPos(pos.above());
        long time = TimeUtil.getMillis();
        module.map().put(pos, time);
        module.setRenderPos(pos);
        module.placeTimer().reset();
    }

    // TODO: build height!!!
    private @Nullable HitResult getHitResult(BlockPos immutable, LocalPlayer player, ClientLevel level) {
        BlockStateLevel.Delegating stateLevel = getStateLevel(level);

        float[] rotations = null;
        BlockHitResult hitResult;
        Player rotationPlayer = module.getBlockPlacer().getLocalPlayerPositionService().getPlayerOnLastPosition(player);
        if (phobot.getAntiCheat().getCrystalRotations().getValue()) { // We have to rotate
            boolean invert = module.getBlockPlacer().getAntiCheat().getOpposite().getValue();
            // first check if we can hit the block with the rotations we currently have on the server
            hitResult = RaytraceUtil.raytraceChecked(rotationPlayer, stateLevel, immutable,
                    (hr) -> module.getBlockPlacer()
                            .getAntiCheat()
                            .getStrictDirectionCheck()
                            .strictDirectionCheck(hr.getBlockPos(), hr.getDirection(), stateLevel, rotationPlayer), invert);
            if (hitResult == null) { // current rotations are not legit, we have to rotate
                Direction dir = Direction.UP;
                if (phobot.getAntiCheat().getCrystalStrictDirection().getValue() != StrictDirection.Type.Vanilla) {
                    // first check if direction.up is ok, because we really like up, because if we rotate for up, we also are rotated on the crystal
                    if (!phobot.getAntiCheat().getCrystalStrictDirectionCheck().strictDirectionCheck(immutable, dir, stateLevel, player)) {
                        dir = phobot.getAntiCheat().getCrystalStrictDirectionCheck().getStrictDirection(immutable, player, stateLevel);
                        if (dir == null) {
                            return null;
                        }
                    }
                }

                Vec3 vec = getOffset(immutable, dir);
                rotations = RotationUtil.getRotations(player, vec.x, vec.y, vec.z);
                hitResult = new BlockHitResult(vec, dir, immutable, false);
            }
        } else if (phobot.getAntiCheat().getCrystalStrictDirection().getValue() != StrictDirection.Type.Vanilla) { // We have to target a direction we can see
            Direction dir = phobot.getAntiCheat().getCrystalStrictDirectionCheck().getStrictDirection(immutable, rotationPlayer, stateLevel);
            if (dir == null) {
                return null;
            }

            Vec3 vec = getOffset(immutable, dir);
            hitResult = new BlockHitResult(vec, dir, immutable, false);
        } else { // do whatever
            hitResult = new BlockHitResult(new Vec3(immutable.getX() + 0.5, immutable.getY(), immutable.getZ() + 0.5), Direction.DOWN, immutable, false);
        }

        if (hitResult != null && !hitResult.getBlockPos().equals(immutable)) {
            throw new IllegalStateException();
        }

        return new HitResult(hitResult, rotations);
    }

    private Vec3 getOffset(BlockPos pos, Direction direction) {
        return new Vec3(
                pos.getX() + 0.5 - direction.getStepX() * 0.5 + direction.getStepX(),
                pos.getY() + 0.5 - direction.getStepY() * 0.5 + direction.getStepY(),
                pos.getZ() + 0.5 - direction.getStepZ() * 0.5 + direction.getStepZ()
        );
    }

    private BlockStateLevel.Delegating getStateLevel(ClientLevel level) {
        var result = module.getBlockPlacer().getCustomBlockStateLevel();
        return result == null ? new BlockStateLevel.Delegating(level) : result;
    }

    private record HitResult(BlockHitResult result, float @Nullable [] rotations) {
    }

}
