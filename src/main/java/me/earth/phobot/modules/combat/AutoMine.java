package me.earth.phobot.modules.combat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.damagecalc.CrystalPosition;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.misc.Speedmine;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.world.BlockStateLevel;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// TODO: trying to bomb when block above someones head with bedrock on top
// TODO: on CrystalPvP.cc we can also do the 1.13 entity crystal place just one block below them
@Slf4j
@Getter
public class AutoMine extends PhobotModule {
    private final Bomber.PositionsThatBlowUpDrops positionsThatBlowUpDrops = new Bomber.PositionsThatBlowUpDrops();
    private final Speedmine speedmine;
    private Position currentPosition = null;
    private boolean currentPositionValid = true;

    public AutoMine(Phobot phobot, Speedmine speedmine, Bomber bomber, Surround surround) {
        super(phobot, "AutoMine", Categories.COMBAT, "Automatically mines out enemies.");
        this.speedmine = speedmine;
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> currentPosition = null);
        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel clientLevel, MultiPlayerGameMode gameMode) {
                if (!speedmine.isEnabled()) {
                    getPingBypass().getChat().sendWithoutLogging(Component.literal("Speedmine needs to be enabled for AutoMine!").withStyle(ChatFormatting.RED), "SpeedmineCheck");
                    return;
                }

                if (speedmine.getCurrentPos() != null
                        && !clientLevel.getBlockState(speedmine.getCurrentPos()).isAir()
                        && (currentPosition == null || !Objects.equals(speedmine.getCurrentPos(), currentPosition.pos))) {
                    return;
                }

                BlockStateLevel.Delegating level = new BlockStateLevel.Delegating(clientLevel);
                Set<BlockPos> surrounding = surround.getAllSurroundingPositions(player, player, clientLevel, Blocks.OBSIDIAN, false);
                surrounding.removeIf(pos -> pos.getY() < player.getY() - 0.5);
                if (currentPosition != null && Objects.equals(speedmine.getCurrentPos(), currentPosition.pos) && currentPosition.isValid(AutoMine.this, bomber, player, level, surrounding)) {
                    currentPositionValid = true;
                    return;
                }

                level.getMap().clear();
                Set<BlockPos> checkedPositions = new HashSet<>();
                Position position = null;
                for (Player enemy : clientLevel.players()) {
                    if (EntityUtil.isEnemyInRange(getPingBypass(), player, enemy, 8.0)) {
                        Set<BlockPos> surroundingEnemy = surround.getAllSurroundingPositions(enemy, player, clientLevel, Blocks.OBSIDIAN, false);
                        surroundingEnemy.removeIf(pos -> pos.getY() < enemy.getY() - 0.5);
                        for (BlockPos pos : surroundingEnemy) {
                            if (checkedPositions.add(pos) && !surrounding.contains(pos)) {
                                Position computed = computePos(bomber, pos, player, level);
                                if (computed != null && computed.isBetterThan(position, bomber.getAutoCrystal().balance().getValue())) {
                                    position = computed;
                                }
                            }
                        }
                    }
                }

                if (position == null || position.blowsUpDrop() == null) {
                    for (Player enemy : clientLevel.players()) {
                        if (EntityUtil.isEnemyInRange(getPingBypass(), player, enemy, 8.0)) {
                            Set<BlockPos> abovePlayer = PositionUtil.getPositionsBlockedByEntityAtY(enemy, enemy.getBoundingBox().minY + 2.5); // bomb down
                            Set<BlockPos> surroundingEnemy = surround.getAllSurroundingPositions(enemy, player, clientLevel, Blocks.OBSIDIAN, false);
                            surroundingEnemy.removeIf(pos -> pos.getY() < enemy.getY() - 0.5);
                            for (BlockPos enemySurrounding : surroundingEnemy) {
                                abovePlayer.add(enemySurrounding.above());
                            }

                            for (BlockPos pos : abovePlayer) {
                                if (checkedPositions.add(pos) && !surrounding.contains(pos)) {
                                    Position computed = computePos(bomber, pos, player, level);
                                    if (computed != null && computed.isBetterThan(position, bomber.getAutoCrystal().balance().getValue())) {
                                        position = computed;
                                    }
                                }
                            }
                        }
                    }
                }

                if (position != null) {
                    speedmine.startDestroy(position.pos, clientLevel, player);
                    currentPosition = position;
                    currentPositionValid = true;
                } else {
                    currentPositionValid = false;
                }
            }
        });
    }

    private @Nullable Position computePos(Bomber bomber, BlockPos pos, LocalPlayer player, BlockStateLevel.Delegating level) {
        if (player.getEyePosition().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > phobot.getAntiCheat().getMiningRangeSq()) {
            return null;
        }

        BlockState state = level.getBlockState(pos);
        if (speedmine.canMine(pos, state, player, level)) {
            if (phobot.getAntiCheat().getMiningStrictDirectionCheck().getStrictDirection(pos, player, level) == null) {
                return null;
            }

            CrystalPosition blocksSurround = bomber.findPositionToBlockSurround(positionsThatBlowUpDrops, pos, level, player);
            CrystalPosition blowsUpDrops = bomber.findBestPositionToBlowUpDrop(positionsThatBlowUpDrops, pos, level, player);
            CrystalPosition crystal = bomber.findCrystalThatBlowsUpDrop(positionsThatBlowUpDrops, pos, level, player);
            level.getMap().clear();
            if (!bomber.isValid(blowsUpDrops, blocksSurround, crystal)) {
                return null;
            }

            return new Position(blowsUpDrops, blocksSurround, crystal, pos);
        }

        return null;
    }

    // TODO: it does not really make sense to mine a position if we dont have anything to blow up the drop
    private record Position(@Nullable CrystalPosition blowsUpDrop, @Nullable CrystalPosition blocksSurround, @Nullable CrystalPosition crystal, BlockPos pos) {
        public boolean isValid(AutoMine autoMine, Bomber bomber, LocalPlayer player, BlockStateLevel.Delegating level, Set<BlockPos> surrounding) {
            level.getMap().put(pos, Blocks.OBSIDIAN.defaultBlockState()); // layer another CustomBlockStateLevel.Delegating on top since computePos will clear the map
            return !surrounding.contains(pos) && autoMine.computePos(bomber, pos, player, new BlockStateLevel.Delegating(level)) != null;
        }

        public boolean isBetterThan(@Nullable Position position, float balance) {
            if (position == null || blowsUpDrop != null && position.blowsUpDrop == null || blocksSurround != null && position.blocksSurround == null) {
                return true;
            }

            if (position.blowsUpDrop != null && blowsUpDrop == null || blocksSurround == null && position.blocksSurround != null) {
                return false;
            }

            if (blocksSurround != null) {
                return blocksSurround.isBetterThan(position.blocksSurround, balance);
            }

            if (blowsUpDrop != null) {
                return blowsUpDrop.isBetterThan(position.blowsUpDrop, balance);
            }

            if (crystal != null) {
                return crystal.isBetterThan(position.crystal, balance);
            }

            return false;
        }
    }

}
