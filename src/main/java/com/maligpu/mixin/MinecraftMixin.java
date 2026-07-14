package com.maligpu.mixin;

import com.maligpu.MaliGPUConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    private static long maligpu$lastTickTime = 0L;

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void maligpu$throttleTick(CallbackInfo ci) {
        if (!MaliGPUConfig.INSTANCE.aggressiveTickThrottle) return;
        long now = System.nanoTime();
        long budgetNs = (long) (MaliGPUConfig.INSTANCE.tickBudgetSeconds * 1_000_000_000L);
        if (maligpu$lastTickTime != 0L) {
            long elapsed = now - maligpu$lastTickTime;
            if (elapsed < budgetNs) {
                ci.cancel();
            }
        }
        maligpu$lastTickTime = now;
    }
}
