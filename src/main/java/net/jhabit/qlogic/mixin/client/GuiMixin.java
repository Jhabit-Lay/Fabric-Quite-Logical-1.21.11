package net.jhabit.qlogic.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow private int toolHighlightTimer;

    // [설정] 나타날 때는 부드럽게(8틱), 사라질 때는 순식간에(3틱)
    @Unique private static final float IN_DURATION = 12.0F;
    @Unique private static final float OUT_DURATION = 2.0F;

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"))
    private void qlogic$beginPerfectPivotAnimation(GuiGraphics guiGraphics, CallbackInfo ci) {
        // 1.21.11 DeltaTracker를 통한 실시간 프레임 보간
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
        float realTime = (float)this.toolHighlightTimer - partialTick;

        if (realTime > 0) {
            float scale = qlogic$getPolishedScale(realTime);

            if (scale != 1.0F && scale > 0.0F) {
                float x = (float)guiGraphics.guiWidth() / 2.0F;
                float y = (float)guiGraphics.guiHeight() - 59.0F;

                // [피벗 보정] 텍스트가 위로 들리는 현상을 방지하기 위해 축을 중앙보다 약간 아래인 5.0f로 설정
                float pivotY = y + 7.0F;

                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(x, pivotY);
                guiGraphics.pose().scale(scale, scale);
                guiGraphics.pose().translate(-x, -pivotY);
            }
        }
    }

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("RETURN"))
    private void qlogic$endPerfectPivotAnimation(GuiGraphics guiGraphics, CallbackInfo ci) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
        float realTime = (float)this.toolHighlightTimer - partialTick;

        if (realTime > 0 && qlogic$getPolishedScale(realTime) != 1.0F) {
            try {
                guiGraphics.pose().popMatrix();
            } catch (IllegalStateException e) {
                // 스택 오류 방지
            }
        }
    }

    @Unique
    private float qlogic$getPolishedScale(float realTime) {
        // 1. 나타날 때 (1.2 -> 1.0): 8틱 동안 부드럽게 수축 (Ease-Out)
        if (realTime > 40.0F - IN_DURATION) {
            float t = Mth.clamp((40.0F - realTime) / IN_DURATION, 0.0F, 1.0F);
            float invT = 1.0F - t;
            // 3차 함수를 사용해 처음엔 팍! 나중엔 아주 천천히 안착
            return 1.0F + 0.15F * (invT * invT * invT);
        }
        // 2. 사라질 때 (1.0 -> 0.0): 3틱 만에 초고속 소멸 (Ease-In)
        else if (realTime < OUT_DURATION) {
            float t = Mth.clamp(realTime / OUT_DURATION, 0.0F, 0.5F);
            // [Image of a cubic ease-in curve starting flat and curving down]
            // 4제곱 가속 소멸: 처음 2틱은 끈적하게 버티다가 마지막 1틱에 점으로 증발
            return t * t * t * t;
        }
        return 1.0F;
    }
}