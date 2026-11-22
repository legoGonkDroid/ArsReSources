# Ars ReSources

An [Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau) addon for NeoForge 1.21.1 that adds the **Source Vessel**: give it an item and it makes copies of that item from source.

![Source Vessel](art/curseforge_logo.png)

## How it works
- **Set what to copy:** right-click the vessel with an item. That item becomes the template and is never used up. Right-click with an empty hand to take it back.
- **Source:** the vessel draws source from nearby source jars. Each time it has stored the item's cost, it makes one copy.
- **Output:** link an inventory with the **Dominion Wand** (click the vessel, then the inventory). Copies fly into it the same way items fly into the Scribe's Table. Sneak-click the vessel with the wand to clear all links.
- **Pausing:** the vessel pauses when nothing is linked, the linked inventories are full, or the item can't be copied.
- **Dyeing:** with [Ars Deco](https://www.curseforge.com/minecraft/mc-mods/ars-deco) installed, the vessel can be dyed like other Ars blocks.
- **Recipes:** with [EMI](https://www.curseforge.com/minecraft/mc-mods/emi), a *Source Vessel* tab lists every copyable item and its source cost.

## Configuration
`world/serverconfig/resources-server.toml`:
- `costs`: entries are `"<item>=<source>"` or `"#<item tag>=<source>"`, e.g. `"minecraft:diamond=5000"`, `"#c:ingots/iron=500"`.
- `pullIntervalTicks` and `sourcePerPull`: how fast the vessel absorbs source.
- `jarRange` and `linkRange`: how far it reaches for jars and linked inventories.

## Building
- `./gradlew build` builds the jar into `build/libs/`.
- `./gradlew runGameTestServer` runs the automated in-game tests.
- Pushing a `v<version>` tag publishes the jar to CurseForge and to a GitHub release (see `.github/workflows/release.yml`).

## License
MIT
