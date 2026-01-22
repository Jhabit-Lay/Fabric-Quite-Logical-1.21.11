package net.jhabit.qlogic.mixin;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HappyGhast.class)
public abstract class HGhastAccelMixin extends Mob {

    // 부모 클래스인 LivingEntity로부터 전진 입력값 필드를 가져옵니다.
    // Mojang Mapping에서 forward 입력값은 'zza'입니다.

    protected HGhastAccelMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private float hg_extraSpeed = 0.0f;
    @Unique
    private static final float NORMAL_MAX_EXTRA = 0.04f; // 포션 없을 때의 쾌적한 가속도
    @Unique
    private static final float POTION_MAX_EXTRA = 0.04f; // 포션 있을 때의 "말도 안 되는" 속도
    @Unique
    private static final float ACCEL_STEP = 0.001f; // 가속도 증가량
    @Unique
    private static final float DECEL_STEP = 0.04f; // 감속도 증가량 (가속보다 천천히 멈춤)

    @Inject(method = "travel", at = @At("HEAD"))
    private void hg$accelerationWithInertia(Vec3 travelVector, CallbackInfo ci) {
        if (this.isAlive() && this.getControllingPassenger() instanceof Player player) {

            boolean isMovingForward = player.zza > 0;

            // 1. 현재 한계치 결정 (신속 포션 유무)
            float currentLimit = NORMAL_MAX_EXTRA;
            if (this.hasEffect(MobEffects.SPEED)) {
                int amp = this.getEffect(MobEffects.SPEED).getAmplifier();
                currentLimit = POTION_MAX_EXTRA + (amp * 0.05f);
            }

            // 2. 가속 및 관성 감속 로직
            if (isMovingForward) {
                // 전진 중: 속도 증가
                if (hg_extraSpeed < currentLimit) {
                    hg_extraSpeed = Math.min(currentLimit, hg_extraSpeed + ACCEL_STEP);
                } else if (hg_extraSpeed > currentLimit) {
                    // 포션이 끝났을 때 초과 속도를 부드럽게 줄임
                    hg_extraSpeed = Math.max(currentLimit, hg_extraSpeed - DECEL_STEP);
                }
            } else {
                // 전진 중단: 관성에 의해 서서히 감소
                if (hg_extraSpeed > 0) {
                    hg_extraSpeed = Math.max(0.0f, hg_extraSpeed - DECEL_STEP);
                }
            }

            // 3. 물리 적용
            if (hg_extraSpeed > 0) {
                // moveRelative는 엔티티의 현재 바라보는 방향으로 힘을 가함
                this.moveRelative(hg_extraSpeed, new Vec3(0, 0, 1));
            }
        } else {
            // 플레이어가 내리면 관성 없이 즉시 초기화 (안전상의 이유)
            hg_extraSpeed = 0.0f;
        }
    }
}