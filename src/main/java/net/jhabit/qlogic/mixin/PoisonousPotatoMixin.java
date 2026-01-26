package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.entity.ThrownPoisonousPotato;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class PoisonousPotatoMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void qlogic$cancelEatingAndThrow(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.POISONOUS_POTATO)) {
            if (!level.isClientSide()) {
                ThrownPoisonousPotato entity = new ThrownPoisonousPotato(level, player);
                entity.setItem(itemStack);
                entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
                level.addFreshEntity(entity);
            }
            if (!player.getAbilities().instabuild) itemStack.shrink(1);
            // SUCCESS를 반환하여 먹기(Consumable) 로직이 실행되지 않게 막음
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
