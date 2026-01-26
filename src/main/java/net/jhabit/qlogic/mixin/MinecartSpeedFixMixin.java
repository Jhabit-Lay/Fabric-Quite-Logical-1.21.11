package net.jhabit.qlogic.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [KR] 마인카트 속도 향상 및 정밀 동기화 믹스인
 * [EN] Mixin for Minecart speed boost and precise synchronization
 */
@Mixin(AbstractMinecart.class)
public abstract class MinecartSpeedFixMixin {

    /**
     * [KR] 최고 속도를 1.1D로 상향 (바닐라 0.4D)
     * [EN] Increase max speed to 1.1D (Vanilla 0.4D)
     */
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void qlogic$getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(0.8D);
    }

    /**
     * [KR] 실험적 물리 엔진 강제 활성화
     * [EN] Force enable experimental physics engine
     */
    @Inject(method = "useExperimentalMovement", at = @At("HEAD"), cancellable = true)
    private static void qlogic$useExperimentalMovement(Level level, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /**
     * [KR] 멀티플레이어 끊김 방지를 위한 정밀 위치 모드 활성화
     * [EN] Enable precise position mode to prevent multiplayer jitter
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void qlogic$enablePrecisePosition(CallbackInfo ci) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        // [KR] 소스 코드 522번 줄의 메서드를 사용하여 네트워크 동기화 정밀도를 높임
        // [EN] Use the method on line 522 to increase network sync precision
        cart.setRequiresPrecisePosition(true);
    }
}