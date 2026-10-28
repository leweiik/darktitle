package com.leweiik.title.mixin.client;

import com.leweiik.title.SettingsScreen;
import com.leweiik.title.TitleConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds the "Fire Settings" button and the "Press to Enter" start: until you press it, the normal
 * menu buttons (Singleplayer, Multiplayer, Mods, ...) are hidden.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique private Button leweiik$enterButton;
    @Unique private Button leweiik$settingsButton;
    @Unique private final List<AbstractWidget> leweiik$hidden = new ArrayList<>();

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void leweiik$addButtons(CallbackInfo ci) {
        this.leweiik$hidden.clear();

        this.leweiik$settingsButton = this.addRenderableWidget(
                Button.builder(Component.literal("Fire Settings"),
                                b -> this.minecraft.setScreen(new SettingsScreen(this)))
                        .bounds(6, 6, 100, 20).build());

        if (TitleConfig.enterScreen && !TitleConfig.entered) {
            this.leweiik$enterButton = this.addRenderableWidget(
                    Button.builder(Component.literal("Press to Enter"), b -> this.leweiik$enter())
                            .bounds(this.width / 2 - 100, this.height / 4 + 48, 200, 20).build());
        } else {
            this.leweiik$enterButton = null;
        }
    }

    /** Every frame until entered: keep all other buttons hidden (also ones other mods add later). */
    @Inject(method = "render", at = @At("HEAD"))
    private void leweiik$gate(CallbackInfo ci) {
        if (this.leweiik$enterButton == null || TitleConfig.entered) return;
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget
                    && widget != this.leweiik$enterButton
                    && widget != this.leweiik$settingsButton
                    && widget.visible) {
                widget.visible = false;
                this.leweiik$hidden.add(widget);
            }
        }
    }

    @Unique
    private void leweiik$enter() {
        TitleConfig.entered = true;
        for (AbstractWidget widget : this.leweiik$hidden) {
            widget.visible = true;
        }
        this.leweiik$hidden.clear();
        if (this.leweiik$enterButton != null) {
            this.leweiik$enterButton.visible = false;
            this.leweiik$enterButton.active = false;
        }
    }
}
