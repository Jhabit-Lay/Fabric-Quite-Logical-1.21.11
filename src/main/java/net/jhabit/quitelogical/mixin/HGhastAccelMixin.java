package net.jhabit.quitelogical.mixin;

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
    private static final float ACCEL_STEP = 0.001f; // 가속 속도 (약간 상향)

    @Inject(method = "travel", at = @At("HEAD"))
    private void hg$dynamicAcceleration(Vec3 travelVector, CallbackInfo ci) {
        if (this.isAlive() && this.getControllingPassenger() instanceof Player player) {

            boolean isMovingForward = player.zza > 0;

            // 1. 현재 상태에 따른 최대 가속 한계치 결정
            float currentLimit = NORMAL_MAX_EXTRA;
            if (this.hasEffect(MobEffects.SPEED)) {
                int amp = this.getEffect(MobEffects.SPEED).getAmplifier();
                // 신속 I일 때 POTION_MAX_EXTRA, 신속 II일 때 더 빠르게 설정 가능
                currentLimit = POTION_MAX_EXTRA + (amp * 0.05f);
            }

            // 2. 가속 로직 (W 입력 시)
            if (isMovingForward) {
                if (hg_extraSpeed < currentLimit) {
                    hg_extraSpeed += ACCEL_STEP;
                } else if (hg_extraSpeed > currentLimit) {
                    // 포션이 끝나서 현재 속도가 리미트보다 높을 경우 서서히 줄임
                    hg_extraSpeed -= ACCEL_STEP * 2;
                }
            } else {
                // W를 떼면 리셋 (부드러운 감속을 원하면 -= 사용)
                hg_extraSpeed = 0.0f;
            }

            // 3. 물리 적용
            if (hg_extraSpeed > 0) {
                this.moveRelative(hg_extraSpeed, new Vec3(0, 0, 1));
            }
        } else {
            hg_extraSpeed = 0.0f;
        }
    }
}