package moze_intel.projecte.content;

import moze_intel.projecte.content.blocks.AlchemicalChestBlockEntity;
import moze_intel.projecte.content.blocks.CollectorBlockEntity;
import moze_intel.projecte.content.blocks.CondenserBlockEntity;
import moze_intel.projecte.content.blocks.PedestalBlockEntity;
import moze_intel.projecte.content.blocks.RelayBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Type holders for ProjectE block entities. Set by {@link ModBlocks#init()}. */
public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static BlockEntityType<AlchemicalChestBlockEntity> ALCHEMICAL_CHEST;
    public static BlockEntityType<CollectorBlockEntity.MK1> COLLECTOR_MK1;
    public static BlockEntityType<CollectorBlockEntity.MK2> COLLECTOR_MK2;
    public static BlockEntityType<CollectorBlockEntity.MK3> COLLECTOR_MK3;
    public static BlockEntityType<RelayBlockEntity.MK1> RELAY_MK1;
    public static BlockEntityType<RelayBlockEntity.MK2> RELAY_MK2;
    public static BlockEntityType<RelayBlockEntity.MK3> RELAY_MK3;
    public static BlockEntityType<CondenserBlockEntity.MK1> CONDENSER_MK1;
    public static BlockEntityType<CondenserBlockEntity.MK2> CONDENSER_MK2;
    public static BlockEntityType<PedestalBlockEntity> DM_PEDESTAL;
}
