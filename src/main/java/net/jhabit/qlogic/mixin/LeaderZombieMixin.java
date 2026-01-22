package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.QuiteLogical;
import net.jhabit.qlogic.entity.LeaderZombie;
import net.jhabit.qlogic.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class LeaderZombieMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void replaceVanillaLeader(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
        Zombie zombie = (Zombie)(Object)this;

        // 1. 무한 루프 방지 및 중복 교체 방지
        // 이미 커스텀 엔티티(리더, 정글, 프로스트바이트)라면 로직을 건너뜁니다.
        if (zombie.getType() == ModEntities.ZOMBIE_LEADER ||
                zombie.getType() == ModEntities.JUNGLE_ZOMBIE ||
                zombie.getType() == ModEntities.FROSTBITE) {
            return;
        }

        // 2. 리더 좀비 판정 (바닐라 reinforcementChance 기준)
        double reinforcementChance = zombie.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE);

        // [수정] 0.0보다 큰 게 아니라, 0.05(5%) 이상인 개체만 리더로 인정합니다.
        if (reinforcementChance >= 0.6) {
            // 3. [교체 로직]
            if (levelAccessor instanceof ServerLevel serverLevel) {
                LeaderZombie newLeader = ModEntities.ZOMBIE_LEADER.create(serverLevel, reason);

                if (newLeader != null) {
                    // 위치 설정
                    newLeader.setPos(zombie.getX(), zombie.getY(), zombie.getZ());
                    newLeader.setYRot(zombie.getYRot());
                    newLeader.setXRot(zombie.getXRot());
                    newLeader.finalizeSpawn(levelAccessor, difficulty, reason, data);

                    var leaderAttr = newLeader.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
                    if (leaderAttr != null) leaderAttr.setBaseValue(reinforcementChance);

                    newLeader.setGuaranteedDrops();

                    zombie.discard();
                    levelAccessor.addFreshEntity(newLeader);

                    QuiteLogical.LOGGER.info("진짜 리더 좀비 발견! 교체 완료 (수치: {})", reinforcementChance);
                }
            }
        }
    }
}