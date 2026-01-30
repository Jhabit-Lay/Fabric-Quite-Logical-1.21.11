package net.jhabit.qlogic.mixin;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [KR] 마인카트 시스템 설정 Mixin (충돌 방지를 위해 getMaxSpeed 제거)
 * [EN] Minecart system settings Mixin (Removed getMaxSpeed to prevent conflict)
 */
@Mixin(AbstractMinecart.class)
public abstract class MinecartSpeedFixMixin {

    // [KR] getMaxSpeed 주입 제거: MinecartBehaviorMixin에서 통합 관리합니다.
    // [EN] Removed getMaxSpeed injection: Managed in MinecartBehaviorMixin.

    /**
     * [KR] 실험적 물리 엔진 강제 활성화
     */
    @Inject(method = "useExperimentalMovement", at = @At("HEAD"), cancellable = true)
    private static void qlogic$useExperimentalMovement(Level level, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /**
     * [KR] 멀티플레이어 끊김 방지를 위한 정밀 위치 모드 활성화
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void qlogic$enablePrecisePosition(CallbackInfo ci) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        cart.setRequiresPrecisePosition(true);
    }
}