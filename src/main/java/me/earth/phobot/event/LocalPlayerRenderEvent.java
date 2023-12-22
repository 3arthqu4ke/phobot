package me.earth.phobot.event;

public record LocalPlayerRenderEvent(boolean pre, float previousYRot, float previousXRot, float previousYRotO, float previousXRotO,
                                     float previousHeadYRot, float previousBodyYRot, float previousHeadYRotO, float previousBodyYRotO) {
    public static final LocalPlayerRenderEvent POST = new LocalPlayerRenderEvent(false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

}
