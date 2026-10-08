package dev.craftyplayz.wayfinder;

public final class MarkerProjection {
    private MarkerProjection() {}

    /** Angle is in radians, clockwise from screen-right. */
    public record Point(double x, double y, boolean offscreen, double angle, double distance) {}

    /** Minecraft angles in degrees: yaw 0 faces +Z, positive pitch looks down. */
    public static Point projectAngles(double pitch, double yaw,
                                      double qx, double qy, double qz, double qw,
                                      double verticalFovDeg, double width, double height, double margin) {
        if (!Double.isFinite(pitch) || pitch < -90 || pitch > 90 || !Double.isFinite(yaw)) {
            throw new IllegalArgumentException("Invalid pitch or yaw");
        }
        double p = Math.toRadians(pitch);
        double y = Math.toRadians(yaw % 360);
        double horizontal = Math.abs(pitch) == 90 ? 0 : Math.cos(p);
        return project(-Math.sin(y) * horizontal, -Math.sin(p), Math.cos(y) * horizontal,
                qx, qy, qz, qw, verticalFovDeg, width, height, margin);
    }

    public static Point project(double dx, double dy, double dz,
                                double qx, double qy, double qz, double qw,
                                double verticalFovDeg, double width, double height, double margin) {
        if (!finite(dx, dy, dz) || !finite(qx, qy, qz) || !Double.isFinite(qw)) {
            throw new IllegalArgumentException("Projection inputs must be finite");
        }
        double norm = Math.hypot(Math.hypot(qx, qy), Math.hypot(qz, qw));
        if (!Double.isFinite(norm) || norm == 0) {
            throw new IllegalArgumentException("Camera quaternion must have a finite nonzero magnitude");
        }
        qx /= norm;
        qy /= norm;
        qz /= norm;
        qw /= norm;
        // The camera quaternion maps local to world; its transpose maps world to local.
        double x = (1 - 2 * (qy * qy + qz * qz)) * dx
                + 2 * (qx * qy + qz * qw) * dy + 2 * (qx * qz - qy * qw) * dz;
        double y = 2 * (qx * qy - qz * qw) * dx
                + (1 - 2 * (qx * qx + qz * qz)) * dy + 2 * (qy * qz + qx * qw) * dz;
        double z = 2 * (qx * qz + qy * qw) * dx
                + 2 * (qy * qz - qx * qw) * dy + (1 - 2 * (qx * qx + qy * qy)) * dz;
        return project(x, y, z, verticalFovDeg, width, height, margin);
    }

    /** Projects camera-local coordinates: forward -Z, right +X, up +Y. */
    public static Point project(double x, double y, double z, double verticalFovDeg,
                                double width, double height, double margin) {
        if (!finite(x, y, z) || !finite(verticalFovDeg, width, height) || !Double.isFinite(margin)) {
            throw new IllegalArgumentException("Projection inputs must be finite");
        }
        if (verticalFovDeg <= 0 || verticalFovDeg >= 180 || width <= 0 || height <= 0
                || margin < 0 || margin >= Math.min(width, height) / 2) {
            throw new IllegalArgumentException("Invalid FOV, viewport, or margin");
        }
        double distance = Math.hypot(Math.hypot(x, y), z);
        if (!Double.isFinite(distance)) {
            throw new IllegalArgumentException("Position magnitude is too large");
        }
        double cx = width / 2;
        double cy = height / 2;
        if (distance == 0) {
            return new Point(cx, cy, false, 0, 0);
        }
        double focal = cy / Math.tan(Math.toRadians(verticalFovDeg) / 2);
        double angle = x == 0 && y == 0 ? 0 : Math.atan2(-y, x);
        if (z < 0) {
            double px = cx + (x / -z) * focal;
            double py = cy - (y / -z) * focal;
            if (px >= margin && px <= width - margin && py >= margin && py <= height - margin) {
                return new Point(px, py, false, angle, distance);
            }
        }
        // Never divide a behind-camera direction by negative depth: that reverses arrows.
        double sx = Math.cos(angle);
        double sy = Math.sin(angle);
        double scale = Math.min((cx - margin) / Math.abs(sx), (cy - margin) / Math.abs(sy));
        double px = Math.clamp(cx + sx * scale, margin, width - margin);
        double py = Math.clamp(cy + sy * scale, margin, height - margin);
        return new Point(px, py, true, angle, distance);
    }

    private static boolean finite(double a, double b, double c) {
        return Double.isFinite(a) && Double.isFinite(b) && Double.isFinite(c);
    }
}
