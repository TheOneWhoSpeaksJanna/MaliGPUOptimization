# MaliComplementary Lite (VPFX pack)

A mobile-first Vulkan PostFX pack that gives Minecraft a **Complementary Reimagined**-style look on Mali GPUs, using cheap screen-space post-processing only (no deferred lighting, no shadow maps). Built for playable FPS on Helio G100 / Mali-G57 MC2.

## What it does
- **Highlight bloom** (half-resolution, 9-tap separable blur) — soft glow on bright surfaces
- **ACES filmic tonemapping** — the cinematic contrast curve Complementary uses
- **Warm/cool split-toning** — warm highlights, cool shadows (the signature Complementary daylight feel)
- **Saturation + contrast grade** — punchy, vibrant color
- **Soft vignette** — gentle edge darkening

## What it does NOT do (by design, for performance)
This is **post-processing**, not a full shader pipeline. It does not add:
- Real dynamic shadows / shadow maps
- Volumetric light rays
- Screen-space reflections (SSR)
- Per-pixel PBR lighting

Those are what drop Mali GPUs to single-digit FPS. This pack replicates the *look and mood* of Complementary Reimagined at a fraction of the cost.

## Requirements
- **VulkanMod** (Vulkan render path)
- **Vulkan PostFX (VPFX)** loader mod — this pack requires it to run. Get it from Modrinth: https://modrinth.com/mod/vulkan-postfx
- Minecraft Java (Vulkan-based rendering path)

## Install
1. Install VulkanMod + Vulkan PostFX (VPFX) loader
2. Drop `MaliComplementary-Lite.zip` into your `shaderpacks/` (or the VPFX pack folder the loader specifies)
3. Select it in the VPFX pack menu

## Performance notes (Mali-G57 MC2)
- Bloom targets run at **0.5x resolution** to save fragment bandwidth (TBDR-friendly)
- Total: 1 threshold + 2 blur + 1 composite = 4 fullscreen passes, all cheap
- Target: playable FPS with a Complementary-style look. Tune `bloomStrength`, `exposure`, `sat` in `shaders/post/complementary_composite.fsh` if you want lighter/heavier.

## Tuning
Open `shaders/post/complementary_composite.fsh`:
- `bloomStrength` (0.55) — glow intensity
- `exposure` (1.18) — overall brightness
- `sat` (1.22) — color saturation
- `contrast` (1.08) — punch
- vignette `smoothstep(0.55, 1.30, ...)` — edge darkening

Lower `scale` in `post_effect/main.json` targets (e.g. 0.35) for even more FPS at the cost of bloom sharpness.
