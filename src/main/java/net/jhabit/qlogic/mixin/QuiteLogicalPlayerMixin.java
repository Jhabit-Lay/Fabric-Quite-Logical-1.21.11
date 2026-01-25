package net.jhabit.qlogic.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jhabit.qlogic.QuiteLogical;
import net.jhabit.qlogic.QuiteLogicalClient;
import net.jhabit.qlogic.network.CrawlPayload;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class QuiteLogicalPlayerMixin extends Entity {

    public QuiteLogicalPlayerMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void qlogic$defineCrawlData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        // ✅ QuiteLogical에 선언된 공용 키를 사용합니다.
        builder.define(QuiteLogical.QLOGIC$CRAWLING, false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void qlogic$handleCrawlSync(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide() && player.isLocalPlayer()) {
            boolean shouldCrawl = qlogic$calculateCrawlState(player);

            // ✅ QuiteLogical.QLOGIC$CRAWLING 참조
            if (player.getEntityData().get(QuiteLogical.QLOGIC$CRAWLING) != shouldCrawl) {
                player.getEntityData().set(QuiteLogical.QLOGIC$CRAWLING, shouldCrawl);
                ClientPlayNetworking.send(new CrawlPayload(shouldCrawl));
                player.refreshDimensions();
            }
        }
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void qlogic$overridePose(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        // ✅ QuiteLogical.QLOGIC$CRAWLING 참조
        if (player.getEntityData().get(QuiteLogical.QLOGIC$CRAWLING)) {
            if (!player.isPassenger() && !player.isInWater() && !player.isFallFlying()) {
                player.setPose(Pose.SWIMMING);
                ci.cancel();
            }
        }
    }

    @Inject(method = "isSwimming", at = @At("HEAD"), cancellable = true)
    private void qlogic$forceAnimation(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        // ✅ QuiteLogical.QLOGIC$CRAWLING 참조
        if (player.getEntityData().get(QuiteLogical.QLOGIC$CRAWLING) && !player.isInWater() && !player.isPassenger()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean qlogic$calculateCrawlState(Player player) {
        if (player.isPassenger()) return false;
        if (QuiteLogicalClient.crawlToggleOption.get()) {
            return QuiteLogicalClient.isCrawlingToggled;
        }
        return QuiteLogicalClient.crawlKey.isDown();
    }
}