# Ars ReSources

NeoForge 1.21.1 addon for Ars Nouveau. Mod id `resources` (display name "Ars ReSources"), maven group `fun.dungeon.debauchery`, artifact `ars_resources`, package `fun.dungeon.debauchery.arsresources`. 

It adds one block, the **Source Vessel** (`resources:source_vessel`):
- Right-click it with an item to set the template; right-click with an empty hand to take the template back. The template is never consumed.
- It draws source from source jars within `jarRange`.
- Each time it has stored the item's configured cost, it puts one copy into an inventory linked with the Dominion Wand. The copy flies there using Ars Nouveau's `EntityFlyingItem` animation.
- It pauses (and takes no source) when nothing is linked, when the linked inventories are full, or when the item isn't configured.
- It can be dyed when Ars Deco is installed.

## Repository & releases
- Git repo: `git@github.com:legoGonkDroid/ArsReSources.git` (branch `main`, MIT license). The GitHub account was renamed from `jarroyer` to `legoGonkDroid`.
- CurseForge project: https://www.curseforge.com/minecraft/mc-mods/ars-resources
- Release flow:
  1. Bump `mod_version` in `gradle.properties` and commit.
  2. `git tag -a v<version> -m ... && git push origin v<version>`.
  3. `.github/workflows/release.yml` then checks that the tag matches `mod_version`, builds, runs the GameTests, and publishes the jar with `Kir-Antipov/mc-publish` to CurseForge (dependencies: Ars Nouveau required; Ars Deco and EMI optional) and to a GitHub release.
- Required repo settings: the secret `CURSEFORGE_TOKEN` and the variable `CURSEFORGE_PROJECT_ID` (the numeric Project ID).

## Build / test
- `./gradlew build` → `build/libs/ars_resources-1.21.1-<version>.jar`. The wrapper is Gradle 9.8; Java 21 comes from the foojay toolchain resolver.
- `./gradlew runGameTestServer` runs the GameTests in `gametest/SourceVesselTests.java`. They check the copy into a linked chest at the configured rate, the pause when nothing is linked, that unconfigured items are refused, and that Ars Deco registration worked. A passing run logs `All N required tests passed`. Test template: `data/resources/structure/gametest/empty.nbt` (empty 9x5x9).
- `./gradlew runClient` for in-game checks.
- Ars Deco isn't on any maven. Copy its jar into `libs/` (git-ignored) and it goes on the dev runtime classpath through the `localRuntime` configuration.
- To try the mod in the real pack, copy the jar to `../../prism-instances/MagicMod/minecraft/mods/`.

## Code map
- `block/SourceVesselTile`: all the logic.
  - Extends Ars Nouveau's `AbstractSourceMachine` (source storage and sync).
  - Implements `ITickable`, `IWandable` (handles both link orders: `onFirstConnection` when the vessel is clicked first, `onLastConnection` when the inventory is clicked first; sneak = `onClearConnections`), `ITooltipProvider` (Ars Nouveau's look-at HUD; deprecated upstream but still the API), and `Container` (1 slot = the template, so `Containers.dropContents` works).
  - Source comes from `SourceUtil.takeSourceMultipleWithParticles`. That call is all-or-nothing, so `pullSource` first caps the request at what nearby jars hold.
  - It deliberately does **not** register the source capability, so relays never push into or drain the vessel.
- `block/SourceVesselBlock`: extends `TickableModBlock`. Its `useItemOn` passes the Dominion Wand through, so linking works and the wand doesn't become the template.
- `config/ResourcesConfig`: SERVER config `world/serverconfig/resources-server.toml`.
  - `costs` entries are `item=cost` or `#tag=cost`; an exact item wins over tags, and among tags the first match wins.
  - Other settings: `pullIntervalTicks`, `sourcePerPull`, `jarRange`, `linkRange`.
  - `VesselCosts` parses the config on `ModConfigEvent.Loading/Reloading`.
- `mixin/ArsDecoTargetsMixin`: Ars Deco's dyeable-block list is a hard-coded private array (`lambdamagus.ars_deco.dye.DyeableArsBlocks`).
  - The mixin injects at the TAIL of its private static `initialize()` and calls `registerTarget("resources", {"source_vessel"})`.
  - It is `@Pseudo` with `require = 0`: no compile dependency, and it's skipped or harmless without Ars Deco.
  - Everything else comes from Ars Deco: dye right-click, crafting recipe, the `base_color` item component, the colour on drops, sync, and `DyedBakedModel` retexturing.
- `compat/ArsDecoCompat`: reads the dye colour reflectively (`arsDeco$getColor`) to tint the flying-item particles.
- `compat/emi/`: EMI plugin (`@EmiEntrypoint`), compiled against EMI's API only (`compileOnly`), so EMI stays optional. It adds a "Source Vessel" category with the vessel as workstation, and one `EmiVesselRecipe` per item whose `VesselCosts.costFor` is > 0. That expands tags and follows the same item-over-tag precedence the vessel uses. Recipes are sorted by cost and show "Source: N". They are built from the synced server config when EMI reloads (on joining a world), so config edits show after rejoining or reloading EMI.
- `client/SourceVesselRenderer`: floats and spins the template at `SourceVesselTile.ITEM_HEIGHT`.

## Art
- `tools/gen_vessel_art.py` writes `textures/block/source_vessel.png` (64x64, four 16x16 material tiles: stone, brick, gold, source) and `models/block/source_vessel.json`. Regenerate both together. Keep `SourceVesselBlock.SHAPE` in sync with the elements.
- `tools/gen_dyed_textures.py <ars_deco jar> <ars_nouveau jar>` writes `assets/ars_deco/textures/block/source_vessel/<color>.png`.
  - Ars Deco looks for `ars_deco:block/<textureKey>/<color>` and swaps **every** quad's sprite. So the model must use one texture sheet, and the dyed sheets must share its UV layout.
  - The sheet may only use Arcane Pedestal palette colours: the script copies Ars Deco's pedestal colour mapping and fails if it finds an unknown colour.
- Gotchas:
  - Ars Deco's mod version is `1.21.1-5.0.0`, so dependency ranges must be written as `[1.21.1-5.0,)`. A bare `[5.0,)` refuses to load.
  - Ars Nouveau's version is plain `5.13.5`.
