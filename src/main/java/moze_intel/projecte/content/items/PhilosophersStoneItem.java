package moze_intel.projecte.content.items;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.entity.MobRandomizerProjectile;
import moze_intel.projecte.transmutation.world.SimpleWorldTransmutation;
import moze_intel.projecte.transmutation.world.WorldTransmutationAction;
import moze_intel.projecte.transmutation.world.WorldTransmutationStore;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Philosopher's Stone. Right-clicking a block performs the normal world transmutation;
 * shift-right-clicking performs the alternate result. The conversion is fully server-authoritative
 * through {@link WorldTransmutationAction}.
 */
public class PhilosophersStoneItem extends Item implements IItemCharge, FabricItem {
    public static final long MOB_RANDOMIZER_EMC_COST = 384;

    public PhilosophersStoneItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(net.minecraft.world.item.ItemStack stack) {
        return 4;
    }

    @Override
    public ItemStackTemplate getCraftingRemainder(ItemStack stack) {
        return ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1));
    }

    public Mode getMode(ItemStack stack) {
        return Mode.byId(stack.getOrDefault(ModDataComponents.PHILOSOPHERS_STONE_MODE, 0));
    }

    public Mode cycleMode(Player player, ItemStack stack) {
        Mode next = getMode(stack).next();
        stack.set(ModDataComponents.PHILOSOPHERS_STONE_MODE, next.ordinal());
        player.sendOverlayMessage(Component.translatable(
              "mode.projecte.switch", Component.translatable(next.translationKey())));
        return next;
    }

    public void openPortableCrafting(ServerPlayer player, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
              (containerId, inventory, menuPlayer) -> new CraftingMenu(
                    containerId, inventory,
                    ContainerLevelAccess.create(menuPlayer.level(), menuPlayer.blockPosition())) {
                  @Override
                  public boolean stillValid(Player ignored) {
                      return true;
                  }
              },
              stack.getHoverName()));
    }

    public boolean shootMobRandomizer(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        MobRandomizerProjectile projectile = new MobRandomizerProjectile(
              player.level(), player, new ItemStack(Items.ENDER_PEARL));
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 1.0F);
        if (!player.level().addFreshEntity(projectile)) {
            return false;
        }
        player.getCooldowns().addCooldown(stack, 10);
        player.level().playSound(
              null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
              SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public enum Mode {
        CUBE,
        PANEL,
        LINE;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static Mode byId(int id) {
            return values()[Math.floorMod(id, values().length)];
        }

        public String translationKey() {
            return "mode.projecte.philosopher." + (ordinal() + 1);
        }
    }

    public static List<BlockPos> targetPositions(
          BlockPos center,
          Direction clickedFace,
          Direction horizontalDirection,
          Mode mode,
          int charge
    ) {
        int radius = Math.max(0, Math.min(4, charge));
        List<BlockPos> targets = new ArrayList<>();
        switch (mode) {
            case CUBE -> addBox(targets, center, radius, radius, radius);
            case PANEL -> {
                switch (clickedFace.getAxis()) {
                    case X -> addBox(targets, center, 0, radius, radius);
                    case Y -> addBox(targets, center, radius, 0, radius);
                    case Z -> addBox(targets, center, radius, radius, 0);
                }
            }
            case LINE -> {
                if (horizontalDirection.getAxis() == Direction.Axis.X) {
                    addBox(targets, center, radius, 0, 0);
                } else {
                    addBox(targets, center, 0, 0, radius);
                }
            }
        }
        return List.copyOf(targets);
    }

    private static void addBox(
          List<BlockPos> targets, BlockPos center, int radiusX, int radiusY, int radiusZ
    ) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    targets.add(center.offset(x, y, z));
                }
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return transmute(
              context.getLevel(), player, context.getItemInHand(), context.getClickedPos(),
              context.getClickedFace(), context.getHorizontalDirection(),
              context.isSecondaryUseActive());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        return transmute(
              level, player, player.getItemInHand(hand), hit.getBlockPos(), hit.getDirection(),
              player.getDirection(), true);
    }

    private InteractionResult transmute(
          Level level,
          Player player,
          ItemStack stack,
          BlockPos center,
          Direction clickedFace,
          Direction horizontalDirection,
          boolean alternate
    ) {
        var transmutations = WorldTransmutationStore.current()
              .forOrigin(level.getBlockState(center).getBlock());
        if (transmutations.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        SimpleWorldTransmutation transmutation = transmutations.getFirst();
        int changed = 0;
        for (BlockPos target : targetPositions(
              center, clickedFace, horizontalDirection, getMode(stack), getCharge(stack))) {
            if (!level.isLoaded(target)) {
                continue;
            }
            if (player instanceof ServerPlayer serverPlayer
                  && (!level.mayInteract(serverPlayer, target)
                  || !serverPlayer.mayUseItemAt(target, clickedFace, stack))) {
                continue;
            }
            if (WorldTransmutationAction.apply(level, target, transmutation, alternate)) {
                changed++;
                if (level instanceof ServerLevel serverLevel && level.getRandom().nextInt(8) == 0) {
                    serverLevel.sendParticles(
                          ParticleTypes.LARGE_SMOKE,
                          target.getX() + 0.5, target.getY() + 0.75, target.getZ() + 0.5,
                          2, 0.1, 0.1, 0.1, 0.0);
                }
            }
        }
        if (changed == 0) {
            return InteractionResult.PASS;
        }
        level.playSound(
              null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.8F);
        return InteractionResult.SUCCESS;
    }
}
