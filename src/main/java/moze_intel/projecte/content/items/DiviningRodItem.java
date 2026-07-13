package moze_intel.projecte.content.items;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Divining Rod — scans a cube in front of the player and reports the most valuable ores found.
 * The three tiers (low/medium/high) increase the scan radius and the number of ore types reported.
 * Right-click air or a block to perform a scan.
 */
public class DiviningRodItem extends Item {
    private final int range;
    private final int reportCount;

    public DiviningRodItem(Properties properties, int range, int reportCount) {
        super(properties);
        this.range = range;
        this.reportCount = reportCount;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        BlockPos center = player.blockPosition();
        Map<Block, Integer> oreCounts = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(
              center.offset(-range, -range, -range), center.offset(range, range, range))) {
            BlockState state = level.getBlockState(pos);
            if (isOre(state)) {
                oreCounts.merge(state.getBlock(), 1, Integer::sum);
            }
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (oreCounts.isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable("item.projecte.divining_rod.none"));
            } else {
                oreCounts.entrySet().stream()
                      .sorted(Map.Entry.<Block, Integer>comparingByValue().reversed())
                      .limit(reportCount)
                      .forEach(entry -> {
                            String name = BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString();
                            serverPlayer.sendSystemMessage(Component.literal(
                                  entry.getValue() + "x " + name));
                      });
                serverPlayer.sendSystemMessage(Component.translatable("item.projecte.divining_rod.found",
                      oreCounts.values().stream().mapToInt(Integer::intValue).sum()));
            }
        }
        player.getCooldowns().addCooldown(stack, 20);
        return InteractionResult.CONSUME;
    }

    private boolean isOre(BlockState state) {
        // Use the consolidated vanilla ore tags; remaining ore types are matched by name as a fallback.
        if (state.is(BlockTags.IRON_ORES)
              || state.is(BlockTags.GOLD_ORES)
              || state.is(BlockTags.COPPER_ORES)) {
            return true;
        }
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key != null && key.getPath().contains("_ore");
    }
}
