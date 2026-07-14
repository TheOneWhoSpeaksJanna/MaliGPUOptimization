# MaliGPUOptimization

Mali GPU (TBDR) performance tuning mod for **Minecraft Java Edition 26.1.2 (Fabric)**, built to coexist with **VulkanMod** on Mali-powered mobile devices — specifically the **MediaTek Helio G100 (Mali-G57 MC2, Valhall)**.

It reduces CPU-side cycle overhead and GPU-side memory bandwidth consumption so the SoC stays out of thermal throttling. It does **not** inject any OpenGL — it only optimizes data before it is submitted to the renderer, which is the compatibility boundary VulkanMod requires.

## Why this exists

Mali TBDR GPUs (Bifrost / Valhall / Immortalis) shade only visible fragments via Hidden Surface Removal (HSR). Many Minecraft render paths defeat HSR (alpha cutouts, small immediate-mode draw calls, unbatched indirect draws), forcing the dual shader cores of a G57 MC2 to shade occluded geometry and burn the shared 34 GB/s LPDDR4X budget. This mod attacks the CPU-side and data-submission bottlenecks identified in a 16-page microarchitecture audit of MC 26.1.2 on the Helio G100.

## Features

| Optimization | Mechanism | Report priority |
|--------------|-----------|-----------------|
| Async Occlusion Culling | `ForkJoinPool` line-of-sight raycasts against block occlusion on worker threads; cancels invisible entity rendering via `EntityRenderDispatcher.shouldRender`. | 1 |
| Particle Bandwidth Cap | Caps per-frame particle spawns and distance-culls particles to cut dynamic vertex uploads over the memory bus. | 4 (spirit) |
| Tick Budgeting | Optional client tick throttle to preserve thermal headroom under sustained load. | 6 (spirit) |
| Config file | `config/maligpu.properties` — every toggle tunable, no recompile needed. | — |

## Compatibility

- ✅ **VulkanMod** — compatible (no OpenGL injection; data-only optimization).
- ✅ Lithium, Krypton, FerriteCore, VMP, ImmediatelyFast — all compatible (non-conflicting subsystems).
- ❌ **Sodium / Iris** — explicitly `breaks` in `fabric.mod.json`. These replace the renderer and crash under VulkanMod anyway; do not run them together.
- ❌ Priorities 4 & 5 from the report (Vulkan transient depth attachments, dynamic uniform offsets) require patching VulkanMod's C++ backend and are **out of scope** for a standalone Fabric mod — tracked as upstream work.

## Build

Requires **JDK 25** (MC 26.1.2 runs on Java 25). On a low-RAM host, the `gradle.properties` is already tuned (`-Xmx2560m`, `parallel=false`, `workers.max=1`).

```bash
export JAVA_HOME=/path/to/jdk-25
cd MaliGPUOptimization
./gradlew build --no-daemon
# artifact: build/libs/MaliGPUOptimization-1.0.0.jar
```

## Install

1. Install Fabric Loader `0.19.3` + Fabric API `0.154.2+26.1.2` for MC 26.1.2.
2. Install VulkanMod.
3. Drop `MaliGPUOptimization-1.0.0.jar` into `.minecraft/mods`.
4. Tune `config/maligpu.properties` to your device.

## Contributing

This repo is open for contributions — the Mali ecosystem needs more eyes on real-device profiling. Focus areas:

- **On-device verification of the mixin targets.** The mod compiles against the deobfuscated 26.1.2 mappings, but mixin method descriptors (`EntityRenderDispatcher.shouldRender`, `ParticleEngine.add`) are best-effort and must be confirmed to apply on a real Helio G100 / VulkanMod run. Report mixin failures with logs.
- **Front-to-back opaque sorting** (report Priority 3) — a CPU-side sort pass before VulkanMod dispatches chunk geometry.
- **Extended occlusion grids** — larger bounding volumes, async tile-entity culling, block-entity visibility.
- **Better LOS** — Bresenham vs. raycast sampling trade-offs for the A55 efficiency cores.

Open an issue or PR. Keep the mod source comment-free per project convention; document in the README/PR instead.

## License

LGPL-3.0-only.
