# Mali GPU FPS Research — Reddit + GitHub (1h pass)

Target: **Helio G100 / Mali-G57 MC2**, Minecraft **26.1.2** (via Amethyst/Zalith launcher shim), Fabric.
Companion mod: `MaliGPUOptimization`. Scope: what actually moves FPS on Mali, from real community sources.

## Verified, directly-applicable findings

### 1. There IS a mobile/Mali VulkanMod fork — use it
- **`bludosDevv/VulkanMobile`** (github) — a VulkanMod fork *"specially for mobile"*. Built on Mali G52 fixes:
  - Downgraded Vulkan requirement **1.2 → 1.1** so it runs on mobile via **Zalith / Amethyst Launcher**.
  - Fixed Mali flicker (depth `storeOp`, present-mode MAILBOX/FIFO, proper sync masks).
  - Targets **1.21.10**; releases on the `Alpha` tag.
- TikTok/community confirms **Helio G100 + VulkanMod on Zalith/Amethyst** works (some report settings crashes — see caveats).
- **This is the renderer-level win.** Our companion mod cannot do what this does; recommend users run BOTH: VulkanMobile as the renderer + MaliGPUOptimization for the CPU-side levers.

### 2. The standard Fabric perf stack (community consensus, r/fabricmc + r/ModdedMinecraft)
The "scorched-earth budget-Android" approach (and the 1000-FPS YouTube list) converge on:
- **Sodium** — base renderer optimizations (still applies under OpenGL; under VulkanMod it's absent, so the Vulkan fork covers this instead).
- **Lithium** — entity/logic/physics tick optimizations (big on low-end CPU).
- **FerriteCore** — memory reduction (less GC → fewer stutters; matters on 6GB RAM).
- **EntityCulling / More Culling** — skip rendering hidden entities/blocks (our mod's occlusion culling is the same idea; stack both).
- **ImmediatelyFast** — reduces render-thread overhead.
- **Krypton** — networking optimization (servers).
- **ThreadTweak / C2ME** — thread utilization for chunk work.
- **LazyDFU / Starlight (legacy)** — startup stutter reduction.

> NOTE: under VulkanMod, Sodium/Iris are NOT loaded (Vulkan replaces OpenGL). So on Mali the effective stack is:
> **VulkanMobile (renderer) + Lithium + FerriteCore + EntityCulling + ImmediatelyFast + (Krypton if online) + MaliGPUOptimization (our culling/caps)**.

### 3. Mali-specific truths from r/EmulationOnAndroid + r/winlator
- **Thermal throttling is the real FPS killer**, not raw shader speed. Mali phones drop to 10–15 FPS *over time* as they heat. No mod fixes thermals — a passive cooler / lower render resolution is the only lever.
- **Driver version swings FPS 3–5%**; some Mali driver builds are dramatically worse. Can't be modded.
- **Mali is "not well optimized" for desktop-class workloads** — accept that absolute FPS is lower than Adreno; the goal is *stable, stutter-free*, not max FPS.

## What our companion mod (`MaliGPUOptimization`) should prioritize (honest)
| Lever | Source backing | Status in our mod |
|-------|---------------|------------------|
| Async occlusion culling (GC-free) | EntityCulling community proof | ✅ done (v1.1.0) |
| Particle / animation caps | Reddit "disable animations = +15-25 FPS" | ✅ done |
| Beryl auto-tune | Beryl+Malicompat | ✅ done |
| Debug HUD (see it working) | user asked | ⬜ next |
| ModMenu config screen | usability | ⬜ next |

## Recommended install (Mali, 26.1.2, Amethyst/Zalith)
1. Launcher: **Amethyst** (or Zalith) with the 26.1.2 shim.
2. Renderer: **VulkanMobile** (mobile VulkanMod fork) — NOT vanilla OpenGL, NOT official VulkanMod (needs 1.2).
3. Fabric mods: Lithium + FerriteCore + EntityCulling + ImmediatelyFast + MaliGPUOptimization.
4. Settings: render resolution ≤ device native, clouds off, particles minimal, smooth lighting off.
5. Thermal: passive cooler if available; expect throttling after ~10 min regardless.

## Caveats / things that do NOT help on Mali
- Sodium/Iris under VulkanMod: inert (renderer replaced).
- "1000 FPS" YouTube mod lists: desktop-focused, many inert on mobile, some *reduce* FPS when stacked blindly.
- Our mod alone will not give Nvidium-level gains — that needs the Vulkan renderer (VulkanMobile covers it).
