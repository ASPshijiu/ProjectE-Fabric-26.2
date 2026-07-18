package moze_intel.projecte.content.menu;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.blocks.CollectorBlockEntity;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.StackEmcResolver;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative menu shared by all three Energy Collector tiers. */
public final class CollectorMenu extends AbstractContainerMenu {
    private static final int PROGRESS_SCALE = 8_000;
    private static final int PLAYER_Y = 84;
    private static final int HOTBAR_Y = 142;

    private final int tier;
    private final int inputSlots;
    private final CollectorBlockEntity.Base collector;
    private final Container machineInventory;
    private final Predicate<ItemStack> inputValidator;
    private final Predicate<ItemStack> lockValidator;
    private final ToLongFunction<ItemStack> emcValue;
    private final Function<ItemStack, ItemStack> nextFuel;
    private final SyncedLong storedEmc;
    private final SyncedLong kleinEmc;
    private int syncedSunLevel;
    private int syncedKleinProgress;
    private int syncedFuelProgress;

    public CollectorMenu(
          int containerId,
          Inventory playerInventory,
          CollectorBlockEntity.Base collector
    ) {
        this(menuType(collector.getTier()), containerId, playerInventory,
              collector.getTier(), collector, collector, rules(playerInventory.player));
    }

    CollectorMenu(
          MenuType<CollectorMenu> menuType,
          int containerId,
          Inventory playerInventory,
          CollectorBlockEntity.Base collector,
          Predicate<ItemStack> inputValidator,
          Predicate<ItemStack> lockValidator,
          ToLongFunction<ItemStack> emcValue,
          Function<ItemStack, ItemStack> nextFuel
    ) {
        this(menuType, containerId, playerInventory, collector.getTier(), collector,
              collector, new Rules(inputValidator, lockValidator, emcValue, nextFuel));
    }

    private CollectorMenu(
          MenuType<CollectorMenu> menuType,
          int containerId,
          Inventory playerInventory,
          int tier
    ) {
        this(menuType, containerId, playerInventory, tier, null,
              new SimpleContainer(machineSlots(tier)), rules(playerInventory.player));
    }

    private CollectorMenu(
          MenuType<CollectorMenu> menuType,
          int containerId,
          Inventory playerInventory,
          int tier,
          CollectorBlockEntity.Base collector,
          Container machineInventory,
          Rules rules
    ) {
        super(menuType, containerId);
        this.tier = tier;
        this.inputSlots = inputSlots(tier);
        this.collector = collector;
        this.machineInventory = machineInventory;
        this.inputValidator = rules.inputValidator();
        this.lockValidator = rules.lockValidator();
        this.emcValue = rules.emcValue();
        this.nextFuel = rules.nextFuel();
        this.storedEmc = new SyncedLong(() -> collector == null ? 0 : collector.getStoredEmc());
        this.kleinEmc = new SyncedLong(this::currentKleinEmc);
        initialize(playerInventory);
    }

    public static CollectorMenu clientMk1(int containerId, Inventory playerInventory) {
        return new CollectorMenu(
              ModMenuTypes.COLLECTOR_MK1, containerId, playerInventory, 1);
    }

    public static CollectorMenu clientMk2(int containerId, Inventory playerInventory) {
        return new CollectorMenu(
              ModMenuTypes.COLLECTOR_MK2, containerId, playerInventory, 2);
    }

    public static CollectorMenu clientMk3(int containerId, Inventory playerInventory) {
        return new CollectorMenu(
              ModMenuTypes.COLLECTOR_MK3, containerId, playerInventory, 3);
    }

    private void initialize(Inventory playerInventory) {
        storedEmc.slots().forEach(this::addDataSlot);
        addDataSlot(sunLevelSlot());
        addDataSlot(kleinProgressSlot());
        addDataSlot(fuelProgressSlot());
        kleinEmc.slots().forEach(this::addDataSlot);
        addMachineSlots();
        addPlayerInventory(playerInventory);
    }

