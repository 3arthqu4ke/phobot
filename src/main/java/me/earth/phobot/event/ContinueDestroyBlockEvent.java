package me.earth.phobot.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Getter
@RequiredArgsConstructor
public class ContinueDestroyBlockEvent extends CancellableEvent {
    private final BlockPos pos;
    private final Direction direction;

}
