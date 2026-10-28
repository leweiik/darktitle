package com.example.darktitle.mixin.client;

import com.example.darktitle.DarkTitleRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs right after the vanilla panorama is drawn and before the logo and buttons,
 * so Singleplayer / Multiplayer etc. stay on top and clickable.
 * (File is still called ScreenMixin so the mixins.json does not need to change.)
 */
@Mixin(PanoramaRenderer.class)
public abstract class ScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void darktitle$afterPanorama(CallbackInfo ci,
                                         @Local(argsOnly = true) GuiGraphics graphics) {
        if (Minecraft.getInstance().screen instanceof TitleScreen) {
            DarkTitleRenderer.render(graphics);
        }
    }
}
