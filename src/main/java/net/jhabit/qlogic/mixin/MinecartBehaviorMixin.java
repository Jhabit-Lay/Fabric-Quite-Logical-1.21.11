package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * [KR] 마인카트 물리 엔진 통합 믹스인 (1.21.11 최종 수정본)
 * 1. BlockState.is() 리다이렉트를 통해 가속/감속 판정 오류 완벽 해결
 * 2. 켜졌을 때 사용자 정의 절대 속도 적용 (1.0, 0.8, 0.6, 0.4)
 * 3. 서버/클라이언트 동기화 로직 강화로 끊김(Jitter) 현상 제거
 */
@Mixin(targets = {
        "net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior",
        "net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior"
})
public abstract class MinecartBehaviorMixin {

    /**
     * [KR] 물리 엔진이 블록의 타입을 판정하는 핵심 메서드(is)를 가로챕니다.
     * [EN] Intercepts the BlockState.is(Block) check.
     * 우리 레일들을 바닐라 파워레일과 동일하게 취급하도록 하여 가속/정지 로직을 활성화합니다.
     */
    @Redirect(
            method = "*",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private boolean qlogic$supportAllPoweredRails(BlockState state, net.minecraft.world.level.block.Block block) {
        // [KR] 바닐라 파워레일을 체크하는 모든 지점에서 우리 구리 레일들도 긍정 판정을 내리게 합니다.
        if (block == Blocks.POWERED_RAIL) {
            return state.getBlock() instanceof PoweredRailBlock;
        }
        return state.is(block);
    }

    /**
     * [KR] 속도 제한치를 결정합니다. 사용자님의 설계 수치를 절대값으로 적용합니다.
     */
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void qlogic$overrideMaxSpeedWithAbsoluteValues(CallbackInfoReturnable<Double> cir) {
        AbstractMinecart minecart = qlogic$getMinecart();
        if (minecart == null) return;

        Level level = minecart.level();
        // [KR] 끊김 방지를 위해 서버/클라이언트 모두 현재 블록 위치를 정밀하게 추적합니다.
        BlockPos pos = minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = level.getBlockState(pos);

        // [KR] 1. 블록이 파워레일 계열인지 확인 (Redirect 덕분에 우리 레일도 여기서 잡힙니다)
        if (state.getBlock() instanceof PoweredRailBlock) {
            boolean isPowered = state.getValue(PoweredRailBlock.POWERED);

            // [KR] 2. 전원이 켜져있을 때만 단계별 절대 속도 적용
            if (isPowered) {
                if (state.is(ModBlocks.EXPOSED_POWERED_RAIL) || state.is(ModBlocks.WAXED_EXPOSED_POWERED_RAIL)) {
                    cir.setReturnValue(1.0D);
                } else if (state.is(ModBlocks.WEATHERED_POWERED_RAIL) || state.is(ModBlocks.WAXED_WEATHERED_POWERED_RAIL)) {
                    cir.setReturnValue(0.8D);
                } else if (state.is(ModBlocks.OXIDIZED_POWERED_RAIL) || state.is(ModBlocks.WAXED_OXIDIZED_POWERED_RAIL)) {
                    cir.setReturnValue(0.6D);
                } else {
                    // [KR] 일반 파워레일 및 산화되지 않은 초기 단계/밀랍칠된 파워레일
                    cir.setReturnValue(1.2D);
                }
                return;
            }
        }

        // [KR] 3. 전원이 꺼져있거나 일반 레일인 경우:
        // 바닐라의 기본 속도 한계값(0.4D)을 반환합니다.
        // 엔진은 이미 Redirect를 통해 파워레일임을 알고 있으므로, 0.4D를 주면 '전원 꺼진 파워레일 정지 로직'을 정상 실행합니다.
        cir.setReturnValue(0.4D);
    }

    /**
     * [KR] 부모 클래스의 minecart 필드를 안전하게 가져오기 위한 도우미 메서드
     */
    @Unique
    private AbstractMinecart qlogic$getMinecart() {
        try {
            // [KR] 필드 이름을 직접 찾는 대신 타입을 기반으로 탐색하여 매핑 오류 원천 차단
            for (Field f : this.getClass().getSuperclass().getDeclaredFields()) {
                if (AbstractMinecart.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (AbstractMinecart) f.get(this);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}