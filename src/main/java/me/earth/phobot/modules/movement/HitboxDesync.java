package me.earth.phobot.modules.movement;

import me.earth.phobot.util.ResetUtil;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * <a href=https://github.com/mioclient/hitbox-desync>https://github.com/mioclient/hitbox-desync</a>
 *
 * @author Cattyn
 */
public class HitboxDesync extends ModuleImpl {
    private static final double MAGIC_OFFSET = .200009968835369999878673424677777777777761;

    public HitboxDesync(PingBypass pingBypass) {
        super(pingBypass, "HitboxDesync", Categories.MOVEMENT, "Cattyn's exploit.");
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
    }

    @Override
    protected void onEnable() {
        LocalPlayer player = mc.player;
        if (player == null) {
            this.disable();
            return;
        }

        Direction facing = player.getDirection();
        AABB bb = player.getBoundingBox();
        Vec3 center = bb.getCenter();
        Vec3 offset = new Vec3(facing.step());

        Vec3 fin = merge(Vec3.atBottomCenterOf(BlockPos.containing(center)).add(0.5, 0.0, 0.5).add(offset.scale(MAGIC_OFFSET)), facing);
        player.setPos(fin.x == 0 ? player.getX() : fin.x, player.getY(), fin.z == 0 ? player.getZ() : fin.z);
        this.disable();
    }

    private Vec3 merge(Vec3 vec, Direction facing) {
        return new Vec3(vec.x * Math.abs(facing.step().x()), vec.y * Math.abs(facing.step().y()), vec.z * Math.abs(facing.step().z()));
    }

}
