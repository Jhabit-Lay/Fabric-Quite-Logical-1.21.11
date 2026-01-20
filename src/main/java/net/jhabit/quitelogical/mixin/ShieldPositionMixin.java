package net.jhabit.quitelogical.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ShieldPositionMixin {

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void adjustShieldSideSpecific(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        if (itemStack.is(Items.SHIELD)) {

            var player = abstractClientPlayer;
            var hand = interactionHand;

            // 1. 현재 렌더링 중인 손이 오른쪽인지 왼쪽인지 판별
            boolean isRightSide = (hand == InteractionHand.MAIN_HAND && player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) ||
                    (hand == InteractionHand.OFF_HAND && player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);

            // 2. 방향에 따른 X축 오프셋 결정 (오른쪽이면 +, 왼쪽이면 -)
            float xTranslate = isRightSide ? 0.19f : -0.16f;

            // 3. 위치 조정 (Y축은 공통으로 내림)
            poseStack.translate(xTranslate, -0.1f, -0.1f);

            // 4. 크기 조정 (공통 적용)
            float scale = 0.80f;
            poseStack.scale(scale, scale, scale);
        }
    }
}