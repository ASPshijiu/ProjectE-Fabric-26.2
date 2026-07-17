package moze_intel.projecte.emc;

import com.google.gson.JsonElement;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import moze_intel.projecte.content.items.KleinStarItem;
import net.minecraft.world.item.ItemStack;

public final class StackEmcResolver {
    private static final String STORED_EMC_COMPONENT = "projecte:stored_emc";
    private static final Set<String> BASE_FALLBACK_COMPONENTS = Set.of(
          "minecraft:damage",
          "projecte:charge",
          "projecte:philosophers_stone_mode",
          "projecte:tool_mode",
          "projecte:night_vision",
          "projecte:step_assist",
          STORED_EMC_COMPONENT
    );

    private StackEmcResolver() {
    }

    public static Optional<Resolved> resolve(
          ItemStack stack,
          ItemStackKey exactKey,
          EmcMappingSnapshot<NormalizedStackKey> snapshot
    ) {
        Optional<EmcValue> exactValue = snapshot.valueFor(exactKey);
        if (exactValue.isPresent()) {
            return exactValue.map(value -> new Resolved(exactKey, value));
        }
        if (!supportsBaseFallback(exactKey)) {
            return Optional.empty();
        }
        ItemStackKey baseKey = new ItemStackKey(exactKey.identifier(), Map.of());
        return snapshot.valueFor(baseKey)
              .map(value -> new Resolved(baseKey, adjustedBaseValue(stack, exactKey, value)));
    }

    private static boolean supportsBaseFallback(ItemStackKey key) {
        return key.components().keySet().stream()
              .map(StackEmcResolver::withoutRemovalPrefix)
              .allMatch(BASE_FALLBACK_COMPONENTS::contains);
    }

    private static String withoutRemovalPrefix(String component) {
        return component.startsWith("!") ? component.substring(1) : component;
    }

    private static EmcValue adjustedBaseValue(ItemStack stack, ItemStackKey exactKey, EmcValue baseValue) {
        long value = baseValue.longValue();
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            int maxDamage = stack.getMaxDamage();
            int remaining = Math.max(0, maxDamage - stack.getDamageValue());
            value = value / maxDamage * remaining
                  + value % maxDamage * remaining / maxDamage;
        }
        if (stack.getItem() instanceof KleinStarItem) {
            value = Math.addExact(value, storedEmc(exactKey));
        }
        return EmcValue.of(value);
    }

    private static long storedEmc(ItemStackKey key) {
        JsonElement stored = key.components().get(STORED_EMC_COMPONENT);
        if (stored == null || !stored.isJsonPrimitive() || !stored.getAsJsonPrimitive().isNumber()) {
            return 0;
        }
        return Math.max(0, stored.getAsLong());
    }

    public record Resolved(ItemStackKey key, EmcValue value) {
    }
}
