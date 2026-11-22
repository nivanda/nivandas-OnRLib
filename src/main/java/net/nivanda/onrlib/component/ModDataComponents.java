package net.nivanda.onrlib.component;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nivanda.onrlib.item.Metal;
import net.minecraft.util.Mth;

import static net.nivanda.onrlib.nivandasOnRLib.MODID;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> METAL =
            DATA_COMPONENTS.registerComponentType("metal", builder -> builder
                    // Preserve the legacy float format, but only transport canonical integer IDs.
                    .persistent(Codec.FLOAT.xmap(Metal::normalizeId, Metal::normalizeId))
                    .networkSynchronized(ByteBufCodecs.FLOAT.map(Metal::normalizeId, Metal::normalizeId)));

    public static final int MIN_MELTING_POINT = 1, MAX_MELTING_POINT = 3;
    public static final int MIN_RESISTANCE = 1, MAX_RESISTANCE = 100;
    public static final int MIN_HEAT_TRANSFER_RATE = 0, MAX_HEAT_TRANSFER_RATE = 100;
    public static final int MIN_WEIGHT = 1, MAX_WEIGHT = 100;

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MELTING_POINT =
            intComponent("melting_point", MIN_MELTING_POINT, MAX_MELTING_POINT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_MAGNETIC =
            boolComponent("is_magnetic");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HAS_ARCANE_POWERS =
            boolComponent("has_arcane_powers");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RESISTANCE =
            intComponent("resistance", MIN_RESISTANCE, MAX_RESISTANCE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_CONDUCTIVE =
            boolComponent("is_conductive");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HEAT_TRANSFER_RATE =
            intComponent("heat_transfer_rate", MIN_HEAT_TRANSFER_RATE, MAX_HEAT_TRANSFER_RATE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WEIGHT =
            intComponent("weight", MIN_WEIGHT, MAX_WEIGHT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_RADIOACTIVE =
            boolComponent("is_radioactive");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_STABLE =
            boolComponent("is_stable");

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> intComponent(String name, int min, int max) {
        return DATA_COMPONENTS.registerComponentType(name, builder -> builder
                // Apply identical normalization on encode and decode, not only in the tooltip.
                .persistent(Codec.INT.xmap(value -> Mth.clamp(value, min, max), value -> Mth.clamp(value, min, max)))
                .networkSynchronized(ByteBufCodecs.VAR_INT.map(value -> Mth.clamp(value, min, max), value -> Mth.clamp(value, min, max))));
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> boolComponent(String name) {
        return DATA_COMPONENTS.registerComponentType(name, builder -> builder
                .persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL));
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
