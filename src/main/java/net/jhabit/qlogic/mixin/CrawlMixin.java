package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.QuiteLogicalClient;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class CrawlMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void qlogic$handleCrawlKey(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // 클라이언트 사이드 플레이어만 키보드 입력을 확인합니다.
        if (player.level().isClientSide() && player.isLocalPlayer()) {
            // 키가 눌려 있는 동안 엎드리기 (Hold 방식)
            // 토글 방식을 원하시면 별도의 변수 처리가 필요합니다.
            boolean isKeyPressed = QuiteLogicalClient.crawlKey.isDown();

            if (isKeyPressed) {
                qlogic$forceCrawl(player);
            }
        }
    }

    @Unique
    private void qlogic$forceCrawl(Player player) {
        // 공중에 있거나, 수영 중이거나, 이미 다른 자세일 때는 무시
        if (!player.isInWater() && !player.isFallFlying() && !player.isSpectator() && !player.isPassenger()) {
            // 엎드리기 포즈 강제 설정
            player.setPose(Pose.SWIMMING);

            // 엎드린 상태에서의 속도 조절 (기어가는 속도)
            if (player.onGround()) {
                // 필요 시 속도 감소 로직 추가 가능
            }
        }
    }
}