package me.earth.phobot.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import me.earth.phobot.ducks.IEntityRenderDispatcher;
import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.util.mutables.MutAABB;
import me.earth.phobot.util.mutables.MutableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Objects;

@UtilityClass
public class Renderer {
    private static final IEntityRenderDispatcher RENDER_MANAGER = (IEntityRenderDispatcher) Objects.requireNonNull(Minecraft.getInstance().getEntityRenderDispatcher());
    private static final Tesselator TESSELATOR = Tesselator.getInstance();
    private static final BufferBuilder BUFFER = TESSELATOR.getBuilder();
    private static final MutAABB BB = new MutAABB();

    public static void renderBoxWithOutlineAndSides(RenderEvent event, float lineWidth, boolean ignoreDepth) {
        Renderer.startBox(ignoreDepth);
        Renderer.drawAABBBox(event);
        Renderer.end(ignoreDepth);
        Renderer.startLines(lineWidth, ignoreDepth);
        Renderer.drawAABBOutline(event);
        Renderer.end(ignoreDepth);
    }

    public static void startLines(float lineWidth, boolean ignoreDepth) {
        start(ignoreDepth);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        BUFFER.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    }

    public static void startBox(boolean ignoreDepth) {
        start(ignoreDepth);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void start(boolean ignoreDepth) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (ignoreDepth) {
            RenderSystem.disableDepthTest();
        }
    }

    public static void end(boolean ignoredDepth) {
        TESSELATOR.end();
        if (ignoredDepth) {
            RenderSystem.enableDepthTest();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * Draws a line from {@link RenderEvent#getFrom()} to {@link RenderEvent#getTo()} in the color of {@link RenderEvent#getLineColor()}.
     *
     * @param event the RenderEvent to use.
     */
    public static void drawLine(RenderEvent event) {
        double xO = RENDER_MANAGER.phobot$renderPosX();
        double yO = RENDER_MANAGER.phobot$renderPosY();
        double zO = RENDER_MANAGER.phobot$renderPosZ();

        double x1 = event.getFrom().getX() - xO;
        double y1 = event.getFrom().getY() - yO;
        double z1 = event.getFrom().getZ() - zO;

        double x2 = event.getTo().getX() - xO;
        double y2 = event.getTo().getY() - yO;
        double z2 = event.getTo().getZ() - zO;

        drawLine(event, x1, y1, z1, x2, y2, z2);
    }

    public static void drawAABBOutline(RenderEvent event) {
        BB.set(event.getAabb());
        BB.move(-RENDER_MANAGER.phobot$renderPosX(), -RENDER_MANAGER.phobot$renderPosY(), -RENDER_MANAGER.phobot$renderPosZ());

        drawLine(event, BB.getMinX(), BB.getMinY(), BB.getMinZ(), BB.getMaxX(), BB.getMinY(), BB.getMinZ(), 1.0, 0.0, 0.0);
        drawLine(event, BB.getMaxX(), BB.getMinY(), BB.getMinZ(), BB.getMaxX(), BB.getMinY(), BB.getMaxZ(), 0.0, 0.0, 1.0);
        drawLine(event, BB.getMaxX(), BB.getMinY(), BB.getMaxZ(), BB.getMinX(), BB.getMinY(), BB.getMaxZ(), -1.0, 0.0, 0.0);
        drawLine(event, BB.getMinX(), BB.getMinY(), BB.getMaxZ(), BB.getMinX(), BB.getMinY(), BB.getMinZ(), 0.0, 0.0, -1.0);

        drawLine(event, BB.getMinX(), BB.getMaxY(), BB.getMinZ(), BB.getMaxX(), BB.getMaxY(), BB.getMinZ(), 1.0, 0.0, 0.0);
        drawLine(event, BB.getMaxX(), BB.getMaxY(), BB.getMinZ(), BB.getMaxX(), BB.getMaxY(), BB.getMaxZ(), 0.0, 0.0, 1.0);
        drawLine(event, BB.getMaxX(), BB.getMaxY(), BB.getMaxZ(), BB.getMinX(), BB.getMaxY(), BB.getMaxZ(), -1.0, 0.0, 0.0);
        drawLine(event, BB.getMinX(), BB.getMaxY(), BB.getMaxZ(), BB.getMinX(), BB.getMaxY(), BB.getMinZ(), 0.0, 0.0, -1.0);

        drawLine(event, BB.getMinX(), BB.getMinY(), BB.getMinZ(), BB.getMinX(), BB.getMaxY(), BB.getMinZ(), 0.0, 1.0, 0.0);
        drawLine(event, BB.getMaxX(), BB.getMinY(), BB.getMinZ(), BB.getMaxX(), BB.getMaxY(), BB.getMinZ(), 0.0, 1.0, 0.0);
        drawLine(event, BB.getMaxX(), BB.getMinY(), BB.getMaxZ(), BB.getMaxX(), BB.getMaxY(), BB.getMaxZ(), 0.0, 1.0, 0.0);
        drawLine(event, BB.getMinX(), BB.getMinY(), BB.getMaxZ(), BB.getMinX(), BB.getMaxY(), BB.getMaxZ(), 0.0, 1.0, 0.0);
    }

    @SuppressWarnings("DuplicatedCode")
    public static void drawAABBBox(RenderEvent event) {
        BB.set(event.getAabb());
        BB.move(-RENDER_MANAGER.phobot$renderPosX(), -RENDER_MANAGER.phobot$renderPosY(), -RENDER_MANAGER.phobot$renderPosZ());
        Matrix4f matrix = event.getPoseStack().last().pose();

        float red = event.getBoxColor().getRed();
        float green = event.getBoxColor().getGreen();
        float blue = event.getBoxColor().getBlue();
        float alpha = event.getBoxColor().getAlpha();

        float x1 = (float) BB.getMinX();
        float y1 = (float) BB.getMinY();
        float z1 = (float) BB.getMinZ();

        float x2 = (float) BB.getMaxX();
        float y2 = (float) BB.getMaxY();
        float z2 = (float) BB.getMaxZ();

        BUFFER.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).endVertex();

        BUFFER.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).endVertex();

        BUFFER.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).endVertex();

        BUFFER.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).endVertex();

        BUFFER.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).endVertex();

        BUFFER.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).endVertex();
        BUFFER.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).endVertex();
    }

    private static void drawLine(RenderEvent event, double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        double distance = 1.0 / Math.max(Math.sqrt(dx * dx + dy * dy + dz * dz), Double.MIN_VALUE); // TODO: check this!
        float nx = (float) (dx * distance);
        float ny = (float) (dy * distance);
        float nz = (float) (dz * distance);

        drawLine(event, x1, y1, z1, x2, y2, z2, nx, ny, nz);
    }

    private static void drawLine(RenderEvent event, double x1, double y1, double z1, double x2, double y2, double z2, double nx, double ny, double nz) {
        drawLine(event, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, (float) nx, (float) ny, (float) nz);
    }

    private static void drawLine(RenderEvent event, float x1, float y1, float z1, float x2, float y2, float z2, float nx, float ny, float nz) {
        Matrix4f matrix = event.getPoseStack().last().pose();
        Matrix3f normal = event.getPoseStack().last().normal();
        MutableColor color = event.getLineColor();
        BUFFER.vertex(matrix, x1, y1, z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(normal, nx, ny, nz).endVertex();
        BUFFER.vertex(matrix, x2, y2, z2).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(normal, nx, ny, nz).endVertex();
    }

}
