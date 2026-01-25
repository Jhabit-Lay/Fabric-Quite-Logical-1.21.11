package net.jhabit.qlogic.mixin;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractEquineModel.class)
public abstract class HorseModelMixin<S extends EquineRenderState> {

    @Shadow
    @Final
    protected ModelPart headParts;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/EquineRenderState;)V",
            at = @At("TAIL")
    )
    private void quitelogical$lowerHeadWhenRidden(S state, CallbackInfo ci) {
        // 1.21.11에서 스켈레톤 말은 일반 말과 다른 물리 로직을 가질 수 있습니다.
        // head가 null이 아닌지 확인하고(NPE 방지), 탑승 중일 때만 각도를 조정합니다.
        if (this.headParts != null && state.isRidden) {
            // 고개를 아래로 약 20도 숙여 시야를 확보합니다.
            this.headParts.xRot += 0.6F;
        }
    }
}