package com.maligpu.mixin;

import com.maligpu.MaliGPUConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dynamic FPS (engine-side, Vulkan-safe): throttle the client frame rate when the window is
 * unfocused or hidden, conserving CPU/GPU/battery. Read every client tick from the Window focus
 * state and apply via the public Options.framerateLimit API. No renderer / Vulkan path touched.
 */
@Mixin(Minecraft.class)
public class MinecraftClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void maligpu$dynamicFps(CallbackInfo ci) {
        if (!MaliGPUConfig.INSTANCE.dynamicFps) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.options == null || mc.getWindow() == null) return;

        com.mojang.blaze3d.platform.Window w = mc.getWindow();
        int userLimit = mc.options.framerateLimit().get();
        // Respect an explicit user cap: don't push above it. 0 (UNLIMITED) -> use refresh as ceiling.
        int ceiling = (userLimit <= 0) ? Math.max(30, w.getRefreshRate()) : userLimit;

        int target;
        if (w.isIconified() || w.isMinimized()) {
            target = MaliGPUConfig.INSTANCE.dynamicFpsInvisible; // 0 = pause rendering while hidden
        } else if (!w.isFocused()) {
            target = Math.min(ceiling, Math.max(1, MaliGPUConfig.INSTANCE.dynamicFpsUnfocused));
        } else {
            target = ceiling; // focused: use the user's setting / refresh
        }

        if (target != lastCap) {
            mc.options.framerateLimit().set(target);
            lastCap = target;
        }
    }

    private static int lastCap = -1;
}
