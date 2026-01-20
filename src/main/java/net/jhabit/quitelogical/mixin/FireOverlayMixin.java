package net.jhabit.quitelogical.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class FireOverlayMixin {

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void lowerFireOverlay(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        // 불꽃 텍스처를 전체적으로 아래로 내림
        // 값을 너무 많이 내리면 불이 아예 안 보일 수 있으니 -0.3f ~ -0.5f를 추천합니다.
        poseStack.translate(0.0, -0.21, 0.0);
    }
}