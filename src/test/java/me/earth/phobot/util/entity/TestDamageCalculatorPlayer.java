package me.earth.phobot.util.entity;

import me.earth.phobot.TestUtil;
import me.earth.phobot.util.player.DamageCalculatorPlayer;
import me.earth.phobot.util.player.FakePlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TestDamageCalculatorPlayer {
    @Test
    public void testDamageCalculatorPlayer() throws IOException {
        try (ClientLevel level = TestUtil.createClientLevel()) {
            FakePlayer fakePlayer = new FakePlayer(level);
            assertDoesNotThrow(() -> new DamageCalculatorPlayer(fakePlayer));
        }
    }

}
