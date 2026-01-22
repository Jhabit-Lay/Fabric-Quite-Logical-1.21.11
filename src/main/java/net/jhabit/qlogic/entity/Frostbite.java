package net.jhabit.qlogic.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Frostbite extends Zombie {
    public Frostbite(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void actuallyHurt(ServerLevel serverLevel, DamageSource damageSource, float amount) {
        // 1. 화염 속성 데미지(불, 용암, 화염구 등)인지 확인
        if (damageSource.is(DamageTypeTags.IS_FIRE)) {
            // 2. 데미지 양을 1.5배로 증폭
            amount *= 2.0F;
        }
        // 3. 증폭된 amount를 바닐라 로직(사용자가 보여준 코드)으로 전달
        // 여기서 체력 차감, 흡수량 계산, 통계 기록 등이 처리됩니다.
        super.actuallyHurt(serverLevel, damageSource, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean success = super.doHurtTarget(level, target);
        if (success && target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
        }
        return success;
    }

    // 바닐라 Zombie 클래스의 aiStep()이 이 메서드를 호출하여 AI 상태를 결정합니다.
    @Override
    public boolean isSunSensitive() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            // 1. 평화로움 제거 로직
            if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
                this.discard();
                return;
            }

            // 2. 연소 로직 (데미지 빈도 조절 버전)
            // 낮 시간 확인 (0 ~ 12000 틱 사이가 낮입니다)
            long dayTime = this.level().getDayTime() % 24000;
            boolean isDay = dayTime < 12000;

            if (isDay && !this.level().isRaining() && this.level().canSeeSky(this.blockPosition())) {

                // 핵심 수정: 이미 불타고 있는지 확인합니다.
                // 이렇게 해야 1초에 20번 데미지가 들어가는 현상을 막을 수 있습니다.
                if (!this.isOnFire()) {
                    ItemStack headStack = this.getItemBySlot(EquipmentSlot.HEAD);
                    if (headStack.isEmpty()) {
                        // 8초(160틱) 동안 불을 붙입니다.
                        this.setRemainingFireTicks(160);
                    }
                }
            }
        }
    }
}