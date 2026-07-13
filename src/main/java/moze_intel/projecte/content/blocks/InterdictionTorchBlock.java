package moze_intel.projecte.content.blocks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.TorchBlock;

/**
 * Interdiction Torch — repels hostile mobs.
 * Repulsion logic will be wired via a server tick event handler.
 */
public class InterdictionTorchBlock extends TorchBlock {
    public InterdictionTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
    }
}
