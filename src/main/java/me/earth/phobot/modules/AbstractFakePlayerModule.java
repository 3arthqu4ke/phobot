package me.earth.phobot.modules;

import me.earth.phobot.Phobot;
import me.earth.phobot.ducks.IDamageProtectionEntity;
import me.earth.phobot.util.player.FakePlayer;
import me.earth.pingbypass.api.module.Category;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for modules that spawn a {@link FakePlayer} on your position.
 */
public class AbstractFakePlayerModule extends PhobotModule {
    protected final int id;

    public AbstractFakePlayerModule(Phobot phobot, String name, Category category, String description, int id) {
        super(phobot, name, category, description);
        this.id = id;
    }

    @Override
    protected void onDisable() {
        ClientLevel level = mc.level;
        if (level != null) {
            level.removeEntity(id, Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    protected void onEnable() {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level != null && player != null) {
            FakePlayer fakePlayer = create(player, level);
            level.addEntity(fakePlayer);
        } else {
            disable();
        }
    }

    public @Nullable Entity getPlayer(ClientLevel level) {
        return level.getEntity(id);
    }

    protected FakePlayer create(LocalPlayer player, ClientLevel level) {
        FakePlayer fakePlayer = instantiate(player, level);
        fakePlayer.setId(id);
        fakePlayer.copyPosition(player);
        for (int i = 0; i < fakePlayer.getInventory().getContainerSize(); i++) {
            fakePlayer.getInventory().setItem(i, player.getInventory().getItem(i));
        }

        fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
        //noinspection DataFlowIssue
        ((IDamageProtectionEntity) fakePlayer).phobot$setDamageProtection(((IDamageProtectionEntity) player).phobot$damageProtection());
        return fakePlayer;
    }

    protected FakePlayer instantiate(LocalPlayer player, ClientLevel level) {
        return new FakePlayer(level);
    }

}
