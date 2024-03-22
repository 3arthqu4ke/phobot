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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Contains the logic for placing a crystal.
 * This is similar to a {@link BlockPlacer.Action},
 * but for placing a crystal we can hit any part of the block so we can be a bit more lenient than when placing blocks.
 */
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
        this(crystalPlacer.getModule(), immutable, immutable/*TODO: should be above?! Where the crystal will actually spawn*/, Direction.UP, Items.END_CRYSTAL, false, false,
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
        if (hitResult.rotations != null) { // We need to rotate
            if (isPacketRotations() && !getBlockPlacer().getMotionUpdateService().isInPreUpdate()) {
                packetRotate = true; // We can just send a packet for rotations
            } else {
                if (phobot.getMinecraft().isSameThread()
                        && !getBlockPlacer().getMotionUpdateService().isSpoofing()
                        && getBlockPlacer().getMotionUpdateService().isInPreUpdate()) {
                    // We are on the mainthread an in PreMotionPlayerUpdate, we can spoof our rotations
                    // Then this.execute will be called again, and we should be rotated correctly
                    getBlockPlacer().getMotionUpdateService().rotate(player, hitResult.rotations[0], hitResult.rotations[1]);
                } else {
                    failedDueToRotations = true;
                }

                return false;
            }
        }
        // switch to end crystals

        int flags = InventoryContext.DEFAULT_SWAP_SWITCH;
        // check if we are holding food in our offhand, in that case switch into main hand
        if (player.getItemInHand(InteractionHand.OFF_HAND).getItem().isEdible()
                && !player.getItemInHand(InteractionHand.MAIN_HAND).getItem().isEdible()) {
            flags = InventoryContext.PREFER_MAINHAND;
        }

        InventoryContext.SwitchResult switchResult = context.switchTo(Items.END_CRYSTAL, flags);
        if (switchResult == null) {
            return false;
        }

        if (packetRotate) { // send rotation packet
            float[] rotations = RotationUtil.getRotations(phobot.getLocalPlayerPositionService().getPlayerOnLastPosition(player), hitResult.result.getLocation());
            player.connection.send(new ServerboundMovePlayerPacket.Rot(rotations[0], rotations[1], player.onGround()));
        }
        // Update our CrystalPlacingModule
        handleModule(clientLevel, pos);
        // Send placement packet
        ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(switchResult.hand(), hitResult.result, 0);
        player.connection.send(packet);
        player.connection.send(new ServerboundSwingPacket(switchResult.hand()));
        successful = true;
        return true;
    }

    private void handleModule(ClientLevel level, BlockPos pos) {
        module.lastCrystalPos(pos.above());
        long time = TimeUtil.getMillis();
        module.map().put(pos, time);
        module.setRenderPos(pos);
        module.placeTimer().reset();
        Integer targetId = crystalPosition.getTargetId();
        if (targetId != null) {
            Entity entity = level.getEntity(targetId);
            module.target(entity);
        }
    }

    // TODO: build height!!!
    private @Nullable HitResult getHitResult(BlockPos immutable, LocalPlayer player, ClientLevel level) {
        // Get prepared world where potentially prepared obsidian blocks are already placed
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
                        if (dir == null) { // We cannot see any faces of the block
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
            if (dir == null) { // We cannot see any faces of the block
                return null;
            }

            Vec3 vec = getOffset(immutable, dir);
            hitResult = new BlockHitResult(vec, dir, immutable, false);
        } else { // do whatever
            // TODO: meh, not very legit, should actually be UP, but build height, bla bla
            hitResult = new BlockHitResult(new Vec3(immutable.getX() + 0.5, immutable.getY(), immutable.getZ() + 0.5), Direction.DOWN, immutable, false);
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
