package fun.dungeon.debauchery.arsresources.mixin;

import fun.dungeon.debauchery.arsresources.ArsReSources;
import fun.dungeon.debauchery.arsresources.ModRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the Source Vessel to Ars Deco's (hard-coded) list of dyeable Ars blocks. Everything else, including
 * dyeing with a right-click, the crafting recipe, keeping the colour on the item, syncing it and retexturing the
 * model, is done by Ars Deco itself. Its retextured sprites are looked up at
 * {@code ars_deco:block/source_vessel/<color>}, which this mod ships.
 *
 * <p>{@link Pseudo} because Ars Deco is optional: without it the target class is missing and the mixin is skipped.
 */
@Pseudo
@Mixin(targets = "lambdamagus.ars_deco.dye.DyeableArsBlocks", remap = false)
public abstract class ArsDecoTargetsMixin {
    @Shadow
    private static void registerTarget(String namespace, String[] target) {
        throw new AssertionError();
    }

    @Inject(method = "initialize", at = @At("TAIL"), require = 0)
    private static void resources$registerSourceVessel(CallbackInfo ci) {
        registerTarget(ArsReSources.MODID, new String[]{ModRegistry.SOURCE_VESSEL_ID});
    }
}