    private DataSlot sunLevelSlot() {
        return new DataSlot() {
            @Override
            public int get() {
                return collector == null ? syncedSunLevel : collector.getSunLevel();
            }

            @Override
            public void set(int value) {
                syncedSunLevel = value;
            }
        };
    }

    private DataSlot kleinProgressSlot() {
        return new DataSlot() {
            @Override
            public int get() {
                return collector == null
                      ? syncedKleinProgress
                      : scaled(currentKleinProgress());
            }

            @Override
            public void set(int value) {
                syncedKleinProgress = value;
            }
        };
    }

    private DataSlot fuelProgressSlot() {
        return new DataSlot() {
            @Override
            public int get() {
                return collector == null ? syncedFuelProgress : scaled(currentFuelProgress());
            }

            @Override
            public void set(int value) {
                syncedFuelProgress = value;
            }
        };
    }

    private void addMachineSlots() {
        addSlot(inputSlot(inputSlots, processingX(), 58));

        int columns = tier + 1;
        int counter = 0;
        int firstX = tier == 1 ? 20 : 18;
        for (int column = columns - 1; column >= 0; column--) {
            for (int row = 3; row >= 0; row--) {
                addSlot(inputSlot(counter++, firstX + column * 18, 8 + row * 18));
            }
        }

        addSlot(new Slot(machineInventory, inputSlots + 1, processingX(), 13) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(machineInventory, inputSlots + 2, lockX(), 36) {
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

    private Slot inputSlot(int machineSlot, int x, int y) {
        return new Slot(machineInventory, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && inputValidator.test(stack);
            }
        };
    }

    private void addPlayerInventory(Inventory inventory) {
        int x = playerX();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                      x + column * 18, PLAYER_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, HOTBAR_Y));
        }
    }

    @Override
    public void clicked(
          int slotId,
          int button,
          ContainerInput input,
          Player player
    ) {
        if (slotId == lockMenuSlot() && collector != null) {
            ItemStack lock = collector.getItem(inputSlots + 2);
            if (!lock.isEmpty()) {
                collector.setItem(inputSlots + 2, ItemStack.EMPTY);
            } else if (!getCarried().isEmpty() && lockValidator.test(getCarried())) {
                collector.setItem(inputSlots + 2, getCarried().copyWithCount(1));
            }
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index == lockMenuSlot()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        int firstPlayerSlot = machineSlots(tier);
        return MenuQuickMove.move(slot, stack -> index < firstPlayerSlot
              ? moveItemStackTo(stack, firstPlayerSlot, slots.size(), false)
              : moveItemStackTo(stack, 0, inputSlots + 1, false));
    }

    @Override
    public boolean stillValid(Player player) {
        return collector == null || Container.stillValidBlockEntity(collector, player);
    }

    public int tier() {
        return tier;
    }

    public long storedEmc() {
        return collector == null ? storedEmc.value() : collector.getStoredEmc();
    }

    public long maximumEmc() {
        if (collector != null) return collector.getMaximumEmc();
        return switch (tier) {
            case 1 -> 10_000;
            case 2 -> 30_000;
            case 3 -> 60_000;
            default -> throw new IllegalStateException("Unknown collector tier: " + tier);
        };
    }

    public int sunLevel() {
        return collector == null ? syncedSunLevel : collector.getSunLevel();
    }

    public long kleinEmc() {
        return collector == null ? kleinEmc.value() : currentKleinEmc();
    }

    public double storageProgress() {
        return fraction(storedEmc(), maximumEmc());
    }

    public double kleinProgress() {
        return collector == null
              ? syncedKleinProgress / (double) PROGRESS_SCALE
              : currentKleinProgress();
    }

    public double fuelProgress() {
        return collector == null
              ? syncedFuelProgress / (double) PROGRESS_SCALE
              : currentFuelProgress();
    }

    private long currentKleinEmc() {
        ItemStack stack = machineInventory.getItem(inputSlots);
        return stack.getItem() instanceof KleinStarItem
              ? KleinStarItem.getStoredEmc(stack)
              : 0;
    }

    private double currentKleinProgress() {
        ItemStack stack = machineInventory.getItem(inputSlots);
        if (!(stack.getItem() instanceof KleinStarItem star)) return 0;
        return fraction(KleinStarItem.getStoredEmc(stack), star.getMaxEmc());
    }

    private double currentFuelProgress() {
        ItemStack upgrading = machineInventory.getItem(inputSlots);
        if (upgrading.isEmpty() || upgrading.getItem() instanceof KleinStarItem) return 0;

        ItemStack standardUpgrade = nextFuel.apply(upgrading);
        if (standardUpgrade.isEmpty()) return 0;

        ItemStack lock = machineInventory.getItem(inputSlots + 2);
        ItemStack result = lock.isEmpty() ? standardUpgrade : lock;

        long required = emcValue.applyAsLong(result) - emcValue.applyAsLong(upgrading);
        if (required < 0) return 0;
        if (required == 0) return 1;
        return fraction(storedEmc(), required);
    }

    private int processingX() {
        return switch (tier) {
            case 1 -> 124;
            case 2 -> 140;
            case 3 -> 158;
            default -> throw new IllegalStateException();
        };
    }

    private int lockX() {
        return processingX() + 29;
    }

    private int playerX() {
        return switch (tier) {
            case 1 -> 8;
            case 2 -> 20;
            case 3 -> 30;
            default -> throw new IllegalStateException();
        };
    }

    private int lockMenuSlot() {
        return inputSlots + 2;
    }

    private static int inputSlots(int tier) {
        return switch (tier) {
            case 1 -> 8;
            case 2 -> 12;
            case 3 -> 16;
            default -> throw new IllegalArgumentException("Unknown collector tier: " + tier);
        };
    }

    private static int machineSlots(int tier) {
        return inputSlots(tier) + 3;
    }

    private static int scaled(double progress) {
        return (int) Math.round(Math.clamp(progress, 0, 1) * PROGRESS_SCALE);
    }

    private static double fraction(long value, long maximum) {
        if (value <= 0 || maximum <= 0) return 0;
        if (value >= maximum) return 1;
        return value / (double) maximum;
    }

    private static MenuType<CollectorMenu> menuType(int tier) {
        return switch (tier) {
            case 1 -> ModMenuTypes.COLLECTOR_MK1;
            case 2 -> ModMenuTypes.COLLECTOR_MK2;
            case 3 -> ModMenuTypes.COLLECTOR_MK3;
            default -> throw new IllegalArgumentException("Unknown collector tier: " + tier);
        };
    }

    private static Rules rules(Player player) {
        MinecraftStackKeyFactory stackKeys = new MinecraftStackKeyFactory(
              player.level().registryAccess());
        ToLongFunction<ItemStack> emcValue = stack -> stackKeys.optionalKey(stack)
              .flatMap(key -> StackEmcResolver.resolve(
                    stack, key, ProjectEEmc.service().current()))
              .map(StackEmcResolver.Resolved::value)
              .orElse(EmcValue.ZERO)
              .longValue();
        return new Rules(
              stack -> CollectorBlockEntity.Base.isCollectorInput(
                    stack, player.level().registryAccess(), emcValue),
              CollectorBlockEntity.Base::isCollectorFuel,
              emcValue,
              stack -> CollectorBlockEntity.Base.nextFuel(
                    stack, player.level().registryAccess(), emcValue));
    }

    private record Rules(
          Predicate<ItemStack> inputValidator,
          Predicate<ItemStack> lockValidator,
          ToLongFunction<ItemStack> emcValue,
          Function<ItemStack, ItemStack> nextFuel
    ) {
    }
}
