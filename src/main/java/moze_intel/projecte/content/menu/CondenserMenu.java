package moze_intel.projecte.content.menu;

import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.blocks.CondenserBlockEntity;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative Energy Condenser menu for both machine tiers. */
public final class CondenserMenu extends AbstractContainerMenu {
    public static final int MAX_PROGRESS = 102;
    private static final int TARGET_SLOT = 0;
    private static final int MK1_MACHINE_SLOTS = 91;
    private static final int MK2_INPUT_SLOTS = 42;
    private static final int MK2_OUTPUT_SLOTS = 42;
    private static final int TARGET_X = 12;
    private static final int TARGET_Y = 6;
    private static final int PLAYER_X = 48;
    private static final int PLAYER_Y = 154;
    private static final int HOTBAR_Y = 212;

    private final int tier;
    private final int inputSlots;
    private final int machineSlots;
    private final CondenserBlockEntity.Base condenser;
    private final Container machineInventory;
    private final SimpleContainer targetInventory = new SimpleContainer(1);
    private final Predicate<ItemStack> inputValidator;
    private final ToLongFunction<ItemStack> emcValue;
    private final SyncedLong storedEmc;
    private final SyncedLong requiredEmc;

    public CondenserMenu(
          int containerId,
          Inventory playerInventory,
          CondenserBlockEntity.Base condenser
    ) {
        this(menuType(condenser.getTier()), containerId, playerInventory, condenser,
              null, emcLookup(playerInventory.player));
    }

    CondenserMenu(
          MenuType<CondenserMenu> menuType,
          int containerId,
          Inventory playerInventory,
          CondenserBlockEntity.Base condenser,
          Predicate<ItemStack> inputValidator,
          ToLongFunction<ItemStack> emcValue
    ) {
        super(menuType, containerId);
        this.tier = condenser.getTier();
        this.inputSlots = inputSlots(tier);
        this.machineSlots = machineSlots(tier);
        this.condenser = condenser;
        this.machineInventory = condenser;
        this.inputValidator = inputValidator;
        this.emcValue = emcValue;
        this.storedEmc = new SyncedLong(condenser::getStoredEmc);
        this.requiredEmc = new SyncedLong(condenser::getRequiredEmc);
        initialize(playerInventory);
    }

    private CondenserMenu(
          MenuType<CondenserMenu> menuType,
          int containerId,
          Inventory playerInventory,
          int tier
    ) {
        super(menuType, containerId);
        this.tier = tier;
        this.inputSlots = inputSlots(tier);
        this.machineSlots = machineSlots(tier);
        this.condenser = null;
        this.machineInventory = new SimpleContainer(machineSlots);
        this.inputValidator = null;
        this.emcValue = emcLookup(playerInventory.player);
        this.storedEmc = new SyncedLong(() -> 0);
        this.requiredEmc = new SyncedLong(() -> 0);
        initialize(playerInventory);
    }

    public static CondenserMenu clientMk1(int containerId, Inventory playerInventory) {
        return new CondenserMenu(
              ModMenuTypes.CONDENSER_MK1, containerId, playerInventory, 1);
    }

    public static CondenserMenu clientMk2(int containerId, Inventory playerInventory) {
        return new CondenserMenu(
              ModMenuTypes.CONDENSER_MK2, containerId, playerInventory, 2);
    }

    private void initialize(Inventory playerInventory) {
        addDataSlot(storedEmc.lowSlot());
        addDataSlot(storedEmc.highSlot());
        addDataSlot(requiredEmc.lowSlot());
        addDataSlot(requiredEmc.highSlot());
        addTargetSlot();
        addMachineSlots();
        addPlayerInventory(playerInventory);
        refreshTargetSlot();
    }

    private void addTargetSlot() {
        addSlot(new Slot(targetInventory, 0, TARGET_X, TARGET_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isFake() {
                return true;
            }
        });
    }

