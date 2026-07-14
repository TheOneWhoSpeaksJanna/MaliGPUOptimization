package com.maligpu.mixin;

import com.maligpu.MaliGPUConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    // Engine-side particle spawn throttle. ParticleEngine.add() is game logic (not the GL/Vulkan
    // render path), so this is safe under both the OpenGL/Krypton and VulkanMod renderers.
    // Far-particle distance culling reduces vertex submission cost on Mali TBDR.
    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void maligpu$limitParticle(Particle particle, CallbackInfo ci) {
        MaliGPUConfig cfg = MaliGPUConfig.INSTANCE;
        if (!cfg.particleLimit) return;

        if (cfg.particleDistanceCull) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                Vec3 center = particle.getBoundingBox().getCenter();
                double r2 = cfg.particleCullRadius * cfg.particleCullRadius;
                if (mc.player.distanceToSqr(center) > r2) {
                    ci.cancel();
                }
            }
        }
    }
}
