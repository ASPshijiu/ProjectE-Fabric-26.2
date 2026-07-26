package moze_intel.projecte.content.menu.slots;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.transmutation.table.TransmutationTransaction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Transmutation-table output slot. Read-only; populated by the menu from
 * {@link moze_intel.projecte.transmutation.table.TransmutationOutputResolver}. Taking a stack
 * spends EMC at the item's value × count through the validated
 * {@link PlayerDataService#tryRemoveEmc} transaction; if the player cannot afford the requested
 * amount the extraction returns nothing.
 */
public class TransmuteOutputSlot extends Slot {
    private final BooleanSupplier serverSide;
    private final PlayerDataService service;
    private final Function<ItemStack, NormalizedStackKey> keyResolver;
    private final Supplier<EmcMappingSnapshot<NormalizedStackKey>> snapshot;

    public TransmuteOutputSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        this(container, index, x, y, () -> !player.level().isClientSide(), service,
              stack -> keyFactory.optionalKey(stack).orElseThrow(
                    () -> new IllegalArgumentException("unregistered item: " + stack)),
              () -> ProjectEEmc.service().current());
    }

    TransmuteOutputSlot(Container container, int index, int x, int y,
          BooleanSupplier serverSide, PlayerDataService service,
          Function<ItemStack, NormalizedStackKey> keyResolver,
          Supplier<EmcMappingSnapshot<NormalizedStackKey>> snapshot) {
        super(container, index, x, y);
        this.serverSide = serverSide;
        this.service = service;
        this.keyResolver = keyResolver;
        this.snapshot = snapshot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    /**
     * 原版 doClick 的 SWAP 分支不经 {@link #remove}，但所有取物路径（PICKUP/SWAP/THROW/
     * PICKUP_ALL）都会调用 mayPickup 与 onTake。因此这里只做"买得起至少一个"的门禁，
     * 实际扣费统一发生在 {@link #onTake}。
     */
    @Override
    public boolean mayPickup(Player player) {
        if (!serverSide.getAsBoolean()) {
            return true;
        }
        ItemStack stored = getItem();
        if (stored.isEmpty()) {
            return false;
        }
        return TransmutationTransaction.quote(
              service, snapshot.get(), keyOf(stored), 1, stored.getMaxStackSize()).success();
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack stored = getItem();
        if (stored.isEmpty() || !serverSide.getAsBoolean()) {
            return super.remove(amount);
        }
        NormalizedStackKey key = keyOf(stored);
        TransmutationTransaction.Outcome outcome = TransmutationTransaction.quote(
              service, snapshot.get(), key, amount, stored.getMaxStackSize());
        if (!outcome.success()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = stored.copy();
        taken.setCount(outcome.producedCount());
        // Output slots are virtual: they always show the resolver candidate, so leave the slot as-is.
        // 此处不扣费：扣费在 onTake 统一进行，避免 SWAP 绕过。
        return taken;
    }

    @Override
    public void onTake(Player player, ItemStack taken) {
        chargeOnTake(taken);
        super.onTake(player, taken);
    }

    /**
     * 对实际取走的堆叠扣费。mayPickup/remove 已按余额限量，正常流程必然成功；
     * 万一失败（同 tick 内余额被其他路径耗尽）则没收物品，保证不会免费取物。
     */
    void chargeOnTake(ItemStack taken) {
        if (!serverSide.getAsBoolean() || taken.isEmpty()) {
            return;
        }
        if (!TransmutationTransaction.charge(service, snapshot.get(), keyOf(taken), taken.getCount())) {
            taken.setCount(0);
        }
    }

    private NormalizedStackKey keyOf(ItemStack stack) {
        return keyResolver.apply(stack);
    }
}
