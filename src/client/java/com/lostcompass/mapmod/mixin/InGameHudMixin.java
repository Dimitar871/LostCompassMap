package com.lostcompass.mapmod.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    private static final float SCOREBOARD_OFFSET = 110.0f;

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"))
    private void lostcompassmap$offsetScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, SCOREBOARD_OFFSET);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("RETURN"))
    private void lostcompassmap$restoreScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        context.getMatrices().popMatrix();
    }
}
