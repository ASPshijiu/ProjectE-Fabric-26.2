package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.blocks.TransmutationTableBlock;
import moze_intel.projecte.content.blocks.TransmutationTableBlockEntity;
import moze_intel.projecte.content.blocks.FastFurnaceBlockEntity;
import moze_intel.projecte.content.blocks.DMFurnaceBlock;
import moze_intel.projecte.content.blocks.RMFurnaceBlock;
import moze_intel.projecte.content.blocks.InterdictionTorchBlock;
import moze_intel.projecte.content.blocks.NovaCatalystBlock;
import moze_intel.projecte.content.blocks.NovaCataclysmBlock;
import moze_intel.projecte.content.blocks.AlchemicalChestBlock;
import moze_intel.projecte.content.blocks.AlchemicalChestBlockEntity;
import moze_intel.projecte.content.blocks.CollectorBlock;
import moze_intel.projecte.content.blocks.CollectorBlockEntity;
import moze_intel.projecte.content.blocks.CondenserBlock;
import moze_intel.projecte.content.blocks.CondenserBlockEntity;
import moze_intel.projecte.content.blocks.PedestalBlock;
import moze_intel.projecte.content.blocks.PedestalBlockEntity;
import moze_intel.projecte.content.blocks.RelayBlock;
import moze_intel.projecte.content.blocks.RelayBlockEntity;
import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registration hub for all ProjectE blocks and block entities.
 */
public final class ModBlocks {
    // --- Identifiers ---
    public static final Identifier TRANSMUTATION_TABLE_ID = ProjectEAPI.id("transmutation_table");
    public static final Identifier ALCHEMICAL_COAL_BLOCK_ID = ProjectEAPI.id("alchemical_coal_block");
    public static final Identifier MOBIUS_FUEL_BLOCK_ID = ProjectEAPI.id("mobius_fuel_block");
    public static final Identifier AETERNALIS_FUEL_BLOCK_ID = ProjectEAPI.id("aeternalis_fuel_block");
    public static final Identifier DARK_MATTER_BLOCK_ID = ProjectEAPI.id("dark_matter_block");
    public static final Identifier RED_MATTER_BLOCK_ID = ProjectEAPI.id("red_matter_block");
    public static final Identifier ALCHEMICAL_CHEST_ID = ProjectEAPI.id("alchemical_chest");
    public static final Identifier DM_FURNACE_ID = ProjectEAPI.id("dm_furnace");
    public static final Identifier RM_FURNACE_ID = ProjectEAPI.id("rm_furnace");
    public static final Identifier DM_PEDESTAL_ID = ProjectEAPI.id("dm_pedestal");
    public static final Identifier COLLECTOR_MK1_ID = ProjectEAPI.id("collector_mk1");
    public static final Identifier COLLECTOR_MK2_ID = ProjectEAPI.id("collector_mk2");
    public static final Identifier COLLECTOR_MK3_ID = ProjectEAPI.id("collector_mk3");
    public static final Identifier RELAY_MK1_ID = ProjectEAPI.id("relay_mk1");
    public static final Identifier RELAY_MK2_ID = ProjectEAPI.id("relay_mk2");
    public static final Identifier RELAY_MK3_ID = ProjectEAPI.id("relay_mk3");
    public static final Identifier CONDENSER_MK1_ID = ProjectEAPI.id("condenser_mk1");
    public static final Identifier CONDENSER_MK2_ID = ProjectEAPI.id("condenser_mk2");
    public static final Identifier INTERDICTION_TORCH_ID = ProjectEAPI.id("interdiction_torch");
    public static final Identifier NOVA_CATALYST_ID = ProjectEAPI.id("nova_catalyst");
    public static final Identifier NOVA_CATACLYSM_ID = ProjectEAPI.id("nova_cataclysm");

    // --- Block instances ---
    public static Block TRANSMUTATION_TABLE;
    public static BlockItem TRANSMUTATION_TABLE_ITEM;
    public static BlockEntityType<TransmutationTableBlockEntity> TRANSMUTATION_TABLE_ENTITY;

