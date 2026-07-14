package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.entity.MobRandomizerProjectile;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registers ProjectE entity types that require their own client tracking and rendering. */
public final class ModEntityTypes {
    public static final Identifier MOB_RANDOMIZER_ID = ProjectEAPI.id("mob_randomizer");
    public static final ResourceKey<EntityType<?>> MOB_RANDOMIZER_KEY = ResourceKey.create(
          Registries.ENTITY_TYPE, MOB_RANDOMIZER_ID);

    public static EntityType<MobRandomizerProjectile> MOB_RANDOMIZER;

    private ModEntityTypes() {
    }

    public static void init() {
        if (MOB_RANDOMIZER == null) {
            MOB_RANDOMIZER = Registry.register(
                  BuiltInRegistries.ENTITY_TYPE,
                  MOB_RANDOMIZER_KEY,
                  createMobRandomizerType());
        }
    }

    static EntityType<MobRandomizerProjectile> createMobRandomizerType() {
        return EntityType.Builder.<MobRandomizerProjectile>of(
                    MobRandomizerProjectile::new, MobCategory.MISC)
              .noLootTable()
              .sized(0.5F, 0.5F)
              .clientTrackingRange(10)
              .updateInterval(10)
              .build(MOB_RANDOMIZER_KEY);
    }
}
