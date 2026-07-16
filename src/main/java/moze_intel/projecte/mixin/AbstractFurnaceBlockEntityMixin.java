package moze_intel.projecte.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import moze_intel.projecte.content.blocks.FastFurnaceBlockEntity;
import moze_intel.projecte.content.blocks.FurnaceBurnTimePersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    @ModifyExpressionValue(
          method = "serverTick",
          at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/item/crafting/AbstractCookingRecipe;"
                      + "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;)"
                      + "Lnet/minecraft/world/item/ItemStack;"
          )
    )
    private static ItemStack projecte$scaleMatterFurnaceOutput(
          ItemStack result,
          ServerLevel level,
          BlockPos pos,
          BlockState state,
          AbstractFurnaceBlockEntity furnace
    ) {
        return FastFurnaceBlockEntity.scaleOutput(furnace, result, level.getRandom());
    }
}
