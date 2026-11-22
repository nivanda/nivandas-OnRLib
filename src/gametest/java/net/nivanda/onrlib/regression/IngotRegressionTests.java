package net.nivanda.onrlib.regression;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nivanda.onrlib.component.ModDataComponents;
import net.nivanda.onrlib.item.Ingot;
import net.nivanda.onrlib.item.Metal;
import net.nivanda.onrlib.item.ModItems;

@GameTestHolder("nv_onrlib")
@PrefixGameTestTemplate(false)
public class IngotRegressionTests {
    static {
        if (Boolean.getBoolean("neoforge.gameTestServer")) {
            try {
                File file = new File(System.getProperty("onrlib.regression.report", "gametest.xml"));
                file.getAbsoluteFile().getParentFile().mkdirs();
                var xml = new JUnitLikeTestReporter(file);
                var log = new LogTestReporter();
                GlobalTestReporter.replaceWith(new TestReporter() {
                    public void onTestFailed(GameTestInfo info) { xml.onTestFailed(info); log.onTestFailed(info); }
                    public void onTestSuccess(GameTestInfo info) { xml.onTestSuccess(info); log.onTestSuccess(info); }
                    public void finish() { xml.finish(); log.finish(); }
                });
            } catch (Exception e) { throw new IllegalStateException("Cannot create regression report", e); }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static ItemStack raw(float metal) {
        ItemStack stack = new ItemStack(ModItems.INGOT.get());
        stack.set(ModDataComponents.METAL.get(), metal);
        return stack;
    }

    private static ItemStack roundTrip(GameTestHelper helper, ItemStack input) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var json = ItemStack.CODEC.encodeStart(ops, input).getOrThrow();
        ItemStack saved = ItemStack.CODEC.parse(ops, json).getOrThrow();
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buf, input);
            ItemStack network = ItemStack.STREAM_CODEC.decode(buf);
            check(ItemStack.isSameItemSameComponents(saved, network), "Save/network disagree: " + json);
            check(saved.getCount() == input.getCount() && network.getCount() == input.getCount(), "Count changed");
        } finally { buf.release(); }
        check(Float.isFinite(Ingot.getMetal(saved)), "Non-finite ID escaped serialization");
        ItemStack normalized = saved.copy();
        Ingot.normalizeComponents(normalized);
        check(ItemStack.isSameItemSameComponents(saved, normalized), "Decoded components were not canonical");
        var tooltip = new ArrayList<Component>();
        saved.getItem().appendHoverText(saved, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        check(tooltip.size() == 9, "Missing tooltip lines");
        return saved;
    }

