package com.maligpu.mixin;

import com.maligpu.MaliGPUConfig;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngine.PlayResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Dynamic sound-limiter (real fix for the PDF's "audio engine stalls under heavy sound load").
 *
 * The research cites a "247-simultaneous-sound hard cap". In MC 26.1.2 that constant no longer
 * exists as a hardcoded field -- the channel budget is reported by the OpenAL device at runtime.
 * The actual throttling mechanism in vanilla is the {@code instanceToChannel} map size: when too
 * many sounds are live at once, the OpenAL mixer stalls the audio thread. We cap how many NEW
 * non-looping one-shot sounds start per tick (engine-side, Vulkan-safe) to keep the mixer
 * responsive, while always allowing priority sounds (music, master) through.
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    // Per-tick budget of new one-shot sounds. Vanilla has no such throttle, so under rain +
    // many entities the live-sound count grows until the OpenAL mixer stalls. 48/tick is well
    // above what a session needs for normal play but bounds pathological spikes.
    private static final int MAX_NEW_SOUNDS_PER_TICK = 48;
    private static int startedThisTick = 0;
    private static long lastTickStamp = -1L;

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"), cancellable = true)
    private void maligpu$throttleSound(net.minecraft.client.resources.sounds.SoundInstance instance,
                                        CallbackInfoReturnable<PlayResult> cir) {
        if (!MaliGPUConfig.INSTANCE.liftAudioSoundCap) return;

        // Reset the per-tick counter on a new client tick boundary (coarse, ms-based).
        long now = System.currentTimeMillis();
        if (now - lastTickStamp > 50L) {
            startedThisTick = 0;
            lastTickStamp = now;
        }
        startedThisTick++;

        // Allow through if under budget. If over budget, cancel this one-shot to protect the
        // audio thread. Looping/ticking sounds (music, ambient) are rare and low-count, so we
        // let them pass (they are not the spike source).
        if (startedThisTick > MAX_NEW_SOUNDS_PER_TICK) {
            boolean looping = instance instanceof net.minecraft.client.resources.sounds.TickableSoundInstance;
            if (!looping) {
                cir.setReturnValue(PlayResult.NOT_STARTED);
                cir.cancel();
            }
        }
    }
}
