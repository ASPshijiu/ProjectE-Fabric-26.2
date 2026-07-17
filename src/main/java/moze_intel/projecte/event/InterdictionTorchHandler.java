package moze_intel.projecte.event;

import java.util.HashSet;
import java.util.Set;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-side hostile-mob repulsion for loaded Interdiction Torches. */
public final class InterdictionTorchHandler {
    static final int RANGE = 8;
    private static final int TICK_INTERVAL = 5;
    private static final double PUSH_STRENGTH = 0.35;
    private static final TagKey<EntityType<?>> BLACKLIST = TagKey.create(
          Registries.ENTITY_TYPE, ProjectEAPI.id("blacklist/interdiction"));
    private static int tickCounter;

    private InterdictionTorchHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % TICK_INTERVAL != 0) {
                return;
            }
            for (ServerLevel level : server.getAllLevels()) {
                repelMobs(level, torchesNearPlayers(level, server.getPlayerList().getPlayers()));
            }
        });
    }

    private static Set<BlockPos> torchesNearPlayers(ServerLevel level, Iterable<ServerPlayer> players) {
        Set<BlockPos> torches = new HashSet<>();
        for (ServerPlayer player : players) {
            if (player.level() != level) {
                continue;
            }
            BlockPos center = player.blockPosition();
            for (BlockPos candidate : BlockPos.betweenClosed(
                  center.offset(-RANGE, -RANGE, -RANGE), center.offset(RANGE, RANGE, RANGE))) {
                if (level.getBlockState(candidate).is(ModBlocks.INTERDICTION_TORCH)) {
                    torches.add(candidate.immutable());
                }
            }
        }
        return torches;
    }

    private static void repelMobs(ServerLevel level, Iterable<BlockPos> torches) {
        for (BlockPos torch : torches) {
            Vec3 origin = Vec3.atCenterOf(torch);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(torch).inflate(RANGE),
                  candidate -> candidate instanceof Enemy && !isBlacklisted(candidate)
                        && candidate.position().distanceToSqr(origin) <= RANGE * RANGE)) {
                Vec3 push = repulsion(origin, mob.position());
                mob.setDeltaMovement(mob.getDeltaMovement().add(push));
                mob.hurtMarked = true;
            }
        }
    }

    private static boolean isBlacklisted(Mob mob) {
        return BuiltInRegistries.ENTITY_TYPE.get(BLACKLIST)
              .map(types -> types.stream().anyMatch(type -> type.value() == mob.getType()))
              .orElse(false);
    }

    static Vec3 repulsion(Vec3 origin, Vec3 target) {
        Vec3 horizontal = target.subtract(origin).multiply(1, 0, 1);
        if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = new Vec3(1, 0, 0);
        }
        return horizontal.normalize().scale(PUSH_STRENGTH).add(0, 0.08, 0);
    }
}
