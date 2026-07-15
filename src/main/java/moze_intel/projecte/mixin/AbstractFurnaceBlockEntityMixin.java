package moze_intel.projecte.mixin;

import moze_intel.projecte.content.blocks.FurnaceBurnTimePersistence;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AbstractFurnaceBlockEntityMixin {
    @Shadow
    private int litTimeRemaining;

    @Shadow
    private int litTotalTime;

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void projecte$loadFullBurnTimes(ValueInput input, CallbackInfo ci) {
        this.litTimeRemaining = FurnaceBurnTimePersistence.readLitTimeRemaining(input);
        this.litTotalTime = FurnaceBurnTimePersistence.readLitTotalTime(input);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void projecte$saveFullBurnTimes(ValueOutput output, CallbackInfo ci) {
        FurnaceBurnTimePersistence.writeBurnTimes(
              output, this.litTimeRemaining, this.litTotalTime);
    }
}
