package net.jhabit.quitelogical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior.class)
public class NewMinecartBehavior {

    /**
     * 새로운 물리 엔진 내부에서 최고 속도를 참조할 때
     * 존재하지 않는 GameRule(minecartMaxSpeed)을 읽지 않고 직접 값을 반환하게 함.
     */
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void onGetMaxSpeed(CallbackInfoReturnable<Double> cir) {
        // 1.1D = 약 22m/s (바닐라 0.4D의 약 2.75배)
        cir.setReturnValue(1.1D);
    }
}