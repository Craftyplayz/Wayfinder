package dev.craftyplayz.wayfinder;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class MarkerCameraController {
    private static final MarkerCameraSnap SNAP = new MarkerCameraSnap();
    private static MarkerManager manager;
    private static ClientLevel lastLevel;
    private static CameraType lastCameraType;

    private MarkerCameraController() {}

    public static void initialize(MarkerManager markerManager) {
        manager = markerManager;
    }

    public static boolean update(Camera camera) {
        Minecraft client = Minecraft.getInstance();
        CameraType cameraType = client.options.getCameraType();
        if (lastLevel != client.level || lastCameraType != cameraType) SNAP.reset();
        lastLevel = client.level;
        lastCameraType = cameraType;
        boolean active = manager != null && KeyBindings.SHOW.isDown() && client.screen == null
                && client.player != null && client.level != null && !client.options.hideGui
                && client.isWindowActive() && !client.isPaused() && camera.entity() == client.player
                && client.player.isAlive() && !client.player.isSleeping();
        if (!active) {
            SNAP.reset();
            return false;
        }
        Vector3f forward = new Vector3f(0, 0, -1).rotate(camera.rotation());
        Marker marker = SNAP.update(true, manager.markers(),
                forward.x(), forward.y(), forward.z(), System.nanoTime());
        if (marker == null) return false;
        float yaw = (float) marker.yaw() + (cameraType.isMirrored() ? 180 : 0);
        yaw = client.player.getYRot() + Mth.wrapDegrees(yaw - client.player.getYRot());
        float pitch = (float) marker.pitch() * (cameraType.isMirrored() ? -1 : 1);
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
        client.player.yRotO = yaw;
        client.player.xRotO = pitch;
        return true;
    }
}
