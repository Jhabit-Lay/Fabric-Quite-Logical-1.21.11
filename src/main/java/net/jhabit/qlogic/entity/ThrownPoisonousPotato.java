package net.jhabit.qlogic.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownPoisonousPotato extends ThrowableItemProjectile {
    public ThrownPoisonousPotato(EntityType<? extends ThrownPoisonousPotato> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownPoisonousPotato(Level level, LivingEntity owner) {
        super(ModEntities.THROWN_POISONOUS_POTATO, level);
        this.setOwner(owner);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.POISONOUS_POTATO;
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity target = entityHitResult.getEntity();
        if (target == this.getOwner()) return;

        // [KR] 서버에서만 데미지와 효과 적용 / [EN] Apply damage and effects on server only
        if (this.level() instanceof ServerLevel serverLevel) {
            // [KR] LivingEntity 소스 코드 1256행의 hurtServer 사용 권장
            // [EN] Recommended to use hurtServer from line 1256 of LivingEntity.java
            target.hurtServer(serverLevel, this.damageSources().thrown(this, this.getOwner()), 1.0f);

            if (target instanceof LivingEntity livingTarget && this.random.nextFloat() < 0.6f) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 0));
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            if (((EntityHitResult) hitResult).getEntity() == this.getOwner()) return;
        }

        super.onHit(hitResult);

        if (!this.level().isClientSide()) {
            // [KR] 아이템 파괴 입자 전송 (byte 3은 파괴 이벤트를 의미함)
            // [EN] Broadcast item break particles (byte 3)
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }
}