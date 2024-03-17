package me.earth.phobot.modules.combat;

import me.earth.phobot.Phobot;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Set;

/**
 * Like {@link Surround} but for your face.
 */
public class AntiFacePlace extends SurroundModuleBase {
    private final Setting<Boolean> auto = bool("Auto", false, "Only places when you are in danger.");

    public AntiFacePlace(Phobot phobot) {
        super(phobot, phobot.getBlockPlacer(), "AntiFacePlace", Categories.COMBAT, "Places blocks that protect you from getting faceplaced.", BlockPlacer.PRIORITY - 2);
    }

    @Override
    protected void updatePlacements(InventoryContext context, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (!auto.getValue() || EntityUtil.getHealth(player) <= 9.0f) {
            super.updatePlacements(context, player, level, gameMode);
        }
    }

    @Override
    protected void addSurroundPos(BlockPos pos, Set<BlockPos> positions, Set<BlockPos> allSurrounding, Set<BlockPos> allChecked, Set<Entity> checkedEntities, Player placePlayer,
                                  ClientLevel level, Block block, BlockPos surroundPos, boolean addBelow, BlockPos supportingPos) {
        super.addSurroundPos(pos, positions, allSurrounding, allChecked, checkedEntities, placePlayer, level, block, surroundPos, addBelow, supportingPos);
        BlockPos antiFacePlacePos = surroundPos.above();
        if (level.getBlockState(antiFacePlacePos).canBeReplaced()) {
            if (!extendAroundSurroundPos(pos, antiFacePlacePos, positions, allSurrounding, allChecked, checkedEntities, placePlayer, level, block)) {
                positions.add(antiFacePlacePos);
            }

            allSurrounding.add(antiFacePlacePos);
        }
    }

    @Override
    protected void addToPositions(Set<BlockPos> positions, BlockPos pos) {
        // we only allow the positions.add(antiFacePlacePos); in addSurroundPos
    }

    @Override
    protected boolean addSupportPosWithBedrockCheck(BlockPos supportPos, Level level, Set<BlockPos> allSurrounding, boolean addBelow) {
        BlockPos facePlacePos = supportPos.above(2);
        if (!level.getBlockState(facePlacePos).canBeReplaced()) {
            super.addSupportPosWithBedrockCheck(supportPos, level, allSurrounding, addBelow);
            allSurrounding.add(facePlacePos);
            return true;
        }

        return false;
    }

}
