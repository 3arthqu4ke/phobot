package me.earth.phobot.modules.combat.autocrystal;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.damagecalc.CrystalPosition;
import me.earth.phobot.modules.client.anticheat.StrictDirection;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@Slf4j
public record CrystalPlacer(CrystalPlacingModule module, Phobot phobot) {
    public void place(LocalPlayer player, ClientLevel level, CrystalPosition pos) {
        phobot.getInventoryService().use(context -> place(context, player, level, pos));
    }

    public void place(InventoryContext context, LocalPlayer player, ClientLevel level, CrystalPosition pos) {
        if (phobot.getInventoryService().getLockedIntoTotem().get()) {
            return;
        }

        if (pos.isObsidian()) {
            List<BlockPos> path = pos.getPath();
            if (path == null) {
                return;
            }

            BlockPlacer placer = module.getBlockPlacer();
            synchronized (placer) {
                if (placer.isInTick()) { // to be safe also check if we are on the same thread? Having InventoryContext should ensure locking tho
                    boolean crystalWasNull = placer.getCrystal() == null;
                    for (int i = 1; i < path.size(); i++) {
                        BlockPos obbyPos = path.get(i);
                        if (!module.placePos(obbyPos, Blocks.OBSIDIAN, player, level)) {
                            placer.getActions().removeIf(action -> action.getModule() == module);
                            if (crystalWasNull) {
                                placer.setCrystal(null);
                            }

                            return;
                        }
                    }

                    placer.getActions().add(BlockPlacer.Action.crystalPlacingAction(pos, this));
                } else {
                    placer.startTick(player, level);
                    for (int i = 1; i < path.size(); i++) {
                        BlockPos obbyPos = path.get(i);
                        if (!module.placePos(obbyPos, Blocks.OBSIDIAN, player, level)) {
                            placer.getActions().clear();
                            placer.setCrystal(null);
                            placer.endTick(context, player, level);
                            return;
                        }
                    }

                    placer.endTick(context, player, level);
                    if (placer.isCompleted()) {
                        BlockStateLevel.Delegating delegating = new BlockStateLevel.Delegating(level);
                        delegating.getMap().put(pos, Blocks.OBSIDIAN.defaultBlockState());
                        placeCrystal(context, player, delegating, pos);
                    } else {
                        CrystalPosition copy = pos.copy();
                        copy.setObbyTries(pos.getObbyTries() + 1);
                        if (copy.getObbyTries() <= path.size()) {
                            module.obbyPos(copy);
                        }
                    }
                }
            }
        } else {
            placeCrystal(context, player, level, pos);
        }
    }

    private void placeCrystal(InventoryContext context, LocalPlayer player, ClientLevel level, CrystalPosition pos) {
        InventoryContext.SwitchResult switchResult = context.switchTo(Items.END_CRYSTAL, InventoryContext.DEFAULT_SWAP_SWITCH);
        if (switchResult == null) {
            return;
        }

        BlockPos immutable = pos.immutable();
        module.lastCrystalPos(immutable.above());
        long time = TimeUtil.getMillis();
        module.map().put(immutable, time);
        BlockHitResult hitResult;
        if (phobot.getAntiCheat().getCrystalStrictDirection().getValue() != StrictDirection.Type.Vanilla) {
            Direction dir = phobot.getAntiCheat().getCrystalStrictDirectionCheck().getStrictDirection(immutable, player, level);
            if (dir == null) {
                return;
            }

            Vec3 vec = new Vec3(pos.getX() + 0.5 - dir.getStepX() * 0.5 + dir.getStepX(),
                                pos.getY() + 0.5 - dir.getStepY() * 0.5 + dir.getStepY(),
                                pos.getZ() + 0.5 - dir.getStepZ() * 0.5 + dir.getStepZ());
            hitResult = new BlockHitResult(vec, dir, immutable, false);
        } else {
            hitResult = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), Direction.DOWN, immutable, false);
        }

        ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(switchResult.hand(), hitResult, 0);
        player.connection.send(packet);
        player.connection.send(new ServerboundSwingPacket(switchResult.hand()));
        module.setRenderPos(immutable);
        module.placeTimer().reset();
    }

}
