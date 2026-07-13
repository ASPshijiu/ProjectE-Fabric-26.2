package moze_intel.projecte.content.entity;

import java.util.List;
import moze_intel.projecte.ProjectE;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.items.PhilosophersStoneItem;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Server-side snowball variant used by the Philosopher's Stone. The vanilla snowball entity type
 * keeps client rendering and tracking compatible without registering a custom entity type.
 */
public final class MobRandomizerProjectile extends Snowball {
    private static final TagKey<EntityType<?>> PEACEFUL = TagKey.create(
          Registries.ENTITY_TYPE, ProjectEAPI.id("randomizer/peaceful"));
    private static final TagKey<EntityType<?>> HOSTILE = TagKey.create(
          Registries.ENTITY_TYPE, ProjectEAPI.id("randomizer/hostile"));

    public MobRandomizerProjectile(Level level, LivingEntity owner, ItemStack displayStack) {
        super(level, owner, displayStack);
        setNoGravity(true);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level() instanceof ServerLevel serverLevel
              && result.getEntity() instanceof Mob mob
              && getOwner() instanceof ServerPlayer player) {
            randomize(serverLevel, mob, player);
        }
    }

    private static boolean randomize(ServerLevel level, Mob original, ServerPlayer player) {
        TagKey<EntityType<?>> group = groupFor(original);
        if (group == null) {
            return false;
        }
        List<EntityType<?>> candidates = BuiltInRegistries.ENTITY_TYPE.get(group)
              .stream()
              .flatMap(named -> named.stream())
              .map(Holder::value)
              .filter(type -> type != original.getType())
              .toList();
        if (candidates.isEmpty()) {
            return false;
        }

        EntityType<?> selected = candidates.get(level.getRandom().nextInt(candidates.size()));
        var created = selected.create(level, EntitySpawnReason.CONVERSION);
        if (!(created instanceof Mob replacement)) {
            if (created != null) {
                created.discard();
            }
            ProjectE.LOGGER.warn(
                  "Ignoring non-mob entity type {} in Philosopher's Stone randomizer tag {}",
                  BuiltInRegistries.ENTITY_TYPE.getKey(selected), group.location());
            return false;
        }

        replacement.snapTo(original.position(), original.getYRot(), original.getXRot());
        SpawnGroupData groupData = replacement instanceof Rabbit && group.equals(HOSTILE)
              ? new Rabbit.RabbitGroupData(Rabbit.Variant.EVIL)
              : null;
        replacement.finalizeSpawn(
              level, level.getCurrentDifficultyAt(replacement.blockPosition()),
              EntitySpawnReason.CONVERSION, groupData);

        if (!level.tryAddFreshEntityWithPassengers(replacement)) {
            replacement.discard();
            return false;
        }

        PlayerDataService service = new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player));
        if (!service.tryRemoveEmc(EmcValue.of(PhilosophersStoneItem.MOB_RANDOMIZER_EMC_COST))) {
            replacement.discard();
            return false;
        }
        replacement.spawnAnim();
        original.discard();
        return true;
    }

    private static TagKey<EntityType<?>> groupFor(Mob mob) {
        boolean peaceful = mob.typeHolder().is(PEACEFUL);
        boolean hostile = mob.typeHolder().is(HOSTILE);
        if (peaceful && hostile && mob instanceof Rabbit rabbit
              && rabbit.getVariant() == Rabbit.Variant.EVIL) {
            return HOSTILE;
        }
        if (peaceful) {
            return PEACEFUL;
        }
        return hostile ? HOSTILE : null;
    }
}
