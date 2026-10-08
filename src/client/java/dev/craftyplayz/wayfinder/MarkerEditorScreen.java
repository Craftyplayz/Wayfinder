package dev.craftyplayz.wayfinder;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MarkerEditorScreen extends Screen {
    private final MarkerManagementScreen parent;
    private final MarkerManager manager;
    private final int index;
    private String name;
    private String x;
    private String y;
    private String z;
    private String dimension;
    private boolean enabled = true;
    private EditBox nameBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private EditBox dimensionBox;
    private Component error = Component.empty();

    public MarkerEditorScreen(MarkerManagementScreen parent, MarkerManager manager, int index) {
        super(Component.translatable(index < 0 ? "wayfinder.editor.add_title" : "wayfinder.editor.edit_title"));
        this.parent = parent;
        this.manager = manager;
        this.index = index;
        if (index >= 0) {
            Marker marker = manager.markers().get(index);
            name = marker.name();
            x = Double.toString(marker.x());
            y = Double.toString(marker.y());
            z = Double.toString(marker.z());
            dimension = marker.dimension();
            enabled = marker.enabled();
        }
    }

    @Override
    protected void init() {
        if (name == null) {
            name = Component.translatable("wayfinder.marker.default_name").getString();
            x = "0";
            y = "0";
            z = "0";
            dimension = "minecraft:overworld";
            readCurrentPosition();
        }
        int left = (width - contentWidth()) / 2;
        int coordinateWidth = (contentWidth() - 8) / 3;
        nameBox = field("wayfinder.editor.name", left, 44, contentWidth(), name);
        nameBox.setResponder(value -> name = value);
        xBox = field("wayfinder.editor.x", left, 82, coordinateWidth, x);
        xBox.setResponder(value -> x = value);
        yBox = field("wayfinder.editor.y", left + coordinateWidth + 4, 82, coordinateWidth, y);
        yBox.setResponder(value -> y = value);
        zBox = field("wayfinder.editor.z", left + (coordinateWidth + 4) * 2, 82, coordinateWidth, z);
        zBox.setResponder(value -> z = value);
        dimensionBox = field("wayfinder.editor.dimension", left, 120, contentWidth(), dimension);
        dimensionBox.setResponder(value -> dimension = value);

        int halfWidth = (contentWidth() - 4) / 2;
        button(Component.translatable("wayfinder.editor.enabled", MarkerManagementScreen.state(enabled)),
                left, 148, halfWidth, button -> {
                    enabled = !enabled;
                    button.setMessage(Component.translatable("wayfinder.editor.enabled",
                            MarkerManagementScreen.state(enabled)));
                });
        Button current = button(Component.translatable("wayfinder.editor.current_position"),
                left + halfWidth + 4, 148, halfWidth, button -> {
                    readCurrentPosition();
                    xBox.setValue(x);
                    yBox.setValue(y);
                    zBox.setValue(z);
                    dimensionBox.setValue(dimension);
                    error = Component.empty();
                });
        current.active = minecraft.player != null && minecraft.level != null;
        button(Component.translatable("wayfinder.editor.save"),
                left, height - 30, halfWidth, button -> save());
        button(Component.translatable("gui.cancel"),
                left + halfWidth + 4, height - 30, halfWidth, button -> onClose());
    }

    private int contentWidth() {
        return Math.min(360, width - 20);
    }

    private EditBox field(String key, int left, int top, int fieldWidth, String value) {
        EditBox field = new EditBox(font, left, top, fieldWidth, 20, Component.translatable(key));
        field.setMaxLength(512);
        field.setValue(value);
        return addRenderableWidget(field);
    }

    private Button button(Component label, int left, int top, int buttonWidth, Consumer<Button> action) {
        return addRenderableWidget(Button.builder(label, action::accept)
                .bounds(left, top, buttonWidth, 20).build());
    }

    private void readCurrentPosition() {
        if (minecraft.player != null && minecraft.level != null) {
            x = Double.toString(minecraft.player.getX());
            y = Double.toString(minecraft.player.getY());
            z = Double.toString(minecraft.player.getZ());
            dimension = minecraft.level.dimension().identifier().toString();
        }
    }

    private void save() {
        Marker marker;
        try {
            marker = new Marker(name, Double.parseDouble(x.trim()), Double.parseDouble(y.trim()),
                    Double.parseDouble(z.trim()), enabled, dimension);
        } catch (NumberFormatException exception) {
            error = Component.translatable("wayfinder.editor.invalid_coordinates");
            return;
        } catch (IllegalArgumentException exception) {
            error = Component.translatable("wayfinder.editor.invalid_marker", exception.getMessage());
            return;
        }
        boolean saved = index < 0 ? manager.add(marker) : manager.update(index, marker);
        if (saved) {
            parent.mutationSucceeded(index < 0 ? manager.markers().size() - 1 : index);
        } else {
            error = Component.translatable("wayfinder.markers.write_error", manager.lastError());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        int left = (width - contentWidth()) / 2;
        int coordinateWidth = (contentWidth() - 8) / 3;
        graphics.text(font, Component.translatable("wayfinder.editor.name"), left, 32, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("wayfinder.editor.x"), left, 70, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("wayfinder.editor.y"),
                left + coordinateWidth + 4, 70, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("wayfinder.editor.z"),
                left + (coordinateWidth + 4) * 2, 70, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("wayfinder.editor.dimension"), left, 108, 0xFFFFFFFF);
        MarkerManagementScreen.extractError(graphics, font, error, left, 176, contentWidth(),
                Math.max(1, (height - 34 - 176) / (font.lineHeight + 1)), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
