package net.jhabit.qlogic.mixin;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class MinecartSpeedFixMixin {

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void onGetMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(0.8D);
    }

    @Inject(method = "useExperimentalMovement", at = @At("HEAD"), cancellable = true)
    private static void onUseExperimentalMovement(CallbackInfoReturnable<Boolean> cir) {
        // 실험적 물리 엔진을 강제로 활성화하여 오르막길 충돌 버그 해결
        cir.setReturnValue(true);
    }
}