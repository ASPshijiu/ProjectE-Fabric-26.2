package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StackEmcResolverTest {
    private static final ItemStackKey BASE = new ItemStackKey(
          net.minecraft.resources.Identifier.withDefaultNamespace("diamond_pickaxe"), Map.of());

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void fallsBackFromProjectEStateComponentsToTheBaseItem() {
        ItemStackKey charged = key(Map.of("projecte:charge", new JsonPrimitive(2)));
        var resolved = resolve(new ItemStack(Items.DIAMOND_PICKAXE), charged, Map.of(BASE, EmcValue.of(1_000)));

        assertEquals(BASE, resolved.key());
        assertEquals(1_000, resolved.value().longValue());
    }

    @Test
    void exactComponentValueTakesPriority() {
        ItemStackKey charged = key(Map.of("projecte:charge", new JsonPrimitive(2)));
        var resolved = resolve(new ItemStack(Items.DIAMOND_PICKAXE), charged, Map.of(
              BASE, EmcValue.of(1_000), charged, EmcValue.of(2_000)));

        assertEquals(charged, resolved.key());
        assertEquals(2_000, resolved.value().longValue());
    }

    @Test
    void unknownComponentsDoNotUseTheBaseValue() {
        ItemStackKey named = key(Map.of("minecraft:custom_name", new JsonPrimitive("valuable")));

        assertTrue(StackEmcResolver.resolve(
              new ItemStack(Items.DIAMOND_PICKAXE), named,
              snapshot(Map.of(BASE, EmcValue.of(1_000)))).isEmpty());
    }

    @Test
    void damagedItemsSellForTheirRemainingDurability() {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.set(DataComponents.MAX_DAMAGE, 100);
        stack.setDamageValue(50);
        ItemStackKey damaged = key(Map.of("minecraft:damage", new JsonPrimitive(stack.getDamageValue())));
        long expected = 1_000L * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();

        assertEquals(expected, resolve(stack, damaged, Map.of(BASE, EmcValue.of(1_000))).value().longValue());
    }

    @Test
    void kleinStarStoredEmcIsReturnedInsteadOfDiscarded() throws Exception {
        KleinStarItem star = allocateKleinStar();
        ItemStack stack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));
        ItemStackKey base = new ItemStackKey(
              net.minecraft.resources.Identifier.fromNamespaceAndPath("projecte", "klein_star_ein"), Map.of());
        ItemStackKey charged = new ItemStackKey(base.identifier(), Map.of(
              "projecte:stored_emc", new JsonPrimitive(250)));

        var resolved = resolve(stack, charged, Map.of(base, EmcValue.of(1_000)));

        assertEquals(base, resolved.key());
        assertEquals(1_250, resolved.value().longValue());
    }

    private static StackEmcResolver.Resolved resolve(
          ItemStack stack, ItemStackKey key, Map<NormalizedStackKey, EmcValue> values
    ) {
        return StackEmcResolver.resolve(stack, key, snapshot(values)).orElseThrow();
    }

    private static EmcMappingSnapshot<NormalizedStackKey> snapshot(
          Map<NormalizedStackKey, EmcValue> values
    ) {
        return new EmcMappingSnapshot<>(1, values);
    }

    private static ItemStackKey key(Map<String, JsonPrimitive> components) {
        return new ItemStackKey(BASE.identifier(), components);
    }

    private static KleinStarItem allocateKleinStar() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (KleinStarItem) allocateInstance.invoke(unsafe, KleinStarItem.class);
    }
}
