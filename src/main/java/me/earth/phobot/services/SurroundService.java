package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.modules.combat.Surround;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

// TODO: take blocks underneath us into account?
@Getter
public class SurroundService extends SubscriberImpl {
    // TODO: could this work questionably because of ghost blocks that we place on the client side?
    private final StopWatch.ForMultipleThreads lastSafe = new StopWatch.ForMultipleThreads();
    private final Surround surround;
    private Set<BlockPos> positions = new HashSet<>();
    private volatile boolean surrounded;

    public SurroundService(Surround surround) {
        this.surround = surround;
        listen(new SafeListener<PostMotionPlayerUpdateEvent>(surround.getPingBypass().getMinecraft()) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                surrounded = isSurrounded(surround.getPhobot().getLocalPlayerPositionService().getPlayerOnLastPosition(player));
                if (surrounded) {
                    lastSafe.reset();
                }
            }
        });
    }

    public boolean isSurrounded(Entity player) {
        LocalPlayer localPlayer = surround.getPingBypass().getMinecraft().player;
        if (localPlayer == null) {
            return false;
        }

        var positions = surround.getAllSurroundingPositions(player, localPlayer, localPlayer.clientLevel, Blocks.OBSIDIAN, false);
        positions.removeIf(pos -> pos.getY() < player.getY() - 0.5);
        this.positions = positions;
        // TODO: make this better, ensure blocks that cant be blown up
        return positions.stream().noneMatch(pos -> localPlayer.clientLevel.getBlockState(pos).getShape(localPlayer.clientLevel, pos).isEmpty());
    }

}
