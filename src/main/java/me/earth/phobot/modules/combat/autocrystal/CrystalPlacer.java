package me.earth.phobot.modules.combat.autocrystal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.damagecalc.CrystalPosition;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

// TODO: tests should be very much possible for this!
// TODO: revisiting this code to add crystal rotations I realize how shitty to maintain this is!!!
//  even though the BlockPlacer was supposed to help with redundant code this is awful.
//  but crystal rotations are hopefully the last thing to add.
@Slf4j
@Getter
@RequiredArgsConstructor
@SuppressWarnings("ClassCanBeRecord")
public class CrystalPlacer {
    private final CrystalPlacingModule module;
    private final Phobot phobot;

    public boolean place(LocalPlayer player, ClientLevel level, CrystalPosition pos) {
        CrystalPlacingAction action = placeAction(player, level, pos);
        return action != null && action.isSuccessful();
    }

    // TODO: use in Bomber!
    public @Nullable CrystalPlacingAction placeAction(LocalPlayer player, ClientLevel level, CrystalPosition pos) {
        CrystalPlacingAction[] result = new CrystalPlacingAction[1];
        phobot.getInventoryService().use(context -> result[0] = placeAction(context, player, level, pos, false));
        return result[0];
    }

    // TODO: use the packet rotations for the obsidian too!!!
    public @Nullable CrystalPlacingAction placeAction(InventoryContext context, LocalPlayer player, ClientLevel level, CrystalPosition pos, boolean packetRotations) {
        if (phobot.getInventoryService().getLockedIntoTotem().get()) {
            return null;
        }

        if (pos.isObsidian()) {
            List<BlockPos> path = pos.getPath();
            if (path == null) {
                return null;
            }

            BlockPlacer placer = module.getBlockPlacer();
            synchronized (placer) {
                if (placer.isInTick()) { // to be safe also check if we are on the same thread? Having InventoryContext should ensure locking tho
                    BlockPlacer.Action currentAction = null;
                    boolean crystalWasNull = placer.getCrystal() == null;
                    for (int i = 1; i < path.size(); i++) {
                        BlockPos obbyPos = path.get(i);
                        BlockPlacer.Action placeAction = module.placeAction(obbyPos, Blocks.OBSIDIAN, player, level);
                        if (placeAction == null) {
                            placer.getActions().removeIf(action -> action.getModule() == module);
                            if (crystalWasNull) {
                                placer.setCrystal(null);
                            }

                            return null;
                        } else {
                            currentAction = updateDependencies(placeAction, currentAction);
                        }
                    }

                    CrystalPlacingAction crystalPlacingAction = action(pos, packetRotations);
                    updateDependencies(crystalPlacingAction, currentAction);
                    placer.getActions().add(crystalPlacingAction);
                    return crystalPlacingAction;
                } else {
                    placer.startTick(player, level);
                    for (int i = 1; i < path.size(); i++) {
                        BlockPos obbyPos = path.get(i);
                        if (!module.placePos(obbyPos, Blocks.OBSIDIAN, player, level)) {
                            placer.getActions().clear();
                            placer.setCrystal(null);
                            placer.endTick(context, player, level);
                            return null;
                        }
                    }

                    placer.endTick(context, player, level);
                    if (placer.isCompleted()) {
                        BlockStateLevel.Delegating delegating = new BlockStateLevel.Delegating(level);
                        delegating.getMap().put(pos, Blocks.OBSIDIAN.defaultBlockState());
                        CrystalPlacingAction crystalPlacingAction = action(pos, packetRotations);
                        crystalPlacingAction.execute(Collections.emptySet(), context, player, delegating);
                        return crystalPlacingAction;
                    } else {
                        CrystalPosition copy = pos.copy();
                        copy.setObbyTries(pos.getObbyTries() + 1);
                        if (copy.getObbyTries() <= path.size()) {
                            module.obbyPos(copy);
                        }

                        return null;
                    }
                }
            }
        }

        CrystalPlacingAction crystalPlacingAction = action(pos, packetRotations);
        crystalPlacingAction.execute(Collections.emptySet(), context, player, level);
        return crystalPlacingAction;
    }

    private CrystalPlacingAction action(CrystalPosition position, boolean packetRotations) {
        return new CrystalPlacingAction(position, this, packetRotations);
    }

    private BlockPlacer.Action updateDependencies(BlockPlacer.Action action, @Nullable BlockPlacer.Action currentAction) {
        if (currentAction != null) {
            action.getDependencies().addAll(currentAction.getDependencies());
            action.getDependencies().add(currentAction);
        }

        return action;
    }

}