    @GameTest(template = "empty")
    public static void allHistoricalIdsAndLegacySaves(GameTestHelper helper) throws Exception {
        String[] names;
        try (var reader = new InputStreamReader(IngotRegressionTests.class.getResourceAsStream("/legacy-metals.json"), StandardCharsets.UTF_8)) {
            names = new Gson().fromJson(reader, String[].class);
        }
        check(names.length == 101, "Historical fixture changed");
        for (int id = 0; id < names.length; id++) {
            check(Metal.byId(id).serializedName().equals(names[id]), "Historical ID reassigned: " + id);
            ItemStack created = Ingot.create(Metal.byId(id));
            check(created.getDescriptionId().equals("item.nv_onrlib." + names[id] + "_ingot"), "Name changed: " + id);
            check(ItemStack.isSameItemSameComponents(created, roundTrip(helper, created)), "Valid stack changed");
            // Construct the old numeric JSON representation independently of the new writer.
            JsonObject legacy = new JsonObject();
            legacy.addProperty("id", "nv_onrlib:ingot");
            legacy.addProperty("count", 1);
            JsonObject components = new JsonObject();
            components.addProperty("nv_onrlib:metal", (float) id);
            legacy.add("components", components);
            var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
            ItemStack loaded = ItemStack.CODEC.parse(ops, legacy).getOrThrow();
            check(ItemStack.isSameItemSameComponents(created, loaded), "Legacy save changed material: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanHasIronNameAndFiniteProperty(GameTestHelper helper) {
        ItemStack stack = raw(Float.NaN);
        check(Ingot.getMetal(stack) == 0f, "NaN must render as iron");
        check(Ingot.getMetalName(stack).equals("iron"), "NaN name mismatch");
        check(Float.isNaN(stack.get(ModDataComponents.METAL.get())), "Getter unexpectedly mutated the source");
        check(ItemStack.isSameItemSameComponents(Ingot.create(Metal.IRON), roundTrip(helper, stack)), "NaN recovery not canonical");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fractionalIdentitiesCollapseAndStack(GameTestHelper helper) {
        ItemStack one = Ingot.create(1f), fraction = Ingot.create(1.5f);
        check(ItemStack.isSameItemSameComponents(one, fraction), "Safe factory produces ghost Mithril variant");
        ItemStack legacy = raw(1.5f);
        check(ItemStack.isSameItemSameComponents(one, roundTrip(helper, legacy)), "Fraction survived save/network");
        Ingot.normalizeComponents(legacy);
        check(ItemStack.isSameItemSameComponents(one, legacy), "Explicit repair failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void malformedStatsSaveAndSendDisplayedValues(GameTestHelper helper) {
        ItemStack stack = raw(1.5f);
        stack.set(ModDataComponents.RESISTANCE.get(), Integer.MAX_VALUE);
        stack.set(ModDataComponents.MELTING_POINT.get(), Integer.MIN_VALUE);
        stack.set(ModDataComponents.WEIGHT.get(), -20);
        stack.set(ModDataComponents.HEAT_TRANSFER_RATE.get(), 300);
        check(Ingot.getResistance(stack) == 100, "Display clamp");
        ItemStack loaded = roundTrip(helper, stack);
        check(Ingot.getMetal(loaded) == 1f, "Metal not canonical");
        check(loaded.get(ModDataComponents.RESISTANCE.get()) == 100, "Raw resistance still invalid");
        check(Ingot.getMeltingPoint(loaded) == 1 && Ingot.getWeight(loaded) == 1 && Ingot.getHeatTransferRate(loaded) == 100, "Stat normalization failed");
        check(stack.get(ModDataComponents.RESISTANCE.get()) == Integer.MAX_VALUE, "Encoding mutated the source");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void incomingNetworkValuesAreNormalized(GameTestHelper helper) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            for (float value : new float[]{Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -0f, -1f, 1.5f, 101f}) {
                buf.clear(); buf.writeFloat(value);
                float decoded = ModDataComponents.METAL.get().streamCodec().decode(buf);
                check(Float.isFinite(decoded) && decoded >= 0 && decoded <= 100 && decoded == Math.floor(decoded), "Invalid network metal escaped");
            }
            for (int value : new int[]{Integer.MIN_VALUE, -1, 0, 1, 100, 101, Integer.MAX_VALUE}) {
                buf.clear(); buf.writeVarInt(value);
                int decoded = ModDataComponents.RESISTANCE.get().streamCodec().decode(buf);
                check(decoded == Math.max(1, Math.min(100, value)), "Invalid resistance escaped network boundary");
            }
        } finally { buf.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void defaultRepresentationsCanStack(GameTestHelper helper) {
        ItemStack defaultStack = new ItemStack(ModItems.INGOT.get());
        ItemStack explicit = raw(-0f);
        explicit.set(ModDataComponents.RESISTANCE.get(), 1);
        explicit.set(ModDataComponents.MELTING_POINT.get(), 1);
        explicit.set(ModDataComponents.WEIGHT.get(), 1);
        explicit.set(ModDataComponents.HEAT_TRANSFER_RATE.get(), 0);
        explicit.set(ModDataComponents.IS_MAGNETIC.get(), false);
        Ingot.normalizeComponents(explicit);
        check(ItemStack.isSameItemSameComponents(defaultStack, explicit), "Explicit defaults create ghost stack");
        check(ItemStack.isSameItemSameComponents(defaultStack, Ingot.create(-0f)), "Negative zero ghost stack");
        check(ItemStack.isSameItemSameComponents(defaultStack, roundTrip(helper, explicit)), "Defaults changed on wire/save");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allBooleanCombinations(GameTestHelper helper) {
        for (int bits = 0; bits < 32; bits++) {
            ItemStack stack = Ingot.create(Metal.CHAOSTEEL);
            Ingot.setMagnetic(stack, (bits & 1) != 0);
            Ingot.setConductive(stack, (bits & 2) != 0);
            Ingot.setArcanePowers(stack, (bits & 4) != 0);
            Ingot.setRadioactive(stack, (bits & 8) != 0);
            Ingot.setStable(stack, (bits & 16) != 0);
            ItemStack loaded = roundTrip(helper, stack);
            check(ItemStack.isSameItemSameComponents(stack, loaded), "Flags changed");
            check(Ingot.isMagnetic(loaded) == ((bits & 1) != 0) && Ingot.isConductive(loaded) == ((bits & 2) != 0)
                    && Ingot.hasArcanePowers(loaded) == ((bits & 4) != 0) && Ingot.isRadioactive(loaded) == ((bits & 8) != 0)
                    && Ingot.isStable(loaded) == ((bits & 16) != 0), "Boolean meaning changed");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mutationHelpersClampRawStoredStats(GameTestHelper helper) {
        ItemStack stack = Ingot.create(Metal.MITHRIL);
        Ingot.setResistance(stack, Integer.MAX_VALUE);
        Ingot.setMeltingPoint(stack, 800);
        Ingot.setWeight(stack, 1000);
        Ingot.setHeatTransferRate(stack, -100);
        check(stack.get(ModDataComponents.RESISTANCE.get()) == 100, "Setter only clamped display");
        check(stack.get(ModDataComponents.MELTING_POINT.get()) == 3 && stack.get(ModDataComponents.WEIGHT.get()) == 100, "Setter bounds");
        check(Ingot.getHeatTransferRate(stack) == 0, "Heat lower bound");
        check(ItemStack.isSameItemSameComponents(stack, roundTrip(helper, stack)), "Setter creates unsavable stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copyAndBulkUpdatesNormalizeComponents(GameTestHelper helper) {
        ItemStack malformed = raw(1.5f);
        malformed.set(ModDataComponents.RESISTANCE.get(), Integer.MAX_VALUE);
        ItemStack copied = malformed.copy();
        check(copied.get(ModDataComponents.METAL.get()) == 1f && copied.get(ModDataComponents.RESISTANCE.get()) == 100, "Copy bypassed normalization");
        check(malformed.get(ModDataComponents.METAL.get()) == 1.5f, "Copy mutated source");
        ItemStack bulk = Ingot.create(Metal.MITHRIL);
        bulk.applyComponents(DataComponentPatch.builder().set(ModDataComponents.METAL.get(), Float.NaN)
                .set(ModDataComponents.RESISTANCE.get(), Integer.MAX_VALUE).build());
        check(Ingot.getMetal(bulk) == 0f && !bulk.has(ModDataComponents.METAL.get()), "Bulk update did not normalize NaN/default");
        check(bulk.get(ModDataComponents.RESISTANCE.get()) == 100, "Bulk update did not clamp stored stat");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unrelatedComponentsAndCountSurviveRepair(GameTestHelper helper) {
        ItemStack stack = raw(1.5f);
        stack.setCount(7);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Forged Mithril"));
        stack.set(ModDataComponents.RESISTANCE.get(), Integer.MAX_VALUE);
        Ingot.normalizeComponents(stack);
        ItemStack loaded = roundTrip(helper, stack);
        check(loaded.getCount() == 7 && loaded.getHoverName().getString().equals("Forged Mithril"), "Repair lost unrelated data");
        check(ItemStack.isSameItemSameComponents(stack, loaded), "Repair changed unrelated components");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void tenThousandMalformedStackRoundTrips(GameTestHelper helper) {
        Random random = new Random(20261122);
        long started = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            ItemStack stack = raw(Float.intBitsToFloat(random.nextInt()));
            stack.setCount(1 + random.nextInt(64));
            stack.set(ModDataComponents.RESISTANCE.get(), random.nextInt());
            stack.set(ModDataComponents.MELTING_POINT.get(), random.nextInt());
            stack.set(ModDataComponents.HEAT_TRANSFER_RATE.get(), random.nextInt());
            stack.set(ModDataComponents.WEIGHT.get(), random.nextInt());
            ItemStack loaded = roundTrip(helper, stack);
            check(Ingot.getMetal(loaded) == Ingot.getMetal(stack) && Ingot.getResistance(loaded) == Ingot.getResistance(stack)
                    && Ingot.getMeltingPoint(loaded) == Ingot.getMeltingPoint(stack) && Ingot.getWeight(loaded) == Ingot.getWeight(stack)
                    && Ingot.getHeatTransferRate(loaded) == Ingot.getHeatTransferRate(stack), "Decoded values differ from displayed values");
        }
        System.out.printf("[ONRLIB_REGRESSION] 10000 malformed-stack save/network/tooltip round trips in %.3f s%n", (System.nanoTime() - started) / 1e9);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void hundredThousandFactoryInputs(GameTestHelper helper) {
        Random random = new Random(99);
        for (int i = 0; i < 100000; i++) {
            ItemStack stack = Ingot.create(Float.intBitsToFloat(random.nextInt()));
            float id = Ingot.getMetal(stack);
            check(Float.isFinite(id) && id >= 0 && id <= 100 && id == Math.floor(id), "Factory allowed invalid identity");
            check(ItemStack.isSameItemSameComponents(stack, Ingot.create(Metal.byId((int) id))), "Factory representation not canonical");
        }
        helper.succeed();
    }
}
