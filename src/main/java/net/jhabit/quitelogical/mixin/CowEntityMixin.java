package net.jhabit.quitelogical.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Cow.class)
public abstract class CowEntityMixin extends Animal {

    protected CowEntityMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void qlogic$initCowAI(EntityType<? extends Cow> type, Level world, CallbackInfo ci) {

        // 1. 도망 로직 제거
        this.goalSelector.getAvailableGoals().removeIf(goal -> goal.getGoal() instanceof PanicGoal);

        // 2. [통합 AI] 거리 + 시간 + 무리 반격 로직
        // 중복 등록을 피하기 위해 하나의 HurtByTargetGoal만 사용합니다.
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            private int angerTicks = 0;
            private final int MAX_ANGER_TIME = 200; // 10초
            private final double MAX_DISTANCE_SQR = 225.0D; // 15칸 (15 * 15)

            @Override
            public void start() {
                super.start();
                this.angerTicks = 0;
                // 공격받기 시작할 때 즉시 주변 동료들을 부릅니다.
                this.alertOthers();
            }

            @Override
            public void tick() {
                super.tick();
                this.angerTicks++;
            }

            @Override
            public boolean canContinueToUse() {
                if (mob.getTarget() == null) return false;

                // 거리 체크 (15칸)
                double distanceSqr = mob.distanceToSqr(mob.getTarget());
                if (distanceSqr > MAX_DISTANCE_SQR) return false;

                // 시간 체크 (10초)
                return super.canContinueToUse() && this.angerTicks < MAX_ANGER_TIME;
            }

            @Override
            public void stop() {
                super.stop();
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
            }

            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }

            @Override
            protected void alertOthers() {
                // 수평 9칸, 수직 4칸 범위 설정
                double range = 9.0D;
                AABB area = mob.getBoundingBox().inflate(range, 4.0D, range);

                // 주변 소/무시룸 리스트 확보
                List<? extends Mob> nearbyCows = mob.level().getEntitiesOfClass(mob.getClass(), area);

                for (Mob nearbyCow : nearbyCows) {
                    // 자기 자신 제외 & 새끼 제외 & 현재 타겟이 없는 동료만 호출
                    if (mob != nearbyCow && !((Animal)nearbyCow).isBaby() && nearbyCow.getTarget() == null) {
                        this.alertOther(nearbyCow, mob.getLastHurtByMob());
                    }
                }
            }
        });

        // 3. 근접 공격 수행 AI
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
    }
}