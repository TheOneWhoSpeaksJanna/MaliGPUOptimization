package com.maligpu.mixin;

import com.maligpu.MaliGPUConfig;
import com.maligpu.MaliGPUMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla resource-hold cleanup (Debugify-style safe subset).
 *
 * On world disconnect, vanilla does not always promptly release the level renderer's GPU/upload
 * buffers and the live sound set. Over many connect/disconnect cycles (or quick world hops) this
 * holds heap + native memory that the GC cannot reclaim until much later, contributing to the
 * "extended session" memory creep the research flags. We force release on clearClientLevel.
 * Engine-side, Vulkan-safe: this is teardown, not a render-path hook.
 */
@Mixin(Minecraft.class)
public class MinecraftDisconnectMixin {

    @Inject(method = "clearClientLevel", at = @At("HEAD"))
    private void maligpu$releaseLevelResources(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        if (!MaliGPUConfig.INSTANCE.patchMemoryLeaks) return;
        Minecraft mc = (Minecraft) (Object) this;
        try {
            mc.levelRenderer.close();
        } catch (Throwable ignored) {
        }
        try {
            mc.getSoundManager().stop();
        } catch (Throwable ignored) {
        }
        MaliGPUMod.LOGGER.info("[MaliGPUOptimization] Released level renderer + sound engine resources on disconnect (leak-patch).");
    }
}
