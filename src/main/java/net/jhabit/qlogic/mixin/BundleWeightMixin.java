package net.jhabit.qlogic.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleContents.class)
public class BundleWeightMixin {

    @Inject(method = "getWeight", at = @At("HEAD"), cancellable = true)
    private static void modifyItemWeight(ItemStack itemStack, CallbackInfoReturnable<Fraction> cir) {
        // 나침반, 시계, 꾸러미, 망원경 체크
        if (itemStack.is(Items.COMPASS) ||
                itemStack.is(Items.CLOCK) ||
                itemStack.is(Items.SPYGLASS)) {

            // 일반 아이템(64개 스택)과 동일한 1/64 무게를 반환
            cir.setReturnValue(Fraction.getFraction(1, 64));
        }
    }
}