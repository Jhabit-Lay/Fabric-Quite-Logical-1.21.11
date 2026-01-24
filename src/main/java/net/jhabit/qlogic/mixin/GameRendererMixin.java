package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.util.SpyglassZoomManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void modifySpyglassFov(CallbackInfoReturnable<Float> cir) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS)) {
            // 매 프레임 줌 수치를 목표값으로 부드럽게 갱신합니다.
            SpyglassZoomManager.update();

            float baseFov = cir.getReturnValue();
            // 부드럽게 변하는 currentZoom 값을 적용합니다.
            float zoomedFov = (float) (baseFov / SpyglassZoomManager.getZoomLevel());
            cir.setReturnValue(zoomedFov);
        } else {
            SpyglassZoomManager.reset();
        }
    }
}