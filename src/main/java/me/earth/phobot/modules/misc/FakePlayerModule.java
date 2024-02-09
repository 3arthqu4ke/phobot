package me.earth.phobot.modules.misc;

import me.earth.phobot.ducks.IDamageProtectionEntity;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

public class FakePlayerModule extends ModuleImpl {
    private static final int ID = -2352352;

    public FakePlayerModule(PingBypass pingBypass) {
        super(pingBypass, "FakePlayer", Categories.MISC, "Creates a FakePlayer to test stuff with.");
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
    }

    @Override
    protected void onEnable() {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level != null && player != null) {
            FakePlayer fakePlayer = new FakePlayer(level);
            fakePlayer.setId(ID);
            fakePlayer.copyPosition(player);
            for (int i = 0; i < fakePlayer.getInventory().getContainerSize(); i++) {
                fakePlayer.getInventory().setItem(i, player.getInventory().getItem(i));
            }

            level.addEntity(fakePlayer);
            fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
            fakePlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0));
            fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
            //noinspection DataFlowIssue
            ((IDamageProtectionEntity) fakePlayer).phobot$setDamageProtection(((IDamageProtectionEntity) player).phobot$damageProtection());
        } else {
            disable();
        }
    }

    @Override
    protected void onDisable() {
        ClientLevel level = mc.level;
        if (level != null) {
            level.removeEntity(ID, Entity.RemovalReason.DISCARDED);
        }
    }

}
