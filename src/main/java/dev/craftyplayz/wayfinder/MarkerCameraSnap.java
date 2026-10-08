package dev.craftyplayz.wayfinder;

import java.util.List;

/** Camera-independent targeting and hold lifetime. */
public final class MarkerCameraSnap {
    private static final double SNAP_COSINE = Math.cos(Math.toRadians(5));
    private static final double REARM_COSINE = Math.cos(Math.toRadians(10));
    private static final long HOLD_NANOS = 1_000_000_000L;
    private Marker target;
    private long started;
    private boolean armed = true;

    public Marker update(boolean active, List<Marker> markers,
                         double lookX, double lookY, double lookZ,
                         long now) {
        if (!active) {
            reset();
            return null;
        }
        if (target != null) {
            if (now - started < HOLD_NANOS && markers.contains(target) && target.enabled()) {
                return target;
            }
            target = null;
            return null;
        }
        double lookLength = Math.hypot(Math.hypot(lookX, lookY), lookZ);
        if (!Double.isFinite(lookLength) || lookLength == 0) return null;
        Marker closest = null;
        double bestCosine = SNAP_COSINE;
        boolean nearMarker = false;
        for (Marker marker : markers) {
            if (!marker.enabled()) continue;
            double pitch = Math.toRadians(marker.pitch());
            double yaw = Math.toRadians(marker.yaw());
            double horizontal = Math.abs(marker.pitch()) == 90 ? 0 : Math.cos(pitch);
            double cosine = -Math.sin(yaw) * horizontal * (lookX / lookLength)
                    - Math.sin(pitch) * (lookY / lookLength)
                    + Math.cos(yaw) * horizontal * (lookZ / lookLength);
            if (cosine >= REARM_COSINE) nearMarker = true;
            if (cosine >= bestCosine) {
                closest = marker;
                bestCosine = cosine;
            }
        }
        if (!armed) {
            if (!nearMarker) armed = true;
            return null;
        }
        if (closest != null) {
            target = closest;
            started = now;
            armed = false;
        }
        return target;
    }

    public void reset() {
        target = null;
        armed = true;
    }

}
