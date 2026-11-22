package fun.dungeon.debauchery.arsresources.config;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/** Parsed form of {@link ResourcesConfig#COSTS}. */
public final class VesselCosts {
    private static final Logger LOGGER = LogUtils.getLogger();

    private record TagCost(TagKey<Item> tag, int cost) {
    }

    private static volatile Map<Item, Integer> itemCosts = Map.of();
    private static volatile List<TagCost> tagCosts = List.of();

    private VesselCosts() {
    }

    /** @return the source cost of one copy of {@code stack}, or -1 if it can't be copied. */
    public static int costFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        Integer exact = itemCosts.get(stack.getItem());
        if (exact != null) {
            return exact;
        }
        for (TagCost tagCost : tagCosts) {
            if (stack.is(tagCost.tag())) {
                return tagCost.cost();
            }
        }
        return -1;
    }

    public static void reload() {
        Map<Item, Integer> items = new HashMap<>();
        List<TagCost> tags = new ArrayList<>();
        for (String entry : ResourcesConfig.COSTS.get()) {
            int eq = entry.lastIndexOf('=');
            if (eq <= 0) {
                LOGGER.warn("Ignoring Source Vessel cost entry '{}': expected <item or #tag>=<source>", entry);
                continue;
            }
            String key = entry.substring(0, eq).trim();
            int cost;
            try {
                cost = Integer.parseInt(entry.substring(eq + 1).trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Ignoring Source Vessel cost entry '{}': cost is not a number", entry);
                continue;
            }
            if (cost <= 0) {
                LOGGER.warn("Ignoring Source Vessel cost entry '{}': cost must be positive", entry);
                continue;
            }
            boolean isTag = key.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? key.substring(1) : key);
            if (id == null) {
                LOGGER.warn("Ignoring Source Vessel cost entry '{}': invalid id", entry);
                continue;
            }
            if (isTag) {
                tags.add(new TagCost(TagKey.create(Registries.ITEM, id), cost));
            } else {
                var item = BuiltInRegistries.ITEM.getOptional(id);
                if (item.isEmpty()) {
                    LOGGER.warn("Ignoring Source Vessel cost entry '{}': unknown item", entry);
                    continue;
                }
                items.putIfAbsent(item.get(), cost);
            }
        }
        itemCosts = Map.copyOf(items);
        tagCosts = List.copyOf(tags);
        LOGGER.info("Source Vessel: loaded {} item and {} tag costs", items.size(), tags.size());
    }

    static boolean isValidEntry(Object o) {
        return o instanceof String s && s.indexOf('=') > 0;
    }
}