    private void addMachineSlots() {
        if (tier == 1) {
            for (int row = 0; row < 7; row++) {
                for (int column = 0; column < 13; column++) {
                    int machineSlot = column + row * 13;
                    addSlot(inputSlot(machineSlot, 12 + column * 18, 26 + row * 18));
                }
            }
            return;
        }

        for (int row = 0; row < 7; row++) {
            for (int column = 0; column < 6; column++) {
                int machineSlot = column + row * 6;
                addSlot(inputSlot(machineSlot, 12 + column * 18, 26 + row * 18));
            }
        }
        for (int row = 0; row < 7; row++) {
            for (int column = 0; column < 6; column++) {
                int machineSlot = MK2_INPUT_SLOTS + column + row * 6;
                addSlot(new Slot(machineInventory, machineSlot,
                      138 + column * 18, 26 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
    }

    private Slot inputSlot(int machineSlot, int x, int y) {
        return new Slot(machineInventory, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mayPlaceInput(stack);
            }
        };
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                      PLAYER_X + column * 18, PLAYER_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column,
                  PLAYER_X + column * 18, HOTBAR_Y));
        }
    }

    private boolean mayPlaceInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean hasEmc = inputValidator == null
              ? emcValue.applyAsLong(stack) > 0
              : inputValidator.test(stack);
        ItemStack target = currentTarget();
        return hasEmc && (target.isEmpty()
              || !ItemStack.isSameItemSameComponents(stack, target));
    }

    @Override
    public void clicked(
          int slotId,
          int button,
          ContainerInput input,
          Player player
    ) {
        if (slotId == TARGET_SLOT && condenser != null) {
            updateTargetFromCarried();
            refreshTargetSlot();
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    private void updateTargetFromCarried() {
        ItemStack configured = condenser.getTarget();
        ItemStack carried = getCarried();
        if (!configured.isEmpty() && condenser.getRequiredEmc() > 0) {
            condenser.setTarget(ItemStack.EMPTY);
        } else if (carried.isEmpty()) {
            if (!configured.isEmpty()) {
                condenser.setTarget(ItemStack.EMPTY);
            }
        } else if (emcValue.applyAsLong(carried) > 0) {
            condenser.setTarget(carried);
            condenser.refreshTargetEmc(emcValue);
        }
    }

    private void refreshTargetSlot() {
        if (condenser == null) return;
        ItemStack target = currentTarget();
        if (!ItemStack.matches(targetInventory.getItem(0), target)) {
            targetInventory.setItem(0, target);
        }
    }

    private ItemStack currentTarget() {
        if (condenser == null) {
            return targetInventory.getItem(0);
        }
        return condenser.getRequiredEmc() > 0
              ? condenser.getTarget()
              : ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        refreshTargetSlot();
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index <= TARGET_SLOT || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        int firstPlayerSlot = 1 + machineSlots;
        return MenuQuickMove.move(slot, stack -> index < firstPlayerSlot
              ? moveItemStackTo(stack, firstPlayerSlot, slots.size(), false)
              : moveItemStackTo(stack, 1, 1 + inputSlots, false));
    }

    @Override
    public boolean stillValid(Player player) {
        return condenser == null || Container.stillValidBlockEntity(condenser, player);
    }

    public int tier() {
        return tier;
    }

    public long storedEmc() {
        return condenser == null ? storedEmc.value() : condenser.getStoredEmc();
    }

    public long requiredEmc() {
        return condenser == null ? requiredEmc.value() : condenser.getRequiredEmc();
    }

    public int progressScaled() {
        return progressScaled(storedEmc(), requiredEmc());
    }

    public static int progressScaled(long storedEmc, long requiredEmc) {
        if (requiredEmc <= 0 || storedEmc <= 0) return 0;
        if (storedEmc >= requiredEmc) return MAX_PROGRESS;
        return (int) (MAX_PROGRESS * ((double) storedEmc / requiredEmc));
    }

    private static int inputSlots(int tier) {
        return tier == 1 ? MK1_MACHINE_SLOTS : MK2_INPUT_SLOTS;
    }

    private static int machineSlots(int tier) {
        return tier == 1
              ? MK1_MACHINE_SLOTS
              : MK2_INPUT_SLOTS + MK2_OUTPUT_SLOTS;
    }

    private static MenuType<CondenserMenu> menuType(int tier) {
        return tier == 1
              ? ModMenuTypes.CONDENSER_MK1
              : ModMenuTypes.CONDENSER_MK2;
    }

    private static ToLongFunction<ItemStack> emcLookup(Player player) {
        MinecraftStackKeyFactory stackKeys = new MinecraftStackKeyFactory(
              player.level().registryAccess());
        return stack -> stackKeys.optionalKey(stack)
              .flatMap(ProjectEEmc.service().current()::valueFor)
              .orElse(EmcValue.ZERO)
              .longValue();
    }
}
