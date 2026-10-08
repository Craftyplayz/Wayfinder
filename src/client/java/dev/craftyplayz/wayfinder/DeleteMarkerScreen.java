package dev.craftyplayz.wayfinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class DeleteMarkerScreen extends Screen {
    private final MarkerManagementScreen parent;
    private final MarkerManager manager;
    private final int index;
    private final String markerName;
    private Component error = Component.empty();

    DeleteMarkerScreen(MarkerManagementScreen parent, MarkerManager manager, int index) {
        super(Component.translatable("wayfinder.delete.title"));
        this.parent = parent;
        this.manager = manager;
        this.index = index;
        this.markerName = manager.markers().get(index).name();
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(146, (width - 24) / 2);
        addRenderableWidget(Button.builder(Component.translatable("wayfinder.markers.delete"), button -> {
            if (manager.remove(index)) {
                parent.mutationSucceeded(index);
            } else {
                error = Component.translatable("wayfinder.markers.write_error", manager.lastError());
            }
        }).bounds(width / 2 - buttonWidth - 2, height - 40, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 + 2, height - 40, buttonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
        MarkerManagementScreen.extractWrappedText(graphics, font,
                Component.translatable("wayfinder.delete.question", markerName),
                10, 60, width - 20, 3, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("wayfinder.delete.warning"),
                width / 2, 100, 0xFFCCCCCC);
        MarkerManagementScreen.extractError(graphics, font, error, 10, 120, width - 20,
                Math.max(1, (height - 50 - 120) / (font.lineHeight + 1)), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
