package me.earth.phobot.holes;

import net.minecraft.core.Vec3i;

public interface HoleOffsets {
    //  --------------> x-axis
    //        x <-
    //      x a x
    //        x
    Vec3i[] OFFSETS_1x1 = new Vec3i[]{
            new Vec3i(0, 0, 0),
            new Vec3i(0, 0, -1),
            new Vec3i(1, 0, -1),
            new Vec3i(-1, 0, -1),
            new Vec3i(0, 0, -2),
            new Vec3i(0, -1, -1),
            new Vec3i(0, 1, -1),
            new Vec3i(0, 2, -1)
    };

    //  --------------> x-axis
    //     x x <-
    //   x a a x
    //     x x
    Vec3i[] OFFSETS_2x1_X = new Vec3i[]{
            new Vec3i(0, 0, 0),
            new Vec3i(0, 0, -1),
            new Vec3i(1, 0, -1),
            new Vec3i(-1, 0, -1),
            new Vec3i(0, 0, -2),
            new Vec3i(0, -1, -1),
            new Vec3i(0, 1, -1),
            new Vec3i(0, 2, -1),
            // same as 1x1 we just need to check these
            new Vec3i(-1, 0, 0),   //    -> x x
            new Vec3i(-2, 0, -1),  //  -> x y a x and above/under the one with y
            new Vec3i(-1, 0, -2),  //    -> x x
            new Vec3i(-1, -1, -1),
            new Vec3i(-1, 1, -1),
            new Vec3i(-1, 2, -1),
    };

    //  --------------> x-axis
    //      x
    //    x a x <-
    //    x a x
    //      x
    Vec3i[] OFFSETS_2x1_Z = new Vec3i[]{
            new Vec3i(0, 0, 0),
            new Vec3i(-1, -1, 0),
            new Vec3i(-1, 0, 0),
            new Vec3i(-1, 1, 0),
            new Vec3i(-1, 2, 0),
            new Vec3i(-2, 0, 0),
            new Vec3i(-1, 0, 1), // top
            new Vec3i(0, 0, -1),
            new Vec3i(-1, -1, -1),
            new Vec3i(-1, 0, -1),
            new Vec3i(-1, 1, -1),
            new Vec3i(-1, 2, -1),
            new Vec3i(-2, 0, -1),
            new Vec3i(-1, 0, -2)
    };

    //  --------------> x-axis
    //      x x
    //    x a a x <-
    //    x a a x
    //      x x
    Vec3i[] OFFSETS_2x2 = new Vec3i[]{
            new Vec3i(0, 0, 0),
            new Vec3i(-1, -1, 0),
            new Vec3i(-1, 0, 0),
            new Vec3i(-1, 1, 0),
            new Vec3i(-1, 2, 0),
            new Vec3i(-2, 0, 0),
            new Vec3i(-1, 0, 1), // top
            new Vec3i(0, 0, -1),
            new Vec3i(-1, -1, -1),
            new Vec3i(-1, 0, -1),
            new Vec3i(-1, 1, -1),
            new Vec3i(-1, 2, -1),
            new Vec3i(-2, 0, -1),
            new Vec3i(-1, 0, -2),
            // same as 2x1z we just need to check these
            new Vec3i(-2, 0, 1),   //    -> x x
            new Vec3i(-3, 0, 0),   //  -> x y a x  and above/under the ones with y
            new Vec3i(-3, 0, -1),  //  -> x y a x
            new Vec3i(-2, 0, -2),  //    -> x x
            new Vec3i(-2, -1, 0),
            new Vec3i(-2, 1, 0),
            new Vec3i(-2, 2, 0),
            new Vec3i(-2, -1, -1),
            new Vec3i(-2, 1, -1),
            new Vec3i(-2, 2, -1),
    };

    // note that these offsets are meant to be added after one another on a MutPos via increment
    Vec3i[] AIR_OFFSETS = new Vec3i[]{
            new Vec3i(0, 1, 0),
            new Vec3i(-1, -1, 0),
            new Vec3i(1, 0, 1),
            new Vec3i(1, 0, -1),
            new Vec3i(-1, 0, -1)
    };

    Vec3i[] BLOCK_OFFSETS = new Vec3i[]{
            new Vec3i(0, 0, 0),
            new Vec3i(0, -1, 0)
    };

}
