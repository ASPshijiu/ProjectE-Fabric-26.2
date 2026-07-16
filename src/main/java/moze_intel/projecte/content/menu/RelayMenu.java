package moze_intel.projecte.content.menu;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.blocks.RelayBlockEntity;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative menu shared by all three Anti-Matter Relay tiers. */
public final class RelayMenu extends AbstractContainerMenu {
    private static final int PROGRESS_SCALE = 8_000;

    private final int tier;
    private final int inputSlots;
    private final RelayBlockEntity.Base relay;
    private final Container machineInventory;
    private final Predicate<ItemStack> inputValidator;
    private final Predicate<ItemStack> chargeableValidator;
    private final ToDoubleFunction<ItemStack> chargeProgress;
    private final ToDoubleFunction<ItemStack> burnProgress;
    private final SyncedLong storedEmc;
    private int syncedChargeProgress;
    private int syncedBurnProgress;

    public RelayMenu(
          int containerId,
          Inventory playerInventory,
          RelayBlockEntity.Base relay
    ) {
        this(menuType(relay.getTier()), containerId, playerInventory,
              relay.getTier(), relay, relay, rules(playerInventory.player));
    }

    RelayMenu(
          MenuType<RelayMenu> menuType,
          int containerId,
          Inventory playerInventory,
          RelayBlockEntity.Base relay,
          Predicate<ItemStack> inputValidator,
          Predicate<ItemStack> chargeableValidator,
          ToDoubleFunction<ItemStack> chargeProgress,
          ToDoubleFunction<ItemStack> burnProgress
    ) {
        this(menuType, containerId, playerInventory, relay.getTier(), relay, relay,
              new Rules(inputValidator, chargeableValidator, chargeProgress, burnProgress));
    }

    private RelayMenu(
          MenuType<RelayMenu> menuType,
          int containerId,
          Inventory playerInventory,
          int tier
    ) {
        this(menuType, containerId, playerInventory, tier, null,
              new SimpleContainer(machineSlots(tier)), rules(playerInventory.player));
    }

    private RelayMenu(
          MenuType<RelayMenu> menuType,
          int containerId,
          Inventory playerInventory,
          int tier,
          RelayBlockEntity.Base relay,
          Container machineInventory,
          Rules rules
    ) {
        super(menuType, containerId);
        this.tier = tier;
        this.inputSlots = inputSlots(tier);
        this.relay = relay;
        this.machineInventory = machineInventory;
        this.inputValidator = rules.inputValidator();
        this.chargeableValidator = rules.chargeableValidator();
        this.chargeProgress = rules.chargeProgress();
        this.burnProgress = rules.burnProgress();
        this.storedEmc = new SyncedLong(() -> relay == null ? 0 : relay.getStoredEmc());
        initialize(playerInventory);
    }

    public static RelayMenu clientMk1(int containerId, Inventory playerInventory) {
        return new RelayMenu(ModMenuTypes.RELAY_MK1, containerId, playerInventory, 1);
    }

    public static RelayMenu clientMk2(int containerId, Inventory playerInventory) {
        return new RelayMenu(ModMenuTypes.RELAY_MK2, containerId, playerInventory, 2);
    }

    public static RelayMenu clientMk3(int containerId, Inventory playerInventory) {
        return new RelayMenu(ModMenuTypes.RELAY_MK3, containerId, playerInventory, 3);
    }

    private void initialize(Inventory playerInventory) {
        addDataSlot(storedEmc.lowSlot());
        addDataSlot(storedEmc.highSlot());
        addDataSlot(chargeProgressSlot());
        addDataSlot(burnProgressSlot());
        addMachineSlots();
        addPlayerInventory(playerInventory);
    }

    private DataSlot chargeProgressSlot() {
        return new DataSlot() {
            @Override
            public int get() {
                return relay == null
                      ? syncedChargeProgress
                      : scaled(currentChargeProgress());
            }

            @Override
            public void set(int value) {
                syncedChargeProgress = value;
            }
        };
    }

    private DataSlot burnProgressSlot() {
        return new DataSlot() {
            @Override
            public int get() {
                return relay == null ? syncedBurnProgress : scaled(currentBurnProgress());
            }

            @Override
            public void set(int value) {
                syncedBurnProgress = value;
            }
        };
    }

    private void addMachineSlots() {
        addSlot(validatedSlot(
              inputSlots, chargeX(), chargeY(), chargeableValidator));
        addSlot(validatedSlot(0, burnX(), chargeY(), inputValidator));

        int counter = 1;
        for (int column = columns() - 1; column >= 0; column--) {
            for (int row = rows() - 1; row >= 0; row--) {
                addSlot(validatedSlot(counter++,
                      bufferX() + column * 18,
                      bufferY() + row * 18,
                      inputValidator));
            }
        }
    }

