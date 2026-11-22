package fun.dungeon.debauchery.arsresources.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/**
 * Reads the dye colour Ars Deco stores on every block entity, without a compile-time dependency on Ars Deco.
 * Ars Deco mixes {@code lambdamagus.ars_deco.dye.ArsDecoDyeableBlockEntity} into {@link BlockEntity}.
 */
public final class ArsDecoCompat {
    private static final String DYEABLE_INTERFACE = "lambdamagus.ars_deco.dye.ArsDecoDyeableBlockEntity";
    private static Method getColor;
    private static boolean resolved;

    private ArsDecoCompat() {
    }

    public static Optional<DyeColor> dyeColor(BlockEntity blockEntity) {
        Method method = resolve();
        if (method == null || !method.getDeclaringClass().isInstance(blockEntity)) {
            return Optional.empty();
        }
        try {
            Object result = method.invoke(blockEntity);
            if (result instanceof Optional<?> optional && optional.orElse(null) instanceof DyeColor color) {
                return Optional.of(color);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Ars Deco changed its API: fall back to the default colour.
        }
        return Optional.empty();
    }

    private static synchronized Method resolve() {
        if (!resolved) {
            resolved = true;
            if (ModList.get().isLoaded("ars_deco")) {
                try {
                    getColor = Class.forName(DYEABLE_INTERFACE).getMethod("arsDeco$getColor");
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    getColor = null;
                }
            }
        }
        return getColor;
    }
}
