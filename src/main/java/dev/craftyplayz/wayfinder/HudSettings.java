package dev.craftyplayz.wayfinder;

public record HudSettings(boolean showLabels, boolean showOffscreen) {
    public static final HudSettings DEFAULTS = new HudSettings(true, true);
}
