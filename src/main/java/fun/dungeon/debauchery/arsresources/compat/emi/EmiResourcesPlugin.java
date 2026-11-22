package fun.dungeon.debauchery.arsresources.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import fun.dungeon.debauchery.arsresources.ArsReSources;
import fun.dungeon.debauchery.arsresources.ModRegistry;
import fun.dungeon.debauchery.arsresources.config.VesselCosts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Adds a "Source Vessel" tab to EMI with one recipe per copyable item and its source cost.
 * EMI finds this class through {@link EmiEntrypoint}; it is never loaded when EMI is absent.
 *
 * <p>Recipes come from the synced server config ({@link VesselCosts}), so tags are expanded to their items and
 * each item shows the cost the vessel would really charge (exact item entries win over tags).
 */
@EmiEntrypoint
public class EmiResourcesPlugin implements EmiPlugin {
    public static final EmiStack VESSEL = EmiStack.of(ModRegistry.SOURCE_VESSEL_ITEM.get());
    public static final EmiRecipeCategory CATEGORY =
            new EmiRecipeCategory(ArsReSources.id(ModRegistry.SOURCE_VESSEL_ID), VESSEL);

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, VESSEL);

        record Copyable(Item item, int cost) {
        }
        List<Copyable> copyables = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            int cost = VesselCosts.costFor(new ItemStack(item));
            if (cost > 0) {
                copyables.add(new Copyable(item, cost));
            }
        }
        copyables.sort(Comparator.comparingInt(Copyable::cost)
                .thenComparing(c -> BuiltInRegistries.ITEM.getKey(c.item()).toString()));
        for (Copyable c : copyables) {
            registry.addRecipe(new EmiVesselRecipe(c.item(), c.cost()));
        }
    }
}
