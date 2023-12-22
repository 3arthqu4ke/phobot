package me.earth.phobot.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.earth.phobot.util.mutables.MutAABB;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.phobot.util.mutables.MutVec3;
import me.earth.phobot.util.mutables.MutableColor;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

/**
 * This event is only to be accessed when {@link RenderSystem#isOnRenderThread()} is {@code true}.
 * To prevent unnecessary allocations in the render loop this event is a Singleton and comes with a {@link MutAABB},
 * {@link MutPos} and {@link MutVec3} which are always preferred over any allocation of {@link AABB}, {@link BlockPos}
 * or {@link Vec3}.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderEvent {
    @Getter
    private static final RenderEvent instance = new RenderEvent();

    // TODO: private Frustum frustum?!
    private final MutableColor lineColor = new MutableColor();
    private final MutableColor boxColor = new MutableColor();
    private final MutVec3 from = new MutVec3();
    private final MutVec3 to = new MutVec3();
    private final MutAABB aabb = new MutAABB();
    private PoseStack poseStack = new PoseStack();
    private float tickDelta;
    private long limitTime;
    private Camera camera;

    public Entity getEntity() {
        return camera.getEntity();
    }

    public void setBoxColor(Color color, float boxAlpha) {
        lineColor.set(color);
        boxColor.set(color);
        boxColor.setAlpha(boxAlpha);
    }

    public void setBoxColor(float red, float green, float blue, float lineAlpha, float boxAlpha) {
        lineColor.set(red, green, blue, lineAlpha);
        boxColor.set(red, green, blue, boxAlpha);
    }

}
