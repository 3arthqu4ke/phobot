package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Position;

/**
 * Allows you to spoof Position and rotation during a {@link PreMotionPlayerUpdateEvent}.
 * Spoofed changes will be reset on {@link PostMotionPlayerUpdateEvent}.
 */
@Getter
public class MotionUpdateService extends SubscriberImpl {
    private double x;
    private double y;
    private double z;
    private float yRot;
    private float xRot;
    private boolean onGround;

    private double changedX;
    private double changedY;
    private double changedZ;
    private float changedYRot;
    private float changedXRot;
    private boolean changedOnGround;
    boolean spoofing;

    private boolean inPreUpdate;

    private float yRotO = 0.0f;
    private float xRotO = 0.0f;

    public MotionUpdateService(Minecraft mc) {
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, Integer.MAX_VALUE) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                x = player.getX();
                y = player.getY();
                z = player.getZ();
                yRot = player.getYRot();
                xRot = player.getXRot();
                onGround = player.onGround();
                inPreUpdate = true;
                yRotO = changedYRot;
                xRotO = changedXRot;
                spoofing = false;
            }
        });

        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, Integer.MIN_VALUE) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                changedX = player.getX();
                changedY = player.getY();
                changedZ = player.getZ();
                changedYRot = player.getYRot();
                changedXRot = player.getXRot();
                changedOnGround = player.onGround();
                inPreUpdate = false;
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc, Integer.MIN_VALUE) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                // this allows us to detect if other clients have spoofed our position or if we have spoofed without this service to set the position permanently
                if (player.getX() == changedX && player.getY() == changedY && player.getZ() == changedZ) {
                    player.setPos(x, y, z);
                }

                if (player.getXRot() == changedXRot) {
                    player.setXRot(xRot);
                }

                if (player.getYRot() == changedYRot) {
                    player.setYRot(yRot);
                }

                if (player.onGround() == changedOnGround) {
                    player.setOnGround(onGround);
                }
            }
        });
    }

    public void setPosition(LocalPlayer player, Position pos) {
        if (inPreUpdate) {
            setPosition(player, pos.x(), pos.y(), pos.z());
            spoofing = true;
        }
    }

    public void setPosition(LocalPlayer player, double x, double y, double z) {
        if (inPreUpdate) {
            player.setPos(x, y, z);
            changedX = x;
            changedY = y;
            changedZ = z;
            spoofing = true;
        }
    }

    public void rotate(LocalPlayer player, float yRot, float xRot) {
        if (inPreUpdate) {
            player.setYRot(yRot);
            player.setXRot(xRot);
            changedYRot = yRot;
            changedXRot = xRot;
            spoofing = true;
        }
    }

    public void setOnGround(LocalPlayer player, boolean onGround) {
        if (inPreUpdate) {
            player.setOnGround(onGround);
            changedOnGround = onGround;
            spoofing = true;
        }
    }

}
