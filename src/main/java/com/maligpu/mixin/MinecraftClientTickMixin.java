package com.maligpu.mixin;

import com.maligpu.ClientPerfManager;
import com.maligpu.GraphicsPresetApplier;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-tick hook: drives the all-in-one gap-fillers every frame without touching the render path.
 *  - capture the user's starting render/sim distances as the adaptive ceiling
 *  - apply the graphics auto-preset once
 *  - run Dynamic FPS + Adaptive distance sampling
 */
@Mixin(Minecraft.class)
public class MinecraftClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void maligpu$clientTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.level == null || mc.player == null) return;

        ClientPerfManager.captureUserBaseline(mc);
        GraphicsPresetApplier.applyOnFirstRun(mc);
        ClientPerfManager.tick(mc);
    }
}
