package me.earth.phobot.services;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.earth.phobot.util.time.TimeUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Getter
@EqualsAndHashCode(callSuper = true)
public class PlayerPosition extends Vec3 {
    @EqualsAndHashCode.Exclude
    private final long timeStamp = TimeUtil.getMillis();
    private final float yRot;
    private final float xRot;
    private final boolean onGround;

    public PlayerPosition() {
        this(0.0, 0.0, 0.0, 0.0f, 0.0f, false);
    }

    public PlayerPosition(Entity entity) {
        this(entity.getX(), entity.getY(), entity.getZ(), entity.getXRot(), entity.getYRot(), entity.onGround());
    }

    public PlayerPosition(PlayerPosition position, boolean onGround) {
        this(position.getX(), position.getY(), position.getZ(), position.getXRot(), position.getYRot(), onGround);
    }

    public PlayerPosition(PlayerPosition position, float xRot, float YRot, boolean onGround) {
        this(position.getX(), position.getY(), position.getZ(), xRot, YRot, onGround);
    }

    public PlayerPosition(PlayerPosition position, double x, double y, double z, boolean onGround) {
        this(x, y, z, position.getXRot(), position.getYRot(), onGround);
    }

    public PlayerPosition(double x, double y, double z, float xRot, float yRot, boolean onGround) {
        super(x, y, z);
        this.yRot = yRot;
        this.xRot = xRot;
        this.onGround = onGround;
    }

    public void applyTo(Entity entity) {
        entity.setPos(getX(), getY(), getZ());
        entity.setXRot(getXRot());
        entity.setYRot(getYRot());
        entity.setOnGround(isOnGround());
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

}
