package moze_intel.projecte.content.items;

import moze_intel.projecte.content.menu.AlchemicalBagMenu;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * Alchemical Bag — a color-coded portable storage container. Each of the 16 dye colors has its own
 * item linked to a 104-slot inventory in the owning player's persistent data. Right-clicking opens
 * the {@link AlchemicalBagMenu}; earlier per-stack container data is migrated without data loss.
 */
public class AlchemicalBagItem extends Item {
    public static final String TITLE_KEY = "container.projecte.alchemical_bag";
    private static final Component TITLE = Component.translatable(TITLE_KEY);
    private final DyeColor color;

    public AlchemicalBagItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    static void repairContents(ItemStack bagStack) {
        ItemContainerContents stored = bagStack.getOrDefault(
              DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        ItemContainerContents repaired = repairContents(stored);
        if (repaired != stored) {
            bagStack.set(DataComponents.CONTAINER, repaired);
        }
    }

    static ItemContainerContents repairContents(ItemContainerContents stored) {
        SimpleContainer contents = new SimpleContainer(AlchemicalBagMenu.BAG_SLOTS);
        stored.copyInto(contents.items);

        boolean hasTalisman = false;
        for (int slot = 0; slot < contents.getContainerSize(); slot++) {
            if (RepairTalismanItem.isTalisman(contents.getItem(slot))) {
                hasTalisman = true;
                break;
            }
        }
        if (!hasTalisman) return stored;

        RepairTalismanItem.tickRepair(contents, true);
        return ItemContainerContents.fromItems(contents.getItems());
    }

    @Override
    public void inventoryTick(
          ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot equipmentSlot
    ) {
        super.inventoryTick(stack, level, entity, equipmentSlot);
        if (entity.tickCount % 20 == 0 && entity instanceof Player player) {
            AlchemicalBagSession session = AlchemicalBagSession.connect(
                  new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player)),
                  color);
            session.repairContents();
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            AlchemicalBagSession session = AlchemicalBagSession.open(
                  new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(serverPlayer)),
                  color, stack);
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return TITLE;
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                    return new AlchemicalBagMenu(containerId, inventory, stack, session);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
