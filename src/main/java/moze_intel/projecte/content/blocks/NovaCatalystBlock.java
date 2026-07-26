package moze_intel.projecte.content.blocks;

/** Nova Catalyst — explodes with power 8 (2× TNT). */
public class NovaCatalystBlock extends NovaBlock {
    private static final float EXPLOSION_POWER = 8.0F;

    public NovaCatalystBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float explosionPower() {
        return EXPLOSION_POWER;
    }
}
