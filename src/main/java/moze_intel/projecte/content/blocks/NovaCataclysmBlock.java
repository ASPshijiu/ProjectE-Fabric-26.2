package moze_intel.projecte.content.blocks;

/** Nova Cataclysm — massive explosion (power 16, 4× TNT). */
public class NovaCataclysmBlock extends NovaBlock {
    private static final float EXPLOSION_POWER = 16.0F;

    public NovaCataclysmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float explosionPower() {
        return EXPLOSION_POWER;
    }
}
