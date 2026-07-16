package moze_intel.projecte.content.blocks;

import java.util.function.ToLongFunction;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Energy Condenser MK1-2 (102 slots). */
public final class CondenserBlockEntity {
    private CondenserBlockEntity() {}

    public static BlockEntityType<MK1> MK1_TYPE;
    public static BlockEntityType<MK2> MK2_TYPE;

    private static final int SIZE = 102;

    abstract static class Base extends BaseContainerBlockEntity {
        final int tier;
        long storedEmc;
        long requiredEmc;
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ItemStack target = ItemStack.EMPTY;
        MinecraftStackKeyFactory stackKeys;

        Base(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier) {
            super(type, pos, state); this.tier = tier;
        }

        @Override protected Component getDefaultName() { return Component.translatable("container.projecte.condenser_mk" + tier); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> l) { this.items = l; }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return null; } @Override public int getContainerSize() { return SIZE; }

        public long getStoredEmc() { return storedEmc; }
        public void setStoredEmc(long emc) {
            if (storedEmc != emc) {
                storedEmc = emc;
                setChanged();
            }
        }

        public ItemStack getTarget() { return target.copy(); }
        public long getRequiredEmc() { return requiredEmc; }

        public void setTarget(ItemStack stack) {
            ItemStack normalized = stack.isEmpty()
                  ? ItemStack.EMPTY
                  : stack.copyWithCount(1);
            if (ItemStack.matches(target, normalized)) return;

            target = normalized;
            requiredEmc = 0;
            setChanged();
        }

        void refreshTargetEmc(ToLongFunction<ItemStack> emcValue) {
            long refreshed = target.isEmpty()
                  ? 0
                  : Math.max(0, emcValue.applyAsLong(target));
            if (requiredEmc != refreshed) {
                requiredEmc = refreshed;
                setChanged();
            }
        }

        long consumeInput(ToLongFunction<ItemStack> emcValue) {
            if (requiredEmc <= 0) return 0;

            for (int slot = 0; slot < items.size(); slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, target)) {
                    continue;
                }

                long unitValue = emcValue.applyAsLong(stack);
                if (unitValue <= 0) continue;

                int amount = tier == 2 ? stack.getCount() : 1;
                long addedEmc;
                long updatedEmc;
                try {
                    addedEmc = Math.multiplyExact(unitValue, amount);
                    updatedEmc = Math.addExact(storedEmc, addedEmc);
                } catch (ArithmeticException ignored) {
                    return 0;
                }

                stack.shrink(amount);
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
                setStoredEmc(updatedEmc);
                return addedEmc;
            }
            return 0;
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            storedEmc = input.getLongOr("emc", 0);
            target = input.read("target", ItemStack.CODEC)
                  .filter(stack -> !stack.isEmpty())
                  .map(stack -> stack.copyWithCount(1))
                  .orElse(ItemStack.EMPTY);
            requiredEmc = 0;
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("emc", storedEmc);
            if (!target.isEmpty()) {
                output.store("target", ItemStack.CODEC, target);
            }
            ContainerHelper.saveAllItems(output, items);
        }

        static void doTick(Level level, BlockPos pos, BlockState state, Base entity) {
            if (level.isClientSide()) return;
            if (entity.stackKeys == null) {
                entity.stackKeys = new MinecraftStackKeyFactory(level.registryAccess());
            }
            var snapshot = ProjectEEmc.service().current();
            ToLongFunction<ItemStack> emcValue = stack -> entity.stackKeys.optionalKey(stack)
                  .flatMap(snapshot::valueFor)
                  .orElse(EmcValue.ZERO)
                  .longValue();
            entity.refreshTargetEmc(emcValue);
            entity.consumeInput(emcValue);
        }
    }

    public static final class MK1 extends Base {
        public MK1(BlockPos pos, BlockState state) { super(MK1_TYPE, pos, state, 1); }
        public static void tick(Level l, BlockPos p, BlockState s, MK1 e) { doTick(l, p, s, e); }
    }
    public static final class MK2 extends Base {
        public MK2(BlockPos pos, BlockState state) { super(MK2_TYPE, pos, state, 2); }
        public static void tick(Level l, BlockPos p, BlockState s, MK2 e) { doTick(l, p, s, e); }
    }
}
