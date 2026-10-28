package com.leweiik.title.mixin.client;

import com.leweiik.title.SettingsScreen;
import com.leweiik.title.TitleRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs right after the vanilla panorama is drawn and before the logo and buttons,
 * so the menu stays on top and clickable. Also used behind the settings screen,
 * so you see your changes live.
 */
@Mixin(PanoramaRenderer.class)
public abstract class PanoramaMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void leweiik$afterPanorama(CallbackInfo ci,
                                       @Local(argsOnly = true) GuiGraphics graphics) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof TitleScreen || screen instanceof SettingsScreen) {
            TitleRenderer.render(graphics);
        }
    }
}
