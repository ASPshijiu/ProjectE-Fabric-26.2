package moze_intel.projecte.content.blocks;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;

/** Shared Dark Matter and Red Matter furnace timing behavior. */
public final class FastFurnaceBlockEntity {
    private static final int INPUT_SLOT = 0;

    private FastFurnaceBlockEntity() {}

    public static BlockEntityType<DM> DM_TYPE;
    public static BlockEntityType<RM> RM_TYPE;

    static int scaledCookingTime(int recipeCookingTime, int targetCookingTime) {
        if (recipeCookingTime <= 0 || targetCookingTime <= 0) {
            throw new IllegalArgumentException("Cooking times must be positive");
        }
        long scaled = Math.ceilDiv(
              (long) recipeCookingTime * targetCookingTime,
              AbstractFurnaceBlockEntity.BURN_TIME_STANDARD);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, scaled));
    }

    static void applyCookingTime(ContainerData data, int cookingTime) {
        int currentCookingTime = data.get(
              AbstractFurnaceBlockEntity.DATA_COOKING_TOTAL_TIME);
        if (currentCookingTime == cookingTime) return;

        int progress = data.get(AbstractFurnaceBlockEntity.DATA_COOKING_PROGRESS);
        if (progress > 0 && currentCookingTime > 0) {
            long scaledProgress = (long) progress * cookingTime / currentCookingTime;
            data.set(AbstractFurnaceBlockEntity.DATA_COOKING_PROGRESS,
                  (int) Math.min(cookingTime - 1L, scaledProgress));
        }
        data.set(AbstractFurnaceBlockEntity.DATA_COOKING_TOTAL_TIME, cookingTime);
    }

    static int scaledOutputCount(
          int count,
          float oreDoubleChance,
          boolean ore,
          boolean rawMaterial,
          float randomValue
    ) {
        float doubleChance = ore
              ? oreDoubleChance
              : rawMaterial ? oreDoubleChance * 2 / 3 : 0;
        if (randomValue >= doubleChance) return count;
        return (int) Math.min(Integer.MAX_VALUE, 2L * count);
    }

    public static ItemStack scaleOutput(
          AbstractFurnaceBlockEntity furnace, ItemStack result, RandomSource random
    ) {
        if (!(furnace instanceof Base matterFurnace) || result.isEmpty()) return result;

        ItemStack input = furnace.getItem(INPUT_SLOT);
        int scaledCount = scaledOutputCount(
              result.getCount(),
              matterFurnace.oreDoubleChance,
              input.is(ConventionalItemTags.ORES),
              input.is(ConventionalItemTags.RAW_MATERIALS),
              random.nextFloat());
        return scaledCount == result.getCount()
              ? result
              : result.copyWithCount(scaledCount);
    }

    private abstract static class Base extends AbstractFurnaceBlockEntity {
        private final int targetCookingTime;
        private final int efficiencyBonus;
        private final float oreDoubleChance;
        private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes =
              RecipeManager.createCheck(RecipeType.SMELTING);

        Base(
              BlockEntityType<?> type,
              BlockPos pos,
              BlockState state,
              int targetCookingTime,
              int efficiencyBonus,
              float oreDoubleChance
        ) {
            super(type, pos, state, RecipeType.SMELTING);
            this.targetCookingTime = targetCookingTime;
            this.efficiencyBonus = efficiencyBonus;
            this.oreDoubleChance = oreDoubleChance;
        }

        @Override
        protected int getBurnDuration(FuelValues fuelValues, ItemStack stack) {
            int vanillaDuration = super.getBurnDuration(fuelValues, stack);
            if (vanillaDuration <= 0) return 0;

            // 先乘后除：先除会把 vanillaDuration * targetCookingTime < BURN_TIME_STANDARD 的
            // 低燃烧值燃料截断为 0，使其在 DM/RM 熔炉里完全不可用。
            long scaled = (long) vanillaDuration * targetCookingTime * efficiencyBonus
                  / BURN_TIME_STANDARD;
            // 原版认可的燃料至少能烧 1 tick，避免缩放把它变成"不是燃料"。
            return (int) Math.clamp(scaled, 1L, Integer.MAX_VALUE);
        }

        private void prepareCookingTime(ServerLevel level) {
            ItemStack input = getItem(SLOT_INPUT);
            if (input.isEmpty()) return;

            recipes.getRecipeFor(new SingleRecipeInput(input), level)
                  .ifPresent(recipe -> applyCookingTime(dataAccess,
                        scaledCookingTime(recipe.value().cookingTime(), targetCookingTime)));
        }

        static void fastTick(
              ServerLevel level, BlockPos pos, BlockState state, Base furnace
        ) {
            furnace.prepareCookingTime(level);
            AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
        }
    }

    public static final class DM extends Base {
        private static final Component NAME = Component.translatable(
              "container.projecte.dm_furnace");

        public DM(BlockPos pos, BlockState state) {
            super(DM_TYPE, pos, state, SharedConstants.TICKS_PER_SECOND / 2, 3, 0.5F);
        }

        @Override
        protected Component getDefaultName() { return NAME; }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return new FurnaceMenu(id, inventory, this, this.dataAccess);
        }

        public static void fastTick(
              ServerLevel level, BlockPos pos, BlockState state, DM furnace
        ) {
            Base.fastTick(level, pos, state, furnace);
        }
    }

    public static final class RM extends Base {
        private static final Component NAME = Component.translatable(
              "container.projecte.rm_furnace");

        public RM(BlockPos pos, BlockState state) {
            super(RM_TYPE, pos, state, 3, 4, 1.0F);
        }

        @Override
        protected Component getDefaultName() { return NAME; }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return new FurnaceMenu(id, inventory, this, this.dataAccess);
        }

        public static void fastTick(
              ServerLevel level, BlockPos pos, BlockState state, RM furnace
        ) {
            Base.fastTick(level, pos, state, furnace);
        }
    }
}
