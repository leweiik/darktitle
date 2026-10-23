package com.example.darktitle.mixin.client;

import com.example.darktitle.DarkTitleRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs right after the vanilla background (the panorama) is drawn and before the
 * logo and buttons, so Singleplayer / Multiplayer etc. stay on top and clickable.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void darktitle$afterBackground(GuiGraphics graphics, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof TitleScreen) {
            DarkTitleRenderer.render(graphics);
        }
    }
}
