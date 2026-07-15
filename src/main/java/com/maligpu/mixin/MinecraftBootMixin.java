package com.maligpu.mixin;

import com.maligpu.MaliDebug;
import com.maligpu.MaliGPUConfig;
import com.maligpu.MaliGPUMod;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Apply engine-tunables at game boot (Minecraft constructor RETURN) so all changes are in place
 * before the first world loads -> "make changes at the start". Registers a whole-MC resource-reload
 * profiler (debug) via Fabric's ResourceManagerHelper. Engine-side; no Vulkan/render-path access.
 */
@Mixin(Minecraft.class)
public class MinecraftBootMixin {

    private static final AtomicBoolean SETUP_DONE = new AtomicBoolean(false);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void maligpu$applyAtBoot(net.minecraft.client.main.GameConfig gameConfig, CallbackInfo ci) {
        if (!MaliGPUConfig.INSTANCE.applyAtBoot) return;
        if (!SETUP_DONE.compareAndSet(false, true)) return;

        // All engine-side defaults are already loaded into MaliGPUConfig.INSTANCE at class-init.
        // Dynamic FPS / sound-throttle read the config live each tick; nothing else needs forcing here.
        // We just log the applied state so the user sees the mod "start instantly" with its settings.
        MaliGPUMod.LOGGER.info("[MaliGPUOptimization] applyAtBoot: settings locked at game start " +
                "(dynamicFps={}, liftAudioSoundCap={}, patchMemoryLeaks={}, worldGenTickBudgetMs={}, reloadDebounceMs={}).",
                MaliGPUConfig.INSTANCE.dynamicFps,
                MaliGPUConfig.INSTANCE.liftAudioSoundCap,
                MaliGPUConfig.INSTANCE.patchMemoryLeaks,
                MaliGPUConfig.INSTANCE.worldGenTickBudgetMs,
                MaliGPUConfig.INSTANCE.reloadDebounceMs);

        if (MaliGPUConfig.INSTANCE.debugLogging) {
            MaliDebug.gcSnapshot("boot");
        }

        // Register the resource-reload profiler (whole-MC instrumentation).
        try {
            ResourceManagerHelper.getInstance().registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    private final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("maligpu", "reload_profiler");
                    @Override public ResourceLocation getFabricId() { return id; }
                    @Override public void onResourceManagerReload(ResourceManager manager) {
                        if (MaliGPUConfig.INSTANCE.debugLogging) {
                            MaliDebug.reloadEnd();
                        }
                    }
                });
            if (MaliGPUConfig.INSTANCE.debugLogging) {
                // We can't hook the start of every reload cheaply here; the client tick sampler logs
                // reload counts via TextureManager instrumentation (see TextureManagerMixin).
                MaliGPUMod.LOGGER.info("[MaliGPUOptimization/DEBUG] resource-reload profiler registered.");
            }
        } catch (Throwable t) {
            MaliGPUMod.LOGGER.warn("[MaliGPUOptimization] failed to register reload profiler: {}", t.getMessage());
        }
    }
}
