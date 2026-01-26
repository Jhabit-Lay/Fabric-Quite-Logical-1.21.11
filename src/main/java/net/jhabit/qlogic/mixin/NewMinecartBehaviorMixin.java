package net.jhabit.qlogic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;

@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin {

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void qlogic$onGetMaxSpeed(CallbackInfoReturnable<Double> cir) {
        // [KR] 1.1D로 설정하여 다른 믹스인들과 조화를 이룹니다.
        // [EN] Set to 1.1D to harmonize with other mixins.
        cir.setReturnValue(0.8D);
    }
}