package com.maligpu.mixin;

import com.maligpu.MaliGPUConfig;
import com.maligpu.OcclusionCullingSystem;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("RETURN"), cancellable = true)
    private void maligpu$cullEntity(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!MaliGPUConfig.INSTANCE.asyncOcclusionCulling) return;
        if (!cir.getReturnValueZ()) return;
        if (!OcclusionCullingSystem.INSTANCE.isEntityVisible(entity)) {
            cir.setReturnValue(false);
        }
    }
}
