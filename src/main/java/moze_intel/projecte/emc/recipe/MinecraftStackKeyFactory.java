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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MinecraftStackKeyFactory {
    private final Function<DataComponentPatch, Map<String, JsonElement>> componentEncoder;
    private final Function<Map<String, JsonElement>, DataComponentPatch> componentDecoder;

    public MinecraftStackKeyFactory(HolderLookup.Provider registries) {
        Objects.requireNonNull(registries, "registries");
        this.componentEncoder = patch -> encodeComponents(registries, patch);
        this.componentDecoder = components -> decodeComponents(registries, components);
    }

    MinecraftStackKeyFactory(Function<DataComponentPatch, Map<String, JsonElement>> componentEncoder) {
        this(componentEncoder, components -> DataComponentPatch.EMPTY);
    }

    MinecraftStackKeyFactory(
          Function<DataComponentPatch, Map<String, JsonElement>> componentEncoder,
          Function<Map<String, JsonElement>, DataComponentPatch> componentDecoder
    ) {
        this.componentEncoder = Objects.requireNonNull(componentEncoder, "componentEncoder");
        this.componentDecoder = Objects.requireNonNull(componentDecoder, "componentDecoder");
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

    public ItemStack stack(ItemStackKey key) {
        Objects.requireNonNull(key, "key");
        var item = BuiltInRegistries.ITEM.getOptional(key.identifier());
        if (item.isEmpty() || item.get().equals(Items.AIR)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item.get());
        stack.applyComponentsAndValidate(componentDecoder.apply(Map.copyOf(key.components())));
        return stack;
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

    private static DataComponentPatch decodeComponents(
          HolderLookup.Provider registries,
          Map<String, JsonElement> components
    ) {
        JsonObject encoded = new JsonObject();
        components.forEach((name, value) -> encoded.add(name, value.deepCopy()));
        return DataComponentPatch.CODEC
              .parse(RegistryOps.create(JsonOps.INSTANCE, registries), encoded)
              .getOrThrow(IllegalArgumentException::new);
    }
}
