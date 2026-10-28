package com.leweiik.title;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/** In-game settings: opened with the "Fire Settings" button on the title screen. */
public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Leweiik Title Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int colW = 150;
        int gap = 10;
        int left = this.width / 2 - colW - gap / 2;
        int right = this.width / 2 + gap / 2;
        int y = 36;
        int row = 24;

        // left column: the fire
        slider(left, y, colW, "Fire speed", 0.1, 4.0, TitleConfig.fireSpeed, false, v -> TitleConfig.fireSpeed = (float) v);
        slider(left, y + row, colW, "Fire height", 0.3, 2.5, TitleConfig.fireHeight, false, v -> TitleConfig.fireHeight = (float) v);
        slider(left, y + row * 2, colW, "Side fire", 0.0, 2.0, TitleConfig.sideFire, false, v -> TitleConfig.sideFire = (float) v);
        slider(left, y + row * 3, colW, "Glow", 0.0, 1.0, TitleConfig.glow, false, v -> TitleConfig.glow = (float) v);

        // right column: sparks, pixels, stickers
        slider(right, y, colW, "Embers", 0, 400, TitleConfig.embers, true, v -> TitleConfig.embers = (int) v);
        slider(right, y + row, colW, "Ember speed", 0.1, 4.0, TitleConfig.emberSpeed, false, v -> TitleConfig.emberSpeed = (float) v);
        slider(right, y + row * 2, colW, "Pixel detail", 60, 300, TitleConfig.pixelRows, true, v -> TitleConfig.pixelRows = (int) v);
        slider(right, y + row * 3, colW, "Sticker size", 16, 128, TitleConfig.stickerCells, true, v -> TitleConfig.stickerCells = (int) v);

        int by = y + row * 4 + 6;
        this.addRenderableWidget(Button.builder(enterLabel(), b -> {
            TitleConfig.enterScreen = !TitleConfig.enterScreen;
            b.setMessage(enterLabel());
        }).bounds(this.width / 2 - colW - gap / 2, by, colW * 2 + gap, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset to defaults"), b -> {
            TitleConfig.reset();
            TitleConfig.save();
            this.minecraft.setScreen(new SettingsScreen(this.parent));
        }).bounds(this.width / 2 - colW - gap / 2, by + row, colW, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 + gap / 2, by + row, colW, 20).build());
    }

    private static Component enterLabel() {
        return Component.literal("Press to Enter screen: " + (TitleConfig.enterScreen ? "ON" : "OFF"));
    }

    private void slider(int x, int y, int w, String label, double min, double max, double current,
                        boolean integer, DoubleConsumer setter) {
        this.addRenderableWidget(new Slider(x, y, w, 20, label, min, max, current, integer, setter));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        TitleConfig.save();
        this.minecraft.setScreen(this.parent);
    }

    /** A slider that writes its value straight into TitleConfig, so the fire behind it updates live. */
    private static final class Slider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final boolean integer;
        private final DoubleConsumer setter;

        Slider(int x, int y, int w, int h, String label, double min, double max, double current,
               boolean integer, DoubleConsumer setter) {
            super(x, y, w, h, Component.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.setter = setter;
            this.updateMessage();
        }

        private double current() {
            double v = this.min + (this.max - this.min) * this.value;
            return this.integer ? Math.round(v) : v;
        }

        @Override
        protected void updateMessage() {
            if (this.label == null) return; // may be called by the super constructor before fields are set
            double v = current();
            String text = this.integer ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v);
            this.setMessage(Component.literal(this.label + ": " + text));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(current());
        }
    }
}
