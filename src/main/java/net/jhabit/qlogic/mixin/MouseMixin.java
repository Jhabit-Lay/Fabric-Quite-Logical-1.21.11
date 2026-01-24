package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.util.SpyglassZoomManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    /**
     * 사용자 제공 소스 코드 반영: onScroll 메서드를 가로채 줌 기능을 동작시킵니다.
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        // 망원경 사용 중일 때만 휠 스크롤을 줌 조절로 사용하고, 기본 스크롤(슬롯 변경)은 차단
        if (client.player != null && client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS)) {
            SpyglassZoomManager.onScroll(vertical);
            ci.cancel(); // 줌 조절 중 아이템 슬롯이 바뀌는 것을 방지
        }
    }
}