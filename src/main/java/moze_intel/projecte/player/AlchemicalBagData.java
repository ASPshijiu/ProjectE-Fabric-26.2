package moze_intel.projecte.player;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.ItemContainerContents;

/** Player-scoped storage for the sixteen color-linked alchemical bags. */
public final class AlchemicalBagData {
    public static final int SLOTS = 104;
    public static final Codec<AlchemicalBagData> CODEC = Codec.unboundedMap(
          DyeColor.CODEC, ItemContainerContents.CODEC).xmap(
                AlchemicalBagData::new,
                data -> Map.copyOf(data.inventories));
    private static final AlchemicalBagData EMPTY = new AlchemicalBagData(Map.of());
    private final EnumMap<DyeColor, ItemContainerContents> inventories;

    private AlchemicalBagData(Map<DyeColor, ItemContainerContents> inventories) {
        this.inventories = inventories.isEmpty()
              ? new EnumMap<>(DyeColor.class)
              : new EnumMap<>(inventories);
    }

    public static AlchemicalBagData empty() {
        return EMPTY;
    }

    public ItemContainerContents contents(DyeColor color) {
        Objects.requireNonNull(color, "color");
        return inventories.getOrDefault(color, ItemContainerContents.EMPTY);
    }

    public AlchemicalBagData withContents(DyeColor color, ItemContainerContents contents) {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(contents, "contents");
        EnumMap<DyeColor, ItemContainerContents> updated = new EnumMap<>(inventories);
        if (contents.nonEmptyItemCopyStream().findAny().isEmpty()) {
            updated.remove(color);
        } else {
            updated.put(color, contents);
        }
        return updated.isEmpty() ? EMPTY : new AlchemicalBagData(updated);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AlchemicalBagData data
              && inventories.equals(data.inventories);
    }

    @Override
    public int hashCode() {
        return inventories.hashCode();
    }
}
