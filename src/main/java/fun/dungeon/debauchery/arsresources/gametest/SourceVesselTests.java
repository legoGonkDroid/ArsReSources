package fun.dungeon.debauchery.arsresources.gametest;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import fun.dungeon.debauchery.arsresources.ArsReSources;
import fun.dungeon.debauchery.arsresources.ModRegistry;
import fun.dungeon.debauchery.arsresources.block.SourceVesselTile;
import fun.dungeon.debauchery.arsresources.config.VesselCosts;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * End-to-end checks for the Source Vessel. Only registered when the game runs with
 * {@code -Dneoforge.enabledGameTestNamespaces=resources} (the {@code gameTestServer} Gradle run).
 */
@GameTestHolder(ArsReSources.MODID)
@PrefixGameTestTemplate(false)
public final class SourceVesselTests {
    private static final String EMPTY = "gametest/empty";
    private static final BlockPos VESSEL = new BlockPos(4, 1, 4);
    private static final BlockPos JAR = new BlockPos(2, 1, 4);
    private static final BlockPos CHEST = new BlockPos(6, 1, 4);

    private SourceVesselTests() {
    }

    /** A vessel holding an iron ingot (default cost 5000) drains a jar and sends one ingot into a linked chest. */
    @GameTest(template = EMPTY, timeoutTicks = 1200)
    public static void copiesIntoLinkedChest(GameTestHelper helper) {
        int cost = VesselCosts.costFor(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(cost > 0, "iron ingot should be copyable by default, cost=" + cost);

        SourceVesselTile vessel = placeVessel(helper);
        SourceJarTile jar = placeJar(helper, 10_000);
        helper.setBlock(CHEST, Blocks.CHEST);
        link(helper, vessel);
        vessel.setTemplate(new ItemStack(Items.IRON_INGOT));

        helper.succeedWhen(() -> {
            ChestBlockEntity chest = helper.getBlockEntity(CHEST);
            helper.assertTrue(chest.countItem(Items.IRON_INGOT) >= 1, "chest has no copied ingot yet");
            helper.assertTrue(jar.getSource() <= 10_000 - cost, "jar should have paid the cost, has " + jar.getSource());
            helper.assertTrue(!vessel.getTemplate().isEmpty(), "template must not be consumed");
            // 5000 source at the default 50 per 10 ticks takes 100 pulls: copying sooner means the rate is ignored.
            helper.assertTrue(helper.getTick() >= 990, "copied too early, at tick " + helper.getTick());
        });
    }

    /** Without a linked inventory the vessel pauses and takes no source. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pausesWithoutLink(GameTestHelper helper) {
        SourceVesselTile vessel = placeVessel(helper);
        SourceJarTile jar = placeJar(helper, 10_000);
        vessel.setTemplate(new ItemStack(Items.IRON_INGOT));

        helper.runAfterDelay(60, () -> {
            helper.assertTrue(vessel.getStatus() == SourceVesselTile.Status.NO_LINK, "status was " + vessel.getStatus());
            helper.assertTrue(jar.getSource() == 10_000, "jar should be untouched, has " + jar.getSource());
            helper.succeed();
        });
    }

    /** Taking the template out returns it and discards the source stored toward the next copy. */
    @GameTest(template = EMPTY)
    public static void removingTemplateResetsSource(GameTestHelper helper) {
        SourceVesselTile vessel = placeVessel(helper);
        vessel.setTemplate(new ItemStack(Items.IRON_INGOT));
        vessel.setSource(300);
        ItemStack removed = vessel.removeItemNoUpdate(0);
        helper.assertTrue(removed.is(Items.IRON_INGOT), "removal should return the template, got " + removed);
        helper.assertTrue(vessel.getSource() == 0, "stored source should reset, has " + vessel.getSource());
        helper.succeed();
    }

    /** Items not in the config can't be copied. */
    @GameTest(template = EMPTY)
    public static void bedrockIsNotCopyable(GameTestHelper helper) {
        helper.assertTrue(VesselCosts.costFor(new ItemStack(Items.BEDROCK)) < 0, "bedrock should not have a cost");
        helper.succeed();
    }

    /** When Ars Deco is installed, the mixin must have added the vessel to its dyeable blocks. */
    @GameTest(template = EMPTY)
    public static void registeredWithArsDeco(GameTestHelper helper) {
        if (!ModList.get().isLoaded("ars_deco")) {
            helper.succeed();
            return;
        }
        try {
            Class<?> blocks = Class.forName("lambdamagus.ars_deco.dye.DyeableArsBlocks");
            Object target = blocks.getMethod("target", net.minecraft.world.level.block.Block.class)
                    .invoke(null, ModRegistry.SOURCE_VESSEL.get());
            helper.assertTrue(target instanceof Optional<?> o && o.isPresent(), "Source Vessel is not an Ars Deco dye target");
        } catch (ReflectiveOperationException e) {
            helper.fail("Could not query Ars Deco: " + e);
        }
        helper.succeed();
    }

    private static SourceVesselTile placeVessel(GameTestHelper helper) {
        helper.setBlock(VESSEL, ModRegistry.SOURCE_VESSEL.get());
        return helper.getBlockEntity(VESSEL);
    }

    private static SourceJarTile placeJar(GameTestHelper helper, int source) {
        helper.setBlock(JAR, BlockRegistry.SOURCE_JAR.get());
        SourceJarTile jar = helper.getBlockEntity(JAR);
        jar.setSource(source);
        return jar;
    }

    private static void link(GameTestHelper helper, SourceVesselTile vessel) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        GlobalPos chest = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(CHEST));
        vessel.onLastConnection(chest, null, null, player);
    }
}
