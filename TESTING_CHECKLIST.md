# 📋 LazyRoad RC Regression Testing Checklist

Use this checklist prior to packaging and tagging any new Release Candidate (RC) or stable release build.

---

### 1. In-Game Movement & Road Following (2:1 Snapping)
- [ ] **Curved Road Tracking**: With `/lr straight` disabled (default), walk diagonally across the landscape. Road follows you smoothly (2 blocks forward for 1 block to the side) without stalling or skipping.
- [ ] **Straight Lock**: Run `/lr straight` and verify the road stays locked along a single cardinal axis even if you drift laterally.
- [ ] **90-Degree Turns**: Walk straight, turn 90° left/right, and continue walking. Verify the corner draws cleanly without leaving open holes or corrupted blocks.
- [ ] **Backtracking Protection**: Stop and turn 180° back over the freshly laid road. Verify it does not overwrite or corrupt previously placed blocks.

---

### 2. Terrain Slope & Stair Dynamics
- [ ] **Uphill Incline**: Walk up a hill. Verify stairs spawn facing towards the slope so the player walks up the treads.
- [ ] **Downhill Decline**: Walk down a hill. Verify stairs reverse facing appropriately so the player steps smoothly down the treads.
- [ ] **Cardinal Orientation**: Test uphill and downhill in all 4 cardinal directions (North, South, East, West) to confirm no 90° or 180° rotation inversion.
- [ ] **Max Gradient Rule**: Confirm that roads respect `maxGradient` (stairs do not trigger too frequently or bunch up on gentle slopes).

---

### 3. Block States & Physics
- [ ] **Fences & Walls**: Place templates with fences or wall blocks; verify they automatically connect to each other and neighboring solid blocks on placement.
- [ ] **Directional Blocks**: Verify stairs, logs, glazed terracotta, and directional utility blocks face the expected direction relative to travel.
- [ ] **Upside-Down Stairs & Slabs**: Verify half-height states (`half=top`, `half=bottom`, `type=top`, `type=bottom`) preserve their orientation and height.
- [ ] **Lanterns & Hanging Blocks**: Verify hanging lanterns (`hanging=true`) attach to blocks above rather than floating or falling.

---

### 4. Alternating & Repeating Sequences (OuterWall / Complex Roads)
- [ ] **Odd vs. Even Modulo (OuterWall Check)**: For templates with multiple alternating parts (e.g. Part 1 repeating every 2, Part 2 repeating every 1), verify both parts alternate properly and neither part is skipped.
- [ ] **Long Sequences**: Walk 30+ blocks with a multi-part road to ensure modulo counter cadence stays consistent across chunk boundaries.

---

### 5. Bridges & Pillars
- [ ] **Bridge Altitude Lock**: Run `/lb <road> <pillar>` across a ravine or water body. Verify player altitude locks and blocks generate directly ahead of the player.
- [ ] **Pillar Ground Snapping**: Verify pillar shafts extend all the way down to solid ground (ignoring water/leaves/air).
- [ ] **Pillar Bases**: For templates with base sections, verify the base structures generate from ground level upwards.
- [ ] **Build Max Lim / Arches**: For pillar templates with `buildUntil` / `buildMaxLim` (e.g. Classic5):
  - [ ] Arches stop building downwards at their specified limit.
  - [ ] Empty/zero limits continue downwards to bedrock/ground.

---

### 6. Tunnels & LazyMiner Excavation (/lt, /lm)
- [ ] **Tunnel Clearance**: Run /lt <road> through a mountain. Verify the tunnel bores out the bounding box cleanly with headroom.
- [ ] **Voiding without LazyMiner**: With /lm disabled, excavated tunnel blocks void cleanly (no ground entity clutter or lag).
- [ ] **Deprecated /drops Command**: Run /lr drops or /lt drops and verify it informs you that /drops is consolidated into /lm.
- [ ] **LazyMiner Toggle (/lm)**: Enable LazyMiner with /lm (requires lazyroad.lazyminer permission).
- [ ] **Excavation Collection**: Tunnel or build roads through terrain with /lm enabled; verify excavated blocks are accumulated into virtual storage without dropping loose entities.
- [ ] **Deposit into Chest (/lm store)**:
  - [ ] Run /lm store with no stored items; verify message informs you that you have no stored items to deposit.
  - [ ] Run /lm store while not looking at a chest/container; verify helpful error message.
  - [ ] Look at an empty chest and run /lm store; verify all stored blocks deposit and virtual buffer empties.
  - [ ] Look at a completely full chest and run /lm store; verify warning that chest is full, items remain in buffer, and prompts you to select another chest.
  - [ ] Look at a chest with only a few empty slots; run /lm store; verify it deposits what it can and reports the exact number of remaining stacks.
  - [ ] Look at a second chest and run /lm store again to finish offloading the remaining items.

---

### 7. Core Commands & Plugin Lifecycle
- [ ] **Single Active Jar**: Confirm only one `LazyRoad.jar` is in `plugins/`.
- [ ] **Default Template Preservation**: On startup/update, verify default templates are refreshed without overwriting or deleting custom player templates.
- [ ] **Undo (`/lr undo`)**: Build a section of road, run `/lr undo`, and verify the landscape (including destroyed blocks/terrain) is restored.
- [ ] **Error Handling**: Test a road containing an invalid block string (`minecraft:fake_block`). Verify the server logs a silent warning and keeps running without crashing.

---

### 8. Web Designer (`designer/index.html`)
- [ ] **Unified "Load Template"**: Click the single "Load Template" button and load both a road JSON and a pillar JSON. Confirm each auto-detects mode correctly.
- [ ] **Build Max Lim Round-Trip**: Load a pillar template with `buildUntil` (e.g. `Classic5.json`). Verify the values display in the Build Max Lim row, and remain present when exported.
- [ ] **Stair Facing Round-Trip**: Place upside-down and regular stairs in the designer grid facing various directions. Export JSON, re-import, and verify orientations did not rotate.
- [ ] **Template Type Tag**: Verify newly exported JSONs contain `"type": "road"` or `"type": "pillar"`.

---

### 9. Default Template Visual Verification

#### Road Templates (`/lr <road>`)
- [ ] `/lr Basic`
- [ ] `/lr BigBridge`
- [ ] `/lr DiamondWay`
- [ ] `/lr GlassTunnel`
- [ ] `/lr HighMetro`
- [ ] `/lr OuterWall`
- [ ] `/lr Simple3`
- [ ] `/lr Simple5`
- [ ] `/lr Wall`
- [ ] `/lr Water5`
- [ ] `/lr Wood5`

#### Pillar Templates (`/lb Basic <pillar>`)
- [ ] `/lb Basic Basic`
- [ ] `/lb Basic BigBridge`
- [ ] `/lb Basic Classic5`
- [ ] `/lb Basic Fence5`
- [ ] `/lb Basic Round3`
- [ ] `/lb Basic Wall`
