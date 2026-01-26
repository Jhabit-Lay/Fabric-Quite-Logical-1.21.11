package net.jhabit.qlogic.mixin;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class EntityTypeUpdateMixin {
    /**
     * [KR] 마인카트 엔티티들의 위치 업데이트 주기를 1틱으로 단축하여 버벅임을 제거합니다.
     * [EN] Reduce update interval to 1 tick for minecarts to eliminate stuttering.
     */
    @Inject(method = "updateInterval", at = @At("HEAD"), cancellable = true)
    private void qlogic$boostUpdateInterval(CallbackInfoReturnable<Integer> cir) {
        EntityType<?> type = (EntityType<?>) (Object) this;

        // 모든 종류의 마인카트 판별
        if (type == EntityType.MINECART || type == EntityType.CHEST_MINECART ||
                type == EntityType.FURNACE_MINECART || type == EntityType.HOPPER_MINECART ||
                type == EntityType.TNT_MINECART) {

            cir.setReturnValue(1); // 매 틱마다 클라이언트에 위치 전송
        }
    }
}