package me.earth.phobot.util.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FakePlayer extends AbstractClientPlayer {
    public static final GameProfile RANDOM_GAMEPROFILE = new GameProfile(UUID.randomUUID(), "FakePlayer");
    public static final PlayerInfo DUMMY_PLAYERINFO = new PlayerInfo(RANDOM_GAMEPROFILE, true);

    public FakePlayer(ClientLevel clientLevel) {
        super(clientLevel, RANDOM_GAMEPROFILE);
    }

    public FakePlayer(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    /*@Override
    public boolean isControlledByLocalInstance() {
        return true;
    }*/

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    protected PlayerInfo getPlayerInfo() {
        return DUMMY_PLAYERINFO;
    }

    @Override
    protected @NotNull MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    @Override
    protected void moveTowardsClosestSpace(double x, double y, double z) {
        // dont push out of blocks super.moveTowardsClosestSpace(d, e, f);
    }

}