    public static BlockEntityType<FastFurnaceBlockEntity.DM> DM_FURNACE_ENTITY;
    public static BlockEntityType<FastFurnaceBlockEntity.RM> RM_FURNACE_ENTITY;

    public static Block ALCHEMICAL_COAL_BLOCK;
    public static BlockItem ALCHEMICAL_COAL_BLOCK_ITEM;
    public static Block MOBIUS_FUEL_BLOCK;
    public static BlockItem MOBIUS_FUEL_BLOCK_ITEM;
    public static Block AETERNALIS_FUEL_BLOCK;
    public static BlockItem AETERNALIS_FUEL_BLOCK_ITEM;
    public static Block DARK_MATTER_BLOCK;
    public static BlockItem DARK_MATTER_BLOCK_ITEM;
    public static Block RED_MATTER_BLOCK;
    public static BlockItem RED_MATTER_BLOCK_ITEM;
    public static Block ALCHEMICAL_CHEST;
    public static BlockItem ALCHEMICAL_CHEST_ITEM;
    public static BlockEntityType<AlchemicalChestBlockEntity> ALCHEMICAL_CHEST_ENTITY;

    public static Block DM_FURNACE;
    public static BlockItem DM_FURNACE_ITEM;
    public static Block RM_FURNACE;
    public static BlockItem RM_FURNACE_ITEM;
    public static Block DM_PEDESTAL;
    public static BlockItem DM_PEDESTAL_ITEM;
    public static Block COLLECTOR_MK1;
    public static BlockItem COLLECTOR_MK1_ITEM;
    public static Block COLLECTOR_MK2;
    public static BlockItem COLLECTOR_MK2_ITEM;
    public static Block COLLECTOR_MK3;
    public static BlockItem COLLECTOR_MK3_ITEM;
    public static Block RELAY_MK1;
    public static BlockItem RELAY_MK1_ITEM;
    public static Block RELAY_MK2;
    public static BlockItem RELAY_MK2_ITEM;
    public static Block RELAY_MK3;
    public static BlockItem RELAY_MK3_ITEM;
    public static Block CONDENSER_MK1;
    public static BlockItem CONDENSER_MK1_ITEM;
    public static Block CONDENSER_MK2;
    public static BlockItem CONDENSER_MK2_ITEM;
    public static Block INTERDICTION_TORCH;
    public static BlockItem INTERDICTION_TORCH_ITEM;
    public static Block NOVA_CATALYST;
    public static BlockItem NOVA_CATALYST_ITEM;
    public static Block NOVA_CATACLYSM;
    public static BlockItem NOVA_CATACLYSM_ITEM;

    private static volatile boolean initialized;

