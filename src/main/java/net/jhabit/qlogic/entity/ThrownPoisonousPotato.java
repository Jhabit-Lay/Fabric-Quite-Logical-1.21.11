package net.jhabit.qlogic.entity;

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
        // [주의] super 호출 시 owner를 직접 넣지 못하는 환경이라면 아래와 같이 setOwner를 별도로 사용합니다.
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

        // [해결] 주인(Owner)은 절대 맞지 않게 합니다.
        // tickCount < 2 조건을 추가하여 발사 초기 오작동을 한 번 더 막습니다.
        if (target == this.getOwner() || this.tickCount < 2) {
            return;
        }

        super.onHitEntity(entityHitResult);

        // 데미지 하트 반 칸 (1.0f)
        target.hurt(this.damageSources().thrown(this, this.getOwner()), 1.0f);

        if (target instanceof LivingEntity livingTarget) {
            // 60% 확률로 5초 중독
            if (this.random.nextFloat() < 0.6f) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        // [해결] 엔티티 충돌 시, 대상이 주인이라면 super.onHit을 호출하지 않고 종료합니다.
        // 이렇게 해야 투사체가 주인을 통과하고, 사라지지(discard) 않습니다.
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            if (entityHit.getEntity() == this.getOwner()) {
                return;
            }
        }

        super.onHit(hitResult);

        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte)3); // 아이템 파기 입자
            this.discard();
        }
    }
}