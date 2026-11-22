package fun.dungeon.debauchery.arsresources.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import fun.dungeon.debauchery.arsresources.ArsReSources;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * One copyable item: the template (a catalyst, it is never consumed) in the vessel produces one copy
 * for {@code cost} source drawn from nearby source jars.
 */
public class EmiVesselRecipe extends BasicEmiRecipe {
    private final int cost;

    public EmiVesselRecipe(Item item, int cost) {
        super(EmiResourcesPlugin.CATEGORY, idFor(item), 96, 38);
        this.cost = cost;
        EmiStack stack = EmiStack.of(item);
        this.catalysts.add(stack);
        this.outputs.add(stack);
    }

    /** Synthetic EMI ids must start with '/'. */
    private static ResourceLocation idFor(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return ArsReSources.id("/source_vessel/" + key.getNamespace() + "/" + key.getPath());
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(catalysts.get(0), 8, 4).catalyst(true);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 36, 5);
        widgets.addSlot(outputs.get(0), 66, 0).large(true).recipeContext(this);
        widgets.addText(Component.translatable("resources.emi.source_cost", cost), 0, 28, 0x404040, false);
    }
}
