package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
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
 * 1. 시그니처 수정을 통해 NewMinecartBehavior에 정확히 로직 주입
 * 2. 산화 단계별 속도 수치 조정 (1.1D 기준 2배 차이 적용)
 */
@Mixin(targets = {
        "net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior",
        "net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior"
})
public abstract class MinecartBehaviorMixin {

    @Redirect(
            method = "*",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private boolean qlogic$supportAllPoweredRails(BlockState state, net.minecraft.world.level.block.Block block) {
        if (block == Blocks.POWERED_RAIL) {
            return state.getBlock() instanceof PoweredRailBlock;
        }
        return state.is(block);
    }

    /**
     * [KR] 속도 제한치를 결정합니다. 1.1D(최고)와 0.55D(최저)로 2배 차이를 둡니다.
     */
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void qlogic$overrideMaxSpeedWithAbsoluteValues(CallbackInfoReturnable<Double> cir) {
        AbstractMinecart minecart = qlogic$getMinecart();
        if (minecart == null) return;

        BlockPos pos = minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = minecart.level().getBlockState(pos);

        if (state.getBlock() instanceof PoweredRailBlock) {
            boolean isPowered = state.getValue(PoweredRailBlock.POWERED);

            if (isPowered) {
                // [KR] 속도 수치 재조정: 최고속도 1.1D, 최저속도 0.55D (2배 차이)
                // [EN] Adjusted values: Max 1.1D, Min 0.55D (2x difference)
                if (state.is(ModBlocks.EXPOSED_POWERED_RAIL) || state.is(ModBlocks.WAXED_EXPOSED_POWERED_RAIL)) {
                    cir.setReturnValue(0.8D);
                } else if (state.is(ModBlocks.WEATHERED_POWERED_RAIL) || state.is(ModBlocks.WAXED_WEATHERED_POWERED_RAIL)) {
                    cir.setReturnValue(0.6D);
                } else if (state.is(ModBlocks.OXIDIZED_POWERED_RAIL) || state.is(ModBlocks.WAXED_OXIDIZED_POWERED_RAIL)) {
                    cir.setReturnValue(0.4D); // 최고속도 1.1D의 정확히 절반
                } else {
                    // [KR] 바닐라 파워레일 및 깨끗한 구리 레일
                    cir.setReturnValue(1.0D);
                }
                return;
            }
        }

        // [KR] 전원이 꺼진 파워레일 혹은 일반 레일 (기본 브레이크 로직)
        cir.setReturnValue(0.4D);
    }

    @Unique
    private AbstractMinecart qlogic$getMinecart() {
        try {
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