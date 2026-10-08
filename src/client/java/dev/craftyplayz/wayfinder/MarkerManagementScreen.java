package dev.craftyplayz.wayfinder;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class MarkerManagementScreen extends Screen {
    private static final int ROW_HEIGHT = 36;
    private final Screen parent;
    private final MarkerManager manager;
    private int page;
    private int selected = -1;
    private int rowsPerPage;
    private Component error = Component.empty();

    public MarkerManagementScreen(Screen parent, MarkerManager manager) {
        super(Component.translatable("wayfinder.markers.title"));
        this.parent = parent;
        this.manager = manager;
        if (manager.lastError() != null) {
            error = Component.translatable("wayfinder.markers.write_error", manager.lastError());
        }
    }

    @Override
    protected void init() {
        List<Marker> markers = manager.markers();
        boolean recoveryAvailable = manager.lastError() != null;
        if (recoveryAvailable) {
            error = Component.translatable("wayfinder.markers.write_error", manager.lastError());
        }
        rowsPerPage = Math.max(1, (height - 164) / ROW_HEIGHT);
        page = Math.min(page, Math.max(0, (markers.size() - 1) / rowsPerPage));
        if (selected >= markers.size()) {
            selected = markers.size() - 1;
        }
        if (selected >= 0) {
            page = selected / rowsPerPage;
        }
        int left = (width - contentWidth()) / 2;
        int first = page * rowsPerPage;
        for (int row = 0; row < rowsPerPage && first + row < markers.size(); row++) {
            int index = first + row;
            Marker marker = markers.get(index);
            String rowKey = index == selected ? "wayfinder.markers.selected_row" : "wayfinder.markers.row";
            Component status = state(marker.enabled());
            int nameWidth = Math.max(0, contentWidth() - 12
                    - font.width(Component.translatable(rowKey, "", status)));
            Component label = Component.translatable(rowKey,
                    font.plainSubstrByWidth(marker.name(), nameWidth), status);
            addRenderableWidget(Button.builder(label, button -> {
                selected = index;
                refresh();
            }).bounds(left, 30 + row * ROW_HEIGHT, contentWidth(), 20)
                    .tooltip(Tooltip.create(Component.translatable("wayfinder.markers.tooltip",
                            marker.name(), marker.x(), marker.y(), marker.z(), marker.dimension(),
                            state(marker.enabled())))).build());
        }
        int pagingY = 30 + rowsPerPage * ROW_HEIGHT;
        Button previous = button(Component.translatable("wayfinder.markers.previous"),
                left, pagingY, 64, () -> changePage(-1));
        previous.active = page > 0;
        Button next = button(Component.translatable("wayfinder.markers.next"),
                left + contentWidth() - 64, pagingY, 64, () -> changePage(1));
        next.active = (page + 1) * rowsPerPage < markers.size();

        int gap = 4;
        int buttonWidth = (contentWidth() - gap * 2) / 3;
        int actionsY = height - 54;
        button(Component.translatable("wayfinder.markers.add"), left, actionsY, buttonWidth,
                () -> minecraft.setScreen(new MarkerEditorScreen(this, manager, -1)));
        Button edit = button(Component.translatable("wayfinder.markers.edit"),
                left + buttonWidth + gap, actionsY, buttonWidth,
                () -> minecraft.setScreen(new MarkerEditorScreen(this, manager, selected)));
        Button delete = button(Component.translatable("wayfinder.markers.delete"),
                left + (buttonWidth + gap) * 2, actionsY, buttonWidth,
                () -> minecraft.setScreen(new DeleteMarkerScreen(this, manager, selected)));
        edit.active = validSelection();
        delete.active = validSelection();
        int bottomColumns = recoveryAvailable ? 3 : 2;
        int bottomWidth = (contentWidth() - gap * (bottomColumns - 1)) / bottomColumns;
        Button toggle = button(Component.translatable(validSelection() && markers.get(selected).enabled()
                        ? "wayfinder.markers.disable" : "wayfinder.markers.enable"),
                left, height - 30, bottomWidth, this::toggleSelected);
        toggle.active = validSelection();
        if (recoveryAvailable) {
            button(Component.translatable("wayfinder.recovery.button"), left + bottomWidth + gap,
                    height - 30, bottomWidth, this::confirmRecovery);
        }
        button(Component.translatable("gui.done"), left + (bottomWidth + gap) * (bottomColumns - 1),
                height - 30, bottomWidth, this::onClose);
    }

    private Button button(Component label, int x, int y, int buttonWidth, Runnable action) {
        return addRenderableWidget(Button.builder(label, button -> action.run())
                .bounds(x, y, buttonWidth, 20).build());
    }

    private int contentWidth() {
        return Math.min(420, width - 20);
    }

    private boolean validSelection() {
        return selected >= 0 && selected < manager.markers().size();
    }

    private void changePage(int offset) {
        page += offset;
        selected = Math.min(page * rowsPerPage, manager.markers().size() - 1);
        refresh();
    }

    private void toggleSelected() {
        if (!validSelection()) {
            return;
        }
        if (manager.toggle(selected)) {
            error = Component.empty();
        } else {
            error = Component.translatable("wayfinder.markers.write_error", manager.lastError());
        }
        refresh();
    }

    private void confirmRecovery() {
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                if (manager.save()) {
                    error = Component.empty();
                } else {
                    error = Component.translatable("wayfinder.markers.write_error", manager.lastError());
                }
            }
            minecraft.setScreen(this);
        }, Component.translatable("wayfinder.recovery.title"),
                Component.translatable("wayfinder.recovery.message"),
                Component.translatable("wayfinder.recovery.confirm"),
                Component.translatable("gui.cancel")));
    }

    void mutationSucceeded(int index) {
        error = Component.empty();
        selected = Math.min(index, manager.markers().size() - 1);
        if (selected >= 0) {
            page = selected / Math.max(1, rowsPerPage);
        }
        minecraft.setScreen(this);
    }

    private void refresh() {
        rebuildWidgets();
    }

    static Component state(boolean enabled) {
        return Component.translatable(enabled ? "wayfinder.marker.enabled" : "wayfinder.marker.disabled");
    }

    static void extractWrappedText(GuiGraphicsExtractor graphics, Font font, Component message,
                                   int left, int top, int maxWidth, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(message, maxWidth);
        for (int line = 0; line < maxLines && line < lines.size(); line++) {
            graphics.text(font, lines.get(line),
                    left, top + line * (font.lineHeight + 1), color);
        }
    }

    static void extractError(GuiGraphicsExtractor graphics, Font font, Component error,
                             int left, int top, int maxWidth, int maxLines, int mouseX, int mouseY) {
        extractWrappedText(graphics, font, error, left, top, maxWidth, maxLines, 0xFFFF7777);
        if (!error.getString().isEmpty() && mouseX >= left && mouseX < left + maxWidth
                && mouseY >= top && mouseY < top + maxLines * (font.lineHeight + 1)) {
            graphics.setTooltipForNextFrame(font, error, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        int left = (width - contentWidth()) / 2;
        int first = page * rowsPerPage;
        List<Marker> markers = manager.markers();
        for (int row = 0; row < rowsPerPage && first + row < markers.size(); row++) {
            Marker marker = markers.get(first + row);
            int rowY = 30 + row * ROW_HEIGHT;
            graphics.enableScissor(left, rowY + 20, left + contentWidth(), rowY + ROW_HEIGHT);
            graphics.text(font, Component.translatable("wayfinder.markers.coordinates",
                    marker.x(), marker.y(), marker.z()), left + 4, rowY + 23, 0xFFCCCCCC);
            graphics.disableScissor();
        }
        int pagingY = 30 + rowsPerPage * ROW_HEIGHT;
        graphics.centeredText(font, Component.translatable("wayfinder.markers.page", page + 1,
                Math.max(1, (manager.markers().size() + rowsPerPage - 1) / rowsPerPage)),
                width / 2, pagingY + 6, 0xFFFFFFFF);
        if (manager.markers().isEmpty()) {
            graphics.centeredText(font, Component.translatable("wayfinder.markers.empty"),
                    width / 2, 42, 0xFFAAAAAA);
        }
        graphics.enableScissor(left, pagingY + 24, left + contentWidth(), height - 58);
        if (validSelection()) {
            Marker marker = manager.markers().get(selected);
            graphics.text(font, Component.translatable("wayfinder.markers.coordinates",
                    marker.x(), marker.y(), marker.z()), left, pagingY + 26, 0xFFFFFFFF);
            graphics.text(font, Component.translatable("wayfinder.markers.dimension",
                    marker.dimension()), left, pagingY + 38, 0xFFCCCCCC);
        } else {
            graphics.text(font, Component.translatable("wayfinder.markers.select"),
                    left, pagingY + 26, 0xFFCCCCCC);
        }
        graphics.disableScissor();
        extractError(graphics, font, error, left, pagingY + 52, contentWidth(),
                Math.max(1, (height - 58 - pagingY - 52) / (font.lineHeight + 1)), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
