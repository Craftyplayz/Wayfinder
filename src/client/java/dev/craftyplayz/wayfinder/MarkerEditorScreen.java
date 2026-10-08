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
    private String pitch;
    private String yaw;
    private boolean enabled = true;
    private EditBox nameBox;
    private EditBox pitchBox;
    private EditBox yawBox;
    private Component error = Component.empty();

    public MarkerEditorScreen(MarkerManagementScreen parent, MarkerManager manager, int index) {
        super(Component.translatable(index < 0 ? "wayfinder.editor.add_title" : "wayfinder.editor.edit_title"));
        this.parent = parent;
        this.manager = manager;
        this.index = index;
        if (index >= 0) {
            Marker marker = manager.markers().get(index);
            name = marker.name();
            pitch = Double.toString(marker.pitch());
            yaw = Double.toString(marker.yaw());
            enabled = marker.enabled();
        }
    }

    @Override
    protected void init() {
        if (name == null) {
            name = Component.translatable("wayfinder.marker.default_name").getString();
            pitch = "0";
            yaw = "0";
            readCurrentView();
        }
        int left = (width - contentWidth()) / 2;
        int angleWidth = (contentWidth() - 4) / 2;
        nameBox = field("wayfinder.editor.name", left, 44, contentWidth(), name);
        nameBox.setResponder(value -> name = value);
        pitchBox = field("wayfinder.editor.pitch", left, 82, angleWidth, pitch);
        pitchBox.setResponder(value -> pitch = value);
        yawBox = field("wayfinder.editor.yaw", left + angleWidth + 4, 82, angleWidth, yaw);
        yawBox.setResponder(value -> yaw = value);

        int halfWidth = (contentWidth() - 4) / 2;
        button(Component.translatable("wayfinder.editor.enabled", MarkerManagementScreen.state(enabled)),
                left, 148, halfWidth, button -> {
                    enabled = !enabled;
                    button.setMessage(Component.translatable("wayfinder.editor.enabled",
                            MarkerManagementScreen.state(enabled)));
                });
        Button current = button(Component.translatable("wayfinder.editor.current_view"),
                left + halfWidth + 4, 148, halfWidth, button -> {
                    readCurrentView();
                    pitchBox.setValue(pitch);
                    yawBox.setValue(yaw);
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

    private void readCurrentView() {
        if (minecraft.player != null && minecraft.level != null) {
            Marker view = new Marker("View", minecraft.player.getXRot(), minecraft.player.getYRot(), true);
            pitch = Double.toString(view.pitch());
            yaw = Double.toString(view.yaw());
        }
    }

    private void save() {
        Marker marker;
        try {
            marker = new Marker(name, Double.parseDouble(pitch.trim()), Double.parseDouble(yaw.trim()), enabled);
        } catch (NumberFormatException exception) {
            error = Component.translatable("wayfinder.editor.invalid_angles");
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
        int angleWidth = (contentWidth() - 4) / 2;
        graphics.text(font, Component.translatable("wayfinder.editor.name"), left, 32, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("wayfinder.editor.pitch"), left, 70, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("wayfinder.editor.yaw"),
                left + angleWidth + 4, 70, 0xFFFFFFFF);
        MarkerManagementScreen.extractError(graphics, font, error, left, 176, contentWidth(),
                Math.max(1, (height - 34 - 176) / (font.lineHeight + 1)), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
