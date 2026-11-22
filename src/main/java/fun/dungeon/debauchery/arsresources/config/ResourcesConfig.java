package fun.dungeon.debauchery.arsresources.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (world/serverconfig/resources-server.toml). NeoForge syncs it to clients,
 * so the Source Vessel's look-at tooltip shows the same costs the server uses.
 */
public final class ResourcesConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> COSTS;
    public static final ModConfigSpec.IntValue PULL_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue SOURCE_PER_PULL;
    public static final ModConfigSpec.IntValue JAR_RANGE;
    public static final ModConfigSpec.IntValue LINK_RANGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("What the Source Vessel can copy, and how much source one copy costs.").push("costs");
        COSTS = b.comment(
                        "Entries are \"<item id>=<source>\" or \"#<item tag>=<source>\".",
                        "An exact item entry wins over a tag entry; among tags the first match in this list wins.",
                        "A source jar holds 10000 source.")
                .defineListAllowEmpty("costs", List.of(
                        "minecraft:dirt=100",
                        "minecraft:cobblestone=100",
                        "minecraft:smooth_stone=200",
                        "minecraft:sand=150",
                        "minecraft:gravel=150",
                        "minecraft:granite=200",
                        "minecraft:diorite=200",
                        "minecraft:andesite=200",
                        "minecraft:deepslate=250",
                        "minecraft:clay_ball=150",
                        "minecraft:clay=600",
                        "minecraft:flint=300",
                        "minecraft:oak_log=400",
                        "minecraft:spruce_log=400",
                        "minecraft:birch_log=400",
                        "minecraft:jungle_log=400",
                        "minecraft:acacia_log=400",
                        "minecraft:dark_oak_log=400",
                        "minecraft:mangrove_log=400",
                        "minecraft:cherry_log=400",
                        "minecraft:crimson_stem=400",
                        "minecraft:warped_stem=400",
                        "ars_nouveau:blue_archwood_log=800",
                        "ars_nouveau:red_archwood_log=800",
                        "ars_nouveau:purple_archwood_log=800",
                        "ars_nouveau:green_archwood_log=800",
                        "ars_nouveau:frostaya_pod=500",
                        "ars_nouveau:bombegranate_pod=500",
                        "ars_nouveau:mendosteen_pod=500",
                        "ars_nouveau:bastion_pod=500",
                        "#c:ingots/copper=2500",
                        "#c:ingots/iron=5000",
                        "#c:ingots/gold=10000",
                        "#c:dusts/redstone=1000",
                        "#c:gems/lapis=1500",
                        "#c:gems/quartz=2000",
                        "#c:gems/amethyst=2000",
                        "minecraft:ender_pearl=7500",
                        "minecraft:blaze_rod=7500",
                        "#c:gems/emerald=40000",
                        "#c:gems/diamond=50000"
                ), () -> "minecraft:item=100", VesselCosts::isValidEntry);
        b.pop();

        b.comment("How fast the vessel absorbs source from nearby source jars.").push("absorption");
        PULL_INTERVAL_TICKS = b.comment("Ticks between pulls (20 ticks = 1 second).")
                .defineInRange("pullIntervalTicks", 10, 1, 1200);
        SOURCE_PER_PULL = b.comment("Maximum source taken from nearby jars per pull.")
                .defineInRange("sourcePerPull", 50, 1, 1_000_000);
        JAR_RANGE = b.comment("Range (in blocks) in which the vessel looks for source jars.")
                .defineInRange("jarRange", 6, 1, 32);
        b.pop();

        b.comment("Linking output inventories with the Dominion Wand.").push("linking");
        LINK_RANGE = b.comment("Maximum distance (in blocks) between the vessel and a linked inventory.")
                .defineInRange("linkRange", 16, 1, 64);
        b.pop();

        SPEC = b.build();
    }

    private ResourcesConfig() {
    }
}
