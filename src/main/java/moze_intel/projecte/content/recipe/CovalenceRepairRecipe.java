package moze_intel.projecte.content.recipe;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.ModRecipeSerializers;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Repairs one damaged item using the EMC value of one or more covalence dusts. */
public final class CovalenceRepairRecipe extends CustomRecipe {
    private static final TagKey<Item> COVALENCE_DUST = TagKey.create(
          Registries.ITEM, ProjectEAPI.id("covalence_dust"));
    private static final CovalenceRepairRecipe INSTANCE = new CovalenceRepairRecipe();
    private static final MapCodec<CovalenceRepairRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    private static final StreamCodec<RegistryFriendlyByteBuf, CovalenceRepairRecipe> STREAM_CODEC =
          StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<CovalenceRepairRecipe> SERIALIZER =
          new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Predicate<ItemStack> isDust;
    private final ToLongFunction<ItemStack> emcValue;

    public CovalenceRepairRecipe() {
        this(stack -> stack.is(COVALENCE_DUST), CovalenceRepairRecipe::baseEmcValue);
    }

    CovalenceRepairRecipe(Predicate<ItemStack> isDust, ToLongFunction<ItemStack> emcValue) {
        this.isDust = isDust;
        this.emcValue = emcValue;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        RepairTarget target = findTarget(input);
        return target != null && target.emcPerDurability() <= target.dustEmc();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        RepairTarget target = findTarget(input);
        if (target == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = target.tool().copy();
        long repaired = target.dustEmc() / target.emcPerDurability();
        result.setDamageValue((int) Math.max(result.getDamageValue() - repaired, 0));
        return result;
    }

    @Override
    public RecipeSerializer<CovalenceRepairRecipe> getSerializer() {
        return ModRecipeSerializers.COVALENCE_REPAIR;
    }

    private RepairTarget findTarget(CraftingInput input) {
        ItemStack tool = ItemStack.EMPTY;
        long dustEmc = 0;
        boolean foundDust = false;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (isDust.test(stack)) {
                foundDust = true;
                dustEmc = saturatingAdd(dustEmc, Math.max(0, emcValue.applyAsLong(stack)));
            } else if (tool.isEmpty() && isRepairableDamagedItem(stack)) {
                tool = stack;
            } else {
                return null;
            }
        }
        if (tool.isEmpty() || !foundDust) {
            return null;
        }

        ItemStack undamaged = tool.copy();
        undamaged.setDamageValue(0);
        long toolEmc = Math.max(0, emcValue.applyAsLong(undamaged));
        long emcPerDurability = Math.max(
              (long) Math.ceil(toolEmc / (double) tool.getMaxDamage()), 1);
        return new RepairTarget(tool, dustEmc, emcPerDurability);
    }

    private static boolean isRepairableDamagedItem(ItemStack stack) {
        return stack.isDamageableItem()
              && stack.has(DataComponents.REPAIRABLE)
              && stack.getDamageValue() > 0;
    }

    private static long baseEmcValue(ItemStack stack) {
        ItemStackKey key = new ItemStackKey(
              BuiltInRegistries.ITEM.getKey(stack.getItem()), Map.of());
        return ProjectEEmc.service().current().valueFor(key)
              .orElse(EmcValue.ZERO)
              .longValue();
    }

    private static long saturatingAdd(long first, long second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private record RepairTarget(ItemStack tool, long dustEmc, long emcPerDurability) {
    }
}
