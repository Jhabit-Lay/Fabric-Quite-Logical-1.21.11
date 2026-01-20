package net.jhabit.quitelogical.entity;

import net.jhabit.quitelogical.QuiteLogical;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LeaderZombie extends Zombie {
    public LeaderZombie(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 15; // 경험치 보상 설정
    }

    @Override
    public void tick() {
        super.tick();

        // 클라이언트 사이드에서만 실행
        if (this.level().isClientSide()) {
            // 생성 빈도 조절 (0.3f는 약 3틱당 1개꼴)
            if (this.random.nextFloat() < 0.2f) {

                // 1. 생성 위치: 발밑 중심부 (약간의 오차 부여)
                double x = this.getX() + (this.random.nextDouble() - 0.5) * 0.3;
                double y = this.getY() + 0.0;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * 0.3;

                // 2. 사방으로 퍼지는 랜덤 속도 (X, Z)
                // -0.05 ~ 0.05 사이의 값으로 사방으로 퍼짐
                double vx = (this.random.nextDouble() - 0.55) * 0.25;
                double vz = (this.random.nextDouble() - 0.55) * 0.25;

                // 3. 위로 떠오르는 속도 (Y)
                // 0.03 ~ 0.08 사이의 값으로 부드럽게 상승
                double vy = 0.01 + this.random.nextDouble() * 0.04;

                // 4. 파티클 소환
                this.level().addParticle(
                        ParticleTypes.END_ROD, // 빛나는 작은 흰색 점
                        x, y, z,
                        vx, vy, vz
                );
            }
        }
    }
    // 장비 드랍 확률 100% 설정 (스폰 직후 호출용)
    public void setGuaranteedDrops() {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 1.0F);
        }
    }
    @Override
    public boolean isSunSensitive() {
        QuiteLogical.LOGGER.info("리더 좀비 연소 체크 중!");
        return true; // 낮에 불타게 함
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