package dev.craftyplayz.wayfinder;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public final class MarkerHudRenderer {
    private static final int COLOR = 0xFF80E8FF;
    private final MarkerManager manager;
    private float qx, qy, qz, qw;
    private double verticalFov;
    private String dimension;
    private boolean cameraReady;
    private int[] left = new int[16], top = new int[16], right = new int[16], bottom = new int[16];
    private int labels;

    public MarkerHudRenderer(MarkerManager manager) {
        this.manager = manager;
    }

    public void captureCamera(LevelExtractionContext context) {
        cameraReady = false;
        Minecraft client = Minecraft.getInstance();
        if (!KeyBindings.SHOW.isDown() || client.screen != null || client.player == null) return;
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (!camera.initialized) return;
        qx = camera.orientation.x();
        qy = camera.orientation.y();
        qz = camera.orientation.z();
        qw = camera.orientation.w();
        // Read the real world projection, including dynamic FOV (sprinting, zoom, etc.).
        verticalFov = Math.toDegrees(2 * Math.atan(1 / camera.projectionMatrix.m11()));
        dimension = context.level().dimension().identifier().toString();
        cameraReady = Double.isFinite(verticalFov) && verticalFov > 0 && verticalFov < 180;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!KeyBindings.SHOW.isDown() || !cameraReady || client.screen != null
                || client.player == null || client.level == null || client.options.hideGui
                || !client.isWindowActive()
                || !dimension.equals(client.level.dimension().identifier().toString())) return;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (width <= 32 || height <= 32) return;
        List<Marker> markers = manager.markers();
        HudSettings settings = manager.settings();
        labels = 0;
        for (Marker marker : markers) {
            if (!marker.enabled()) continue;
            MarkerProjection.Point point = MarkerProjection.projectAngles(marker.pitch(), marker.yaw(), qx, qy, qz, qw,
                    verticalFov, width, height, 12);
            if (point.offscreen() && !settings.showOffscreen()) continue;
            int x = (int) Math.round(point.x());
            int y = (int) Math.round(point.y());
            if (point.offscreen()) {
                arrow(graphics, x, y, point.angle());
            } else {
                graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xCC000000);
                graphics.fill(x - 1, y - 1, x + 2, y + 2, COLOR);
            }
            String name = settings.showLabels()
                    ? client.font.plainSubstrByWidth(marker.name(), Math.max(1, width - 24)) : "";
            if (name.isEmpty()) continue;
            int boxWidth = client.font.width(name) + 6;
            int boxHeight = client.font.lineHeight + 5;
            if (!placeLabel(x, y, boxWidth, boxHeight, width, height)) continue;
            int index = labels - 1;
            int textY = top[index] + 2;
            if (Math.abs(top[index] - y) > boxHeight || Math.abs(left[index] - x) > boxWidth) {
                int endX = Math.clamp(x, left[index], right[index]);
                int endY = Math.clamp(y, top[index], bottom[index]);
                graphics.fill(Math.min(x, endX), y, Math.max(x, endX) + 1, y + 1, 0x9980E8FF);
                graphics.fill(endX, Math.min(y, endY), endX + 1, Math.max(y, endY) + 1, 0x9980E8FF);
            }
            graphics.fill(left[index], top[index], right[index], bottom[index], 0xAA000000);
            graphics.text(client.font, name, left[index] + 3, textY, 0xFFFFFFFF);
        }
    }

    private boolean placeLabel(int x, int y, int w, int h, int width, int height) {
        // Alternate above/below the anchor; leader lines preserve the true marker direction.
        for (int attempt = 0; attempt < 32; attempt++) {
            int offset = attempt == 0 ? 0 : ((attempt + 1) / 2) * (h + 2) * (attempt % 2 == 1 ? 1 : -1);
            int l = Math.clamp(x + 6, 4, Math.max(4, width - w - 4));
            int t = Math.clamp(y - h / 2 + offset, 4, Math.max(4, height - h - 4));
            boolean collision = false;
            for (int i = 0; i < labels; i++) {
                if (l < right[i] + 2 && l + w + 2 > left[i] && t < bottom[i] + 2 && t + h + 2 > top[i]) {
                    collision = true;
                    break;
                }
            }
            if (collision) continue;
            if (labels == left.length) {
                left = java.util.Arrays.copyOf(left, labels * 2);
                top = java.util.Arrays.copyOf(top, labels * 2);
                right = java.util.Arrays.copyOf(right, labels * 2);
                bottom = java.util.Arrays.copyOf(bottom, labels * 2);
            }
            left[labels] = l;
            top[labels] = t;
            right[labels] = l + w;
            bottom[labels] = t + h;
            labels++;
            return true;
        }
        // In a saturated viewport keep the directional icons instead of overlapping text.
        return false;
    }

    private static void arrow(GuiGraphicsExtractor graphics, int x, int y, double angle) {
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        int tailX = x - (int) Math.round(dx * 5);
        int tailY = y - (int) Math.round(dy * 5);
        int tipX = x + (int) Math.round(dx * 5);
        int tipY = y + (int) Math.round(dy * 5);
        graphics.fill(x - 7, y - 7, x + 8, y + 8, 0xAA000000);
        line(graphics, tailX, tailY, tipX, tipY, COLOR);
        line(graphics, tipX, tipY, tipX - (int) Math.round(dx * 4 - dy * 3),
                tipY - (int) Math.round(dy * 4 + dx * 3), COLOR);
        line(graphics, tipX, tipY, tipX - (int) Math.round(dx * 4 + dy * 3),
                tipY - (int) Math.round(dy * 4 - dx * 3), COLOR);
    }

    private static void line(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int error = dx - dy;
        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) return;
            int twice = 2 * error;
            if (twice > -dy) { error -= dy; x1 += sx; }
            if (twice < dx) { error += dx; y1 += sy; }
        }
    }
}
