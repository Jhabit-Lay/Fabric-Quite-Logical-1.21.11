package net.jhabit.quitelogical.mixin;

import net.jhabit.quitelogical.QuiteLogical;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;

@Mixin(Zombie.class)
public abstract class LeaderZombieMixin extends Monster {

    protected LeaderZombieMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void upgradeLeaderRewards(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
        Zombie zombie = (Zombie)(Object)this;
        double reinforcementChance = zombie.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE);

        if (zombie.getType() == QuiteLogical.ZOMBIE_LEADER) {

            var scaleAttr = zombie.getAttribute(Attributes.SCALE);
            if (scaleAttr != null) scaleAttr.setBaseValue(1.1);

            var maxHealthAttr = zombie.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(30.0);
                zombie.setHealth(30.0f);
            }

            var attackDamageAttr = zombie.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamageAttr != null) {
                attackDamageAttr.setBaseValue(4.0);
            }

            // 2. 경험치 보상
            this.xpReward = 15;

            // 3. 장비 드랍 확률 100%
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                zombie.setDropChance(slot, 1.0F);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void addLeaderParticles(CallbackInfo ci) {
        if (this.level().isClientSide()) {
            Zombie zombie = (Zombie)(Object)this;
            if (zombie.getAttributeValue(Attributes.SCALE) >= 1.1) {
                if (this.random.nextFloat() < 0.15f) {
                    // FIREWORK -> FIREWORK_ROCKET (버전별 확인 필요, 1.21은 FIREWORK 사용 가능)
                    this.level().addParticle(
                            ParticleTypes.FIREWORK,
                            this.getRandomX(0.5),
                            this.getY() + 0.1, // 발밑(0.0)
                            this.getRandomZ(0.5),
                            0.0, 0.1, 0.0
                    );
                }
            }
        }
    }
    @Inject(method = "isSunSensitive", at = @At("HEAD"), cancellable = true)
    private void forceSunSensitivity(CallbackInfoReturnable<Boolean> cir) {
        if (this.getType() == QuiteLogical.ZOMBIE_LEADER) {
            cir.setReturnValue(true);
        }
    }

    // [평화로움 즉시 제거]
    @Inject(method = "tick", at = @At("HEAD"))
    private void forcePeacefulRemoval(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            // 난이도가 평화로움이면 즉시 제거
            if (this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                if (this.getType() == QuiteLogical.ZOMBIE_LEADER) {
                    this.discard();
                }
            }
        }
    }
}