package moze_intel.projecte.emc.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import moze_intel.projecte.emc.ItemStackKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public final class MinecraftStackKeyFactory {
    private final Function<DataComponentPatch, Map<String, JsonElement>> componentEncoder;

    public MinecraftStackKeyFactory(HolderLookup.Provider registries) {
        Objects.requireNonNull(registries, "registries");
        this.componentEncoder = patch -> encodeComponents(registries, patch);
    }

    MinecraftStackKeyFactory(Function<DataComponentPatch, Map<String, JsonElement>> componentEncoder) {
        this.componentEncoder = Objects.requireNonNull(componentEncoder, "componentEncoder");
    }

    public Optional<ItemStackKey> optionalKey(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(key(stack));
    }

    public ItemStackKey key(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("empty ItemStack has no EMC key");
        }
        var itemKey = stack.typeHolder().unwrapKey()
              .orElseThrow(() -> new IllegalArgumentException("unregistered item: " + stack));
        return new ItemStackKey(itemKey.identifier(), componentEncoder.apply(stack.getComponentsPatch()));
    }

    private static Map<String, JsonElement> encodeComponents(HolderLookup.Provider registries, DataComponentPatch patch) {
        JsonElement encoded = DataComponentPatch.CODEC
              .encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), patch)
              .getOrThrow(IllegalArgumentException::new);
        if (!encoded.isJsonObject()) {
            throw new IllegalArgumentException("component patch did not encode to an object: " + encoded);
        }
        JsonObject object = encoded.getAsJsonObject();
        Map<String, JsonElement> components = new HashMap<>();
        object.entrySet().forEach(entry -> components.put(entry.getKey(), entry.getValue()));
        return components;
    }
}
