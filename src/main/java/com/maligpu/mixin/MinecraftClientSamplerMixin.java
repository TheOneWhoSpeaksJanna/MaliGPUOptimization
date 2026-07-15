package com.maligpu.mixin;

import com.maligpu.MaliDebug;
import com.maligpu.MaliGPUConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-tick sampler: when debug logging is on, periodically logs loaded-chunk delta + GC pause
 * deltas so you can see what Minecraft is doing (and doing wrong) frame to frame. Engine-side.
 */
@Mixin(Minecraft.class)
public class MinecraftClientSamplerMixin {

    private static int tickCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void maligpu$sample(CallbackInfo ci) {
        if (!MaliGPUConfig.INSTANCE.debugLogging) return;
        tickCounter++;
        // Sample roughly once per second (~20 ticks).
        if (tickCounter % 20 == 0) {
            MaliDebug.sampleChunks();
            MaliDebug.gcSnapshot("tick");
        }
    }
}
