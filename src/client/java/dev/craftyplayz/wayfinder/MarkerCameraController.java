package dev.craftyplayz.wayfinder;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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

    public static boolean update(Camera camera, Vec3 eyePosition) {
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
        Vec3 position = camera.position();
        Vector3f forward = new Vector3f(0, 0, -1).rotate(camera.rotation());
        Marker marker = SNAP.update(true, client.level.dimension().identifier().toString(),
                manager.markers(), manager.settings().maxDistance(), position.x, position.y, position.z,
                forward.x(), forward.y(), forward.z(), System.nanoTime());
        if (marker == null) return false;
        double dx = marker.x() - eyePosition.x;
        double dy = marker.y() - eyePosition.y;
        double dz = marker.z() - eyePosition.z;
        if (cameraType.isMirrored()) {
            dx = -dx;
            dy = -dy;
            dz = -dz;
        }
        double horizontal = Math.hypot(dx, dz);
        if (Math.hypot(horizontal, dy) <= 1.0e-6) {
            SNAP.reset();
            return false;
        }
        float yaw = horizontal <= 1.0e-6 ? client.player.getYRot()
                : (float) Math.toDegrees(Math.atan2(-dx, dz));
        yaw = client.player.getYRot() + Mth.wrapDegrees(yaw - client.player.getYRot());
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
        client.player.yRotO = yaw;
        client.player.xRotO = pitch;
        return true;
    }
}
