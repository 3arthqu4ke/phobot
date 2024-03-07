package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.SafeListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Position;

@Getter
public class MotionUpdateService extends SubscriberImpl {
    private double changedX;
    private double changedY;
    private double changedZ;
    private double x;
    private double y;
    private double z;

    private float changedXRot;
    private float changedYRot;
    private float yRot;
    private float xRot;

    private boolean changedOnground;
    private boolean onGround;

    private boolean inPreUpdate;
    boolean spoofing;

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
                changedOnground = player.onGround();
                inPreUpdate = false;
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc, Integer.MIN_VALUE) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                double playerX = player.getX();
                double playerY = player.getY();
                double playerZ = player.getZ();
                boolean changed = false;
                if (player.getX() == changedX) {
                    changed = true;
                    playerX = x;
                }

                if (player.getY() == changedY) {
                    changed = true;
                    playerY = y;
                }

                if (player.getZ() == changedZ) {
                    changed = true;
                    playerZ = z;
                }

                if (changed) {
                    player.setPos(playerX, playerY, playerZ);
                }

                if (player.getXRot() == changedXRot) {
                    player.setXRot(xRot);
                }

                if (player.getYRot() == changedYRot) {
                    player.setYRot(yRot);
                }

                if (player.onGround() == changedOnground) {
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
            spoofing = true;
        }
    }

    public void setOnGround(LocalPlayer player, boolean onGround) {
        if (inPreUpdate) {
            player.setOnGround(onGround);
            spoofing = true;
        }
    }

    public void rotate(LocalPlayer player, float yRot, float xRot) {
        if (inPreUpdate) {
            player.setYRot(yRot);
            player.setXRot(xRot);
            spoofing = true;
        }
    }

}
