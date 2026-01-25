package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.QuiteLogicalClient;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void qlogic$updateCrawlPose(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide() && player.isLocalPlayer()) {
            // 탈것에 타고 있지 않을 때만 엎드리기 포즈를 시도합니다.
            if (qlogic$shouldCrawl() && !player.isPassenger()) {
                if (!player.isInWater() && !player.isFallFlying()) {
                    player.setPose(Pose.SWIMMING);
                }
            }
        }
    }


    @Inject(method = "isSwimming", at = @At("HEAD"), cancellable = true)
    private void qlogic$forceVisualCrawl(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide() && player.isLocalPlayer()) {
            // 1. 탈것에 타고 있다면 절대로 '수영 중' 판정을 주지 않습니다.
            if (player.isPassenger()) {
                return; // 바닐라 로직을 따르게 둠
            }

            // 2. 엎드리기 모드일 때만 true 반환
            if (qlogic$shouldCrawl() && !player.isInWater() && !player.isFallFlying()) {
                cir.setReturnValue(true);
            }
        }
    }

    private boolean qlogic$shouldCrawl() {
        // 탑승 중일 때는 엎드리기 판정 자체를 무효화 (가장 확실한 방법)
        Player player = (Player) (Object) this;
        if (player.isPassenger()) return false;

        if (player.isPassenger()) {
            QuiteLogicalClient.isCrawlingToggled = false; // 타는 순간 토글을 강제로 끔
            return false;
        }

        if (QuiteLogicalClient.crawlToggleOption.get()) {
            return QuiteLogicalClient.isCrawlingToggled;
        }
        return QuiteLogicalClient.crawlKey.isDown();


    }
}