    private ModBlocks() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Transmutation table
        TRANSMUTATION_TABLE = registerBlock(TRANSMUTATION_TABLE_ID,
              new TransmutationTableBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, TRANSMUTATION_TABLE_ID))
                    .strength(10.0F, 30.0F)
                    .sound(SoundType.STONE)));
        TRANSMUTATION_TABLE_ITEM = registerBlockItem(TRANSMUTATION_TABLE_ID, TRANSMUTATION_TABLE);
        TRANSMUTATION_TABLE_ENTITY = registerBlockEntity(TRANSMUTATION_TABLE_ID,
              new BlockEntityType<>(TransmutationTableBlockEntity::new, java.util.Set.of(TRANSMUTATION_TABLE)));

        // Fuel blocks
        ALCHEMICAL_COAL_BLOCK = registerSimpleBlock(ALCHEMICAL_COAL_BLOCK_ID, 0.5F, 1.5F);
        ALCHEMICAL_COAL_BLOCK_ITEM = registerBlockItem(ALCHEMICAL_COAL_BLOCK_ID, ALCHEMICAL_COAL_BLOCK);

        MOBIUS_FUEL_BLOCK = registerSimpleBlock(MOBIUS_FUEL_BLOCK_ID, 0.5F, 1.5F);
        MOBIUS_FUEL_BLOCK_ITEM = registerBlockItem(MOBIUS_FUEL_BLOCK_ID, MOBIUS_FUEL_BLOCK);

        AETERNALIS_FUEL_BLOCK = registerSimpleBlock(AETERNALIS_FUEL_BLOCK_ID, 0.5F, 1.5F);
        AETERNALIS_FUEL_BLOCK_ITEM = registerBlockItem(AETERNALIS_FUEL_BLOCK_ID, AETERNALIS_FUEL_BLOCK);

        // Matter blocks
        DARK_MATTER_BLOCK = registerSimpleBlock(DARK_MATTER_BLOCK_ID, 1_000_000F, 3_000_000F);
        DARK_MATTER_BLOCK_ITEM = registerFireImmuneBlockItem(DARK_MATTER_BLOCK_ID, DARK_MATTER_BLOCK);

        RED_MATTER_BLOCK = registerSimpleBlock(RED_MATTER_BLOCK_ID, 2_000_000F, 6_000_000F);
        RED_MATTER_BLOCK_ITEM = registerFireImmuneBlockItem(RED_MATTER_BLOCK_ID, RED_MATTER_BLOCK);

        // Alchemical chest (104-slot storage)
        ALCHEMICAL_CHEST = registerBlock(ALCHEMICAL_CHEST_ID,
              new AlchemicalChestBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, ALCHEMICAL_CHEST_ID))
                    .strength(10.0F, 3_600_000F)
                    .sound(SoundType.STONE)));
        ALCHEMICAL_CHEST_ITEM = registerBlockItem(ALCHEMICAL_CHEST_ID, ALCHEMICAL_CHEST);
        ALCHEMICAL_CHEST_ENTITY = registerBlockEntity(ALCHEMICAL_CHEST_ID,
              new BlockEntityType<>(AlchemicalChestBlockEntity::new, java.util.Set.of(ALCHEMICAL_CHEST)));
        ModBlockEntities.ALCHEMICAL_CHEST = ALCHEMICAL_CHEST_ENTITY;

        // Matter furnaces (fast: DM=2x, RM=4x speed)
        var dmFurnaceBlock = new DMFurnaceBlock(BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, DM_FURNACE_ID))
              .strength(1_000_000F, 3_000_000F)
              .sound(SoundType.STONE));
        DM_FURNACE = registerBlock(DM_FURNACE_ID, dmFurnaceBlock);
        DM_FURNACE_ITEM = registerFireImmuneBlockItem(DM_FURNACE_ID, DM_FURNACE);
        DM_FURNACE_ENTITY = registerBlockEntity(DM_FURNACE_ID,
              new BlockEntityType<>(FastFurnaceBlockEntity.DM::new, java.util.Set.of(DM_FURNACE)));
        FastFurnaceBlockEntity.DM_TYPE = DM_FURNACE_ENTITY;

        var rmFurnaceBlock = new RMFurnaceBlock(BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, RM_FURNACE_ID))
              .strength(2_000_000F, 6_000_000F)
              .sound(SoundType.STONE));
        RM_FURNACE = registerBlock(RM_FURNACE_ID, rmFurnaceBlock);
        RM_FURNACE_ITEM = registerFireImmuneBlockItem(RM_FURNACE_ID, RM_FURNACE);
        RM_FURNACE_ENTITY = registerBlockEntity(RM_FURNACE_ID,
              new BlockEntityType<>(FastFurnaceBlockEntity.RM::new, java.util.Set.of(RM_FURNACE)));
        FastFurnaceBlockEntity.RM_TYPE = RM_FURNACE_ENTITY;

        // Pedestal
        var pedestalBlock = new PedestalBlock(BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, DM_PEDESTAL_ID))
              .strength(1_000_000F, 3_000_000F)
              .sound(SoundType.STONE));
        DM_PEDESTAL = registerBlock(DM_PEDESTAL_ID, pedestalBlock);
        DM_PEDESTAL_ITEM = registerFireImmuneBlockItem(DM_PEDESTAL_ID, DM_PEDESTAL);
        var dmPedestalEntity = registerBlockEntity(DM_PEDESTAL_ID,
              new BlockEntityType<>(PedestalBlockEntity::new, java.util.Set.of(DM_PEDESTAL)));
        ModBlockEntities.DM_PEDESTAL = dmPedestalEntity;

        // Collectors
        var collector1 = new CollectorBlock(1, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, COLLECTOR_MK1_ID)).strength(0.3F, 0.9F).sound(SoundType.STONE));
        COLLECTOR_MK1 = registerBlock(COLLECTOR_MK1_ID, collector1);
        COLLECTOR_MK1_ITEM = registerBlockItem(COLLECTOR_MK1_ID, COLLECTOR_MK1);
        ModBlockEntities.COLLECTOR_MK1 = registerBlockEntity(COLLECTOR_MK1_ID,
              new BlockEntityType<>(CollectorBlockEntity.MK1::new, java.util.Set.of(COLLECTOR_MK1)));
        CollectorBlockEntity.MK1_TYPE = ModBlockEntities.COLLECTOR_MK1;

        var collector2 = new CollectorBlock(2, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, COLLECTOR_MK2_ID)).strength(0.3F, 0.9F).sound(SoundType.STONE));
        COLLECTOR_MK2 = registerBlock(COLLECTOR_MK2_ID, collector2);
        COLLECTOR_MK2_ITEM = registerBlockItem(COLLECTOR_MK2_ID, COLLECTOR_MK2);
        ModBlockEntities.COLLECTOR_MK2 = registerBlockEntity(COLLECTOR_MK2_ID,
              new BlockEntityType<>(CollectorBlockEntity.MK2::new, java.util.Set.of(COLLECTOR_MK2)));
        CollectorBlockEntity.MK2_TYPE = ModBlockEntities.COLLECTOR_MK2;

        var collector3 = new CollectorBlock(3, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, COLLECTOR_MK3_ID)).strength(0.3F, 0.9F).sound(SoundType.STONE));
        COLLECTOR_MK3 = registerBlock(COLLECTOR_MK3_ID, collector3);
        COLLECTOR_MK3_ITEM = registerBlockItem(COLLECTOR_MK3_ID, COLLECTOR_MK3);
        ModBlockEntities.COLLECTOR_MK3 = registerBlockEntity(COLLECTOR_MK3_ID,
              new BlockEntityType<>(CollectorBlockEntity.MK3::new, java.util.Set.of(COLLECTOR_MK3)));
        CollectorBlockEntity.MK3_TYPE = ModBlockEntities.COLLECTOR_MK3;

        // Relays
        var relay1 = new RelayBlock(1, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, RELAY_MK1_ID)).strength(10.0F, 30.0F).sound(SoundType.STONE));
        RELAY_MK1 = registerBlock(RELAY_MK1_ID, relay1);
        RELAY_MK1_ITEM = registerBlockItem(RELAY_MK1_ID, RELAY_MK1);
        ModBlockEntities.RELAY_MK1 = registerBlockEntity(RELAY_MK1_ID,
              new BlockEntityType<>(RelayBlockEntity.MK1::new, java.util.Set.of(RELAY_MK1)));
        RelayBlockEntity.MK1_TYPE = ModBlockEntities.RELAY_MK1;

        var relay2 = new RelayBlock(2, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, RELAY_MK2_ID)).strength(10.0F, 30.0F).sound(SoundType.STONE));
        RELAY_MK2 = registerBlock(RELAY_MK2_ID, relay2);
        RELAY_MK2_ITEM = registerBlockItem(RELAY_MK2_ID, RELAY_MK2);
        ModBlockEntities.RELAY_MK2 = registerBlockEntity(RELAY_MK2_ID,
              new BlockEntityType<>(RelayBlockEntity.MK2::new, java.util.Set.of(RELAY_MK2)));
        RelayBlockEntity.MK2_TYPE = ModBlockEntities.RELAY_MK2;

        var relay3 = new RelayBlock(3, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, RELAY_MK3_ID)).strength(10.0F, 30.0F).sound(SoundType.STONE));
        RELAY_MK3 = registerBlock(RELAY_MK3_ID, relay3);
        RELAY_MK3_ITEM = registerBlockItem(RELAY_MK3_ID, RELAY_MK3);
        ModBlockEntities.RELAY_MK3 = registerBlockEntity(RELAY_MK3_ID,
              new BlockEntityType<>(RelayBlockEntity.MK3::new, java.util.Set.of(RELAY_MK3)));
        RelayBlockEntity.MK3_TYPE = ModBlockEntities.RELAY_MK3;

        // Condensers
        var condenser1 = new CondenserBlock(1, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, CONDENSER_MK1_ID)).strength(10.0F, 3_600_000F).sound(SoundType.STONE));
        CONDENSER_MK1 = registerBlock(CONDENSER_MK1_ID, condenser1);
        CONDENSER_MK1_ITEM = registerBlockItem(CONDENSER_MK1_ID, CONDENSER_MK1);
        ModBlockEntities.CONDENSER_MK1 = registerBlockEntity(CONDENSER_MK1_ID,
              new BlockEntityType<>(CondenserBlockEntity.MK1::new, java.util.Set.of(CONDENSER_MK1)));
        CondenserBlockEntity.MK1_TYPE = ModBlockEntities.CONDENSER_MK1;

        var condenser2 = new CondenserBlock(2, BlockBehaviour.Properties.of()
              .setId(ResourceKey.create(Registries.BLOCK, CONDENSER_MK2_ID)).strength(10.0F, 3_600_000F).sound(SoundType.STONE));
        CONDENSER_MK2 = registerBlock(CONDENSER_MK2_ID, condenser2);
        CONDENSER_MK2_ITEM = registerFireImmuneBlockItem(CONDENSER_MK2_ID, CONDENSER_MK2);
        ModBlockEntities.CONDENSER_MK2 = registerBlockEntity(CONDENSER_MK2_ID,
              new BlockEntityType<>(CondenserBlockEntity.MK2::new, java.util.Set.of(CONDENSER_MK2)));
        CondenserBlockEntity.MK2_TYPE = ModBlockEntities.CONDENSER_MK2;

        // Interdiction torch (mob repellent)
        INTERDICTION_TORCH = registerBlock(INTERDICTION_TORCH_ID,
              new InterdictionTorchBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, INTERDICTION_TORCH_ID))
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.WOOD)
                    .noCollision()
                    .lightLevel(s -> 14)));
        INTERDICTION_TORCH_ITEM = registerBlockItem(INTERDICTION_TORCH_ID, INTERDICTION_TORCH);

        // Explosives
        NOVA_CATALYST = registerBlock(NOVA_CATALYST_ID,
              new NovaCatalystBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, NOVA_CATALYST_ID))
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.GRASS)));
        NOVA_CATALYST_ITEM = registerBlockItem(NOVA_CATALYST_ID, NOVA_CATALYST);

        NOVA_CATACLYSM = registerBlock(NOVA_CATACLYSM_ID,
              new NovaCataclysmBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, NOVA_CATACLYSM_ID))
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.GRASS)));
        NOVA_CATACLYSM_ITEM = registerBlockItem(NOVA_CATACLYSM_ID, NOVA_CATACLYSM);
    }

    private static Block registerBlock(Identifier id, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static BlockItem registerBlockItem(Identifier id, Block block) {
        return Registry.register(BuiltInRegistries.ITEM, id,
              new BlockItem(block, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))));
    }

    private static BlockItem registerFireImmuneBlockItem(Identifier id, Block block) {
        return Registry.register(BuiltInRegistries.ITEM, id,
              new BlockItem(block, new Item.Properties()
                    .fireResistant()
                    .setId(ResourceKey.create(Registries.ITEM, id))));
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> registerBlockEntity(
          Identifier id, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
    }

    private static Block registerSimpleBlock(Identifier id, float hardness, float resistance) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        return registerBlock(id, new Block(BlockBehaviour.Properties.of()
              .setId(key)
              .strength(hardness, resistance)
              .sound(SoundType.STONE)));
    }
}
