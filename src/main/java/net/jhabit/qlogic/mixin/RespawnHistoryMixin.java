package net.jhabit.qlogic.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData.RespawnData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class RespawnHistoryMixin extends Player {

    @Unique private static final Logger MOD_LOGGER = LogUtils.getLogger();
    @Shadow @Final private MinecraftServer server;
    @Shadow public abstract void setRespawnPosition(ServerPlayer.RespawnConfig respawnConfig, boolean bl);

    @Shadow
    public abstract void sendSystemMessage(Component component);

    @Unique private final List<GlobalPos> respawnHistory = new ArrayList<>();
    @Unique private static final int MAX_SLOTS = 3;
    @Unique private boolean isUpdatingInternally = false;

    public RespawnHistoryMixin(net.minecraft.world.level.Level level, com.mojang.authlib.GameProfile profile) {
        super(level, profile);
    }

    @Invoker("findRespawnAndUseSpawnBlock")
    public static Optional<ServerPlayer.RespawnPosAngle> callFindSafePos(ServerLevel level, ServerPlayer.RespawnConfig config, boolean bl) {
        throw new AssertionError();
    }

    /**
     * [슬롯 시프팅 로직]
     * 새로운 침대 등록 시: 0번 삭제 -> 1이 0으로, 2가 1로 이동 -> 새 좌표가 2번에 저장
     */
    @Inject(method = "setRespawnPosition", at = @At("HEAD"))
    private void onSetRespawnPosition(ServerPlayer.RespawnConfig config, boolean sendMessage, CallbackInfo ci) {
        if (config == null || isUpdatingInternally) return;
        GlobalPos newPos = config.respawnData().globalPos();

        // 2번 슬롯(최신)과 동일하면 덮어씌우기(무시) 로직
        if (!respawnHistory.isEmpty()) {
            GlobalPos latest = respawnHistory.get(respawnHistory.size() - 1);
            if (latest.equals(newPos)) return;
        }

        // 3개 꽉 찼으면 0번(가장 오래된 것) 삭제 (자동으로 인덱스 1, 2가 0, 1로 당겨짐)
        if (respawnHistory.size() >= MAX_SLOTS) {
            respawnHistory.remove(0);
        }

        respawnHistory.add(newPos);
        MOD_LOGGER.info("새 침대 등록 (2번 슬롯): {} | 현재 슬롯 수: {}", newPos.pos().toShortString(), respawnHistory.size());
    }

    /**
     * 죽었을 때 이전 히스토리를 새 플레이어 객체로 인계하는 핵심 메서드
     */
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void onRestoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        RespawnHistoryMixin oldMixin = (RespawnHistoryMixin)(Object)oldPlayer;
        this.respawnHistory.clear();
        this.respawnHistory.addAll(oldMixin.respawnHistory);
        MOD_LOGGER.info("리스폰 히스토리 데이터 인계 완료 ({}개)", this.respawnHistory.size());
    }

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("RETURN"), cancellable = true)
    private void onFallbackRespawn(boolean bl, TeleportTransition.PostTeleportTransition post, CallbackInfoReturnable<TeleportTransition> cir) {
        if (cir.getReturnValue().missingRespawnBlock()) {
            // 역순(2 -> 1 -> 0)으로 탐색
            for (int i = respawnHistory.size() - 1; i >= 0; i--) {
                GlobalPos candidate = respawnHistory.get(i);
                ServerLevel targetLevel = this.server.getLevel(candidate.dimension());

                if (targetLevel != null) {
                    RespawnData data = RespawnData.of(candidate.dimension(), candidate.pos(), 0.0F, 0.0F);
                    ServerPlayer.RespawnConfig testConfig = new ServerPlayer.RespawnConfig(data, false);

                    // 하드코딩된 좌표 대신 바닐라의 안전 좌표 계산 로직 사용
                    Optional<ServerPlayer.RespawnPosAngle> result = callFindSafePos(targetLevel, testConfig, bl);

                    if (result.isPresent()) {
                        ServerPlayer.RespawnPosAngle angle = result.get();

                        this.isUpdatingInternally = true;
                        this.setRespawnPosition(testConfig, false);
                        this.isUpdatingInternally = false;

                        this.sendSystemMessage(Component.translatable("chat.qlogic.respawn_fallback"));

                        // angle.position() 등을 사용하여 "막힘" 에러 방지
                        cir.setReturnValue(new TeleportTransition(targetLevel, angle.position(), Vec3.ZERO, angle.yaw(), angle.pitch(), post));
                        MOD_LOGGER.info("침대 복구 성공! 슬롯 {}번 침대에서 부활합니다.", i);
                        return;
                    } else {
                        respawnHistory.remove(i);
                    }
                }
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveRespawnHistory(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.store("RespawnHistory", GlobalPos.CODEC.listOf(), this.respawnHistory);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadRespawnHistory(ValueInput valueInput, CallbackInfo ci) {
        this.respawnHistory.clear();
        valueInput.read("RespawnHistory", GlobalPos.CODEC.listOf()).ifPresent(this.respawnHistory::addAll);
    }
}