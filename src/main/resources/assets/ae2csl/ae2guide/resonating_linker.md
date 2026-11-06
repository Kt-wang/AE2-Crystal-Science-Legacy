---
navigation:
  parent: index.md
  title: Resonating Linker
  icon: ae2csl:resonating_linker
  position: 151
item_ids:
  - ae2csl:resonating_linker
---

# Resonating Linker

<ItemImage id="ae2csl:resonating_linker" scale="3" />

The **Resonating Linker** edits the *default input targets* of a
[Resonating Pattern Provider](resonating_pattern_provider.md) in place.

## Usage

1. **Right-click the provider** (block or part) to bind the linker to it.
2. **Right-click any block face** to mark that face as the target of the
   provider's currently selected default input slot. Right-click the same face
   again to clear the mark.
3. **Sneak + scroll** to switch the selected default input slot; the chat HUD
   shows the slot index and its current target.

Default targets are used for every input of a resonating pattern that has no
target of its own, and for plain processing patterns (mapped by input index).

The binding stores dimension, position and the part side, so it keeps working
after reloading the world. Bind a different provider to retarget the linker.

## Recipe

<RecipeFor id="ae2csl:resonating_linker" />
