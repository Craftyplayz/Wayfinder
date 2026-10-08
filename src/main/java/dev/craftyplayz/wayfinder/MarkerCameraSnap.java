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

    public Marker update(boolean active, String dimension, List<Marker> markers, double maxDistance,
                         double x, double y, double z, double lookX, double lookY, double lookZ,
                         long now) {
        if (!active) {
            reset();
            return null;
        }
        if (target != null) {
            if (now - started < HOLD_NANOS && markers.contains(target)
                    && eligible(target, dimension, maxDistance, x, y, z)) {
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
            if (!eligible(marker, dimension, maxDistance, x, y, z)) continue;
            double dx = marker.x() - x, dy = marker.y() - y, dz = marker.z() - z;
            double distance = Math.hypot(Math.hypot(dx, dy), dz);
            double cosine = (dx / distance) * (lookX / lookLength)
                    + (dy / distance) * (lookY / lookLength)
                    + (dz / distance) * (lookZ / lookLength);
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

    private static boolean eligible(Marker marker, String dimension, double maxDistance,
                                    double x, double y, double z) {
        double distance = Math.hypot(Math.hypot(marker.x() - x, marker.y() - y), marker.z() - z);
        return marker.enabled() && marker.dimension().equals(dimension)
                && distance > 1.0e-6 && distance <= maxDistance;
    }
}
