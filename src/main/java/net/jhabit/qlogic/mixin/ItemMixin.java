package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.entity.ThrownPoisonousPotato;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void throwPoisonousPotato(Level level, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemStack = user.getItemInHand(hand);

        if (itemStack.is(Items.POISONOUS_POTATO)) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

            if (!level.isClientSide()) {
                ThrownPoisonousPotato potatoEntity = new ThrownPoisonousPotato(level, user);

                // [해결] 발사 위치를 플레이어의 시선 방향으로 0.5블록 앞당깁니다.
                // 이렇게 해야 생성되자마자 본인 몸에 맞아 사라지는 현상을 방지할 수 있습니다.
                Vec3 look = user.getLookAngle();
                potatoEntity.setPos(
                        user.getX() + look.x * 0.5,
                        user.getEyeY() + look.y * 0.5,
                        user.getZ() + look.z * 0.5
                );

                potatoEntity.setItem(itemStack);
                // same throw speed with snowball
                potatoEntity.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0f, 1.5f, 1.0f);
                level.addFreshEntity(potatoEntity);
            }

            // consume item
            if (!user.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            user.awardStat(Stats.ITEM_USED.get((Item) (Object) this));

            // block eating
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void removePotatoAnim(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (stack.is(Items.POISONOUS_POTATO)) {
            cir.setReturnValue(ItemUseAnimation.NONE);
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void removePotatoDuration(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (stack.is(Items.POISONOUS_POTATO)) {
            cir.setReturnValue(0);
        }
    }
}