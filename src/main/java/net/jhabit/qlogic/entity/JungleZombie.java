package net.jhabit.qlogic.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JungleZombie extends Zombie {
    public JungleZombie(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean success = super.doHurtTarget(level, target);
        if (success && target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        }
        return success;
    }

    // IDE에서 Unused라고 떠도 절대 삭제하지 마세요.
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
            if (this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
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