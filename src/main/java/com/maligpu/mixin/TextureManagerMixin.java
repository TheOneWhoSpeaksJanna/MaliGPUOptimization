package com.maligpu.mixin;

import com.maligpu.MaliDebug;
import com.maligpu.MaliGPUConfig;
import com.maligpu.MaliGPUMod;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Instrument texture/atlas reloads (the bandwidth-heavy part of a resource reload on Mali).
 * Counts rebuilds and, in debug mode, marks the reload start so the profiler can time it.
 * Visibility-only: never blocks or alters the reload.
 */
@Mixin(TextureManager.class)
public class TextureManagerMixin {

    @Inject(method = "reload",
            at = @At("HEAD"),
            cancellable = false,
            remap = false)
    private void maligpu$reloadStart(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        // HEAD of reload -> record start for profiling
        if (MaliGPUConfig.INSTANCE.debugLogging) {
            MaliDebug.reloadStart();
        }
    }

    @Inject(method = "reload",
            at = @At("RETURN"),
            cancellable = false,
            remap = false)
    private void maligpu$reloadReturn(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (MaliGPUConfig.INSTANCE.trackAtlasRebuilds && !MaliGPUConfig.INSTANCE.debugLogging) {
            // Non-debug: just note every reload so the user can see how often full rebuilds happen.
            MaliGPUMod.LOGGER.info("[MaliGPUOptimization] resource reload fired (texture/atlas rebuild). " +
                    "Frequent reloads re-upload all atlases to GPU (bandwidth tax on Mali).");
        }
    }
}