    private Slot validatedSlot(
          int machineSlot,
          int x,
          int y,
          Predicate<ItemStack> validator
    ) {
        return new Slot(machineInventory, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && validator.test(stack);
            }
        };
    }

    private void addPlayerInventory(Inventory inventory) {
        int x = playerX();
        int y = playerY();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                      x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        int firstPlayerSlot = machineSlots(tier);
        return MenuQuickMove.move(slot, stack -> index < firstPlayerSlot
              ? moveItemStackTo(stack, firstPlayerSlot, slots.size(), false)
              : moveItemStackTo(stack, 0, firstPlayerSlot, false));
    }

    @Override
    public boolean stillValid(Player player) {
        return relay == null || Container.stillValidBlockEntity(relay, player);
    }

    public int tier() {
        return tier;
    }

    public long storedEmc() {
        return relay == null ? storedEmc.value() : relay.getStoredEmc();
    }

    public long maximumEmc() {
        if (relay != null) return relay.getMaximumEmc();
        return switch (tier) {
            case 1 -> 100_000;
            case 2 -> 1_000_000;
            case 3 -> 10_000_000;
            default -> throw new IllegalStateException("Unknown relay tier: " + tier);
        };
    }

    public double storageProgress() {
        return fraction(storedEmc(), maximumEmc());
    }

    public double chargeProgress() {
        return relay == null
              ? syncedChargeProgress / (double) PROGRESS_SCALE
              : currentChargeProgress();
    }

    public double burnProgress() {
        return relay == null
              ? syncedBurnProgress / (double) PROGRESS_SCALE
              : currentBurnProgress();
    }

    private double currentChargeProgress() {
        return clampProgress(chargeProgress.applyAsDouble(
              machineInventory.getItem(inputSlots)));
    }

    private double currentBurnProgress() {
        return clampProgress(burnProgress.applyAsDouble(machineInventory.getItem(0)));
    }

    private int chargeX() {
        return switch (tier) {
            case 1 -> 127;
            case 2 -> 144;
            case 3 -> 164;
            default -> throw new IllegalStateException();
        };
    }

    private int burnX() {
        return chargeX() - 60;
    }

    private int chargeY() {
        return switch (tier) {
            case 1 -> 43;
            case 2 -> 44;
            case 3 -> 58;
            default -> throw new IllegalStateException();
        };
    }

    private int columns() {
        return tier + 1;
    }

    private int rows() {
        return tier + 2;
    }

    private int bufferX() {
        return switch (tier) {
            case 1 -> 27;
            case 2 -> 26;
            case 3 -> 28;
            default -> throw new IllegalStateException();
        };
    }

    private int bufferY() {
        return tier == 1 ? 17 : 18;
    }

    private int playerX() {
        return switch (tier) {
            case 1 -> 8;
            case 2 -> 16;
            case 3 -> 26;
            default -> throw new IllegalStateException();
        };
    }

    private int playerY() {
        return switch (tier) {
            case 1 -> 95;
            case 2 -> 101;
            case 3 -> 113;
            default -> throw new IllegalStateException();
        };
    }

    private static int inputSlots(int tier) {
        return switch (tier) {
            case 1 -> 7;
            case 2 -> 13;
            case 3 -> 21;
            default -> throw new IllegalArgumentException("Unknown relay tier: " + tier);
        };
    }

    private static int machineSlots(int tier) {
        return inputSlots(tier) + 1;
    }

    private static int scaled(double progress) {
        return (int) (clampProgress(progress) * PROGRESS_SCALE);
    }

    private static double clampProgress(double progress) {
        return Math.clamp(progress, 0, 1);
    }

    private static double fraction(long value, long maximum) {
        if (value <= 0 || maximum <= 0) return 0;
        if (value >= maximum) return 1;
        return value / (double) maximum;
    }

    private static MenuType<RelayMenu> menuType(int tier) {
        return switch (tier) {
            case 1 -> ModMenuTypes.RELAY_MK1;
            case 2 -> ModMenuTypes.RELAY_MK2;
            case 3 -> ModMenuTypes.RELAY_MK3;
            default -> throw new IllegalArgumentException("Unknown relay tier: " + tier);
        };
    }

    private static Rules rules(Player player) {
        MinecraftStackKeyFactory stackKeys = new MinecraftStackKeyFactory(
              player.level().registryAccess());
        ToLongFunction<ItemStack> emcValue = stack -> stackKeys.optionalKey(stack)
              .flatMap(ProjectEEmc.service().current()::valueFor)
              .orElse(EmcValue.ZERO)
              .longValue();
        return new Rules(
              stack -> RelayBlockEntity.Base.isRelayInput(stack, emcValue),
              RelayBlockEntity.Base::isChargeable,
              RelayMenu::kleinProgress,
              RelayMenu::burnProgress);
    }

    private static double kleinProgress(ItemStack stack) {
        if (!(stack.getItem() instanceof KleinStarItem star)) return 0;
        return fraction(KleinStarItem.getStoredEmc(stack), star.getMaxEmc());
    }

    private static double burnProgress(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof KleinStarItem) return kleinProgress(stack);
        return fraction(stack.getCount(), stack.getMaxStackSize());
    }

    private record Rules(
          Predicate<ItemStack> inputValidator,
          Predicate<ItemStack> chargeableValidator,
          ToDoubleFunction<ItemStack> chargeProgress,
          ToDoubleFunction<ItemStack> burnProgress
    ) {
    }
}
