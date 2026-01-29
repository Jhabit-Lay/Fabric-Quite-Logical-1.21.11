package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends Entity {

    // [KR] 현재 마인카트가 있는 위치를 계산하는 내부 메서드 가져오기
    @Shadow public abstract BlockPos getCurrentBlockPosOrRailBelow();

    public AbstractMinecartMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /**
     * [KR] 마인카트가 레일을 지날 때 산화 정도에 따라 속도를 조절합니다.
     * [EN] Adjusts minecart speed based on rail oxidation level.
     */
    @Inject(method = "moveAlongTrack", at = @At("TAIL"))
    private void qlogic$applyCopperRailSpeed(ServerLevel serverLevel, CallbackInfo ci) {
        // [KR] 현재 위치와 블록 상태 가져오기
        BlockPos pos = this.getCurrentBlockPosOrRailBelow();
        BlockState state = serverLevel.getBlockState(pos);

        // [KR] 파워레일이 활성화(Powered)된 상태인지 확인
        if (state.getBlock() instanceof PoweredRailBlock && state.getValue(PoweredRailBlock.POWERED)) {
            double multiplier = 1.0D;

            // [KR] 산화 단계에 따른 속도 멀티플라이어 적용
            if (state.is(ModBlocks.EXPOSED_POWERED_RAIL) || state.is(ModBlocks.WAXED_EXPOSED_POWERED_RAIL)) {
                multiplier = 0.8D;
            } else if (state.is(ModBlocks.WEATHERED_POWERED_RAIL) || state.is(ModBlocks.WAXED_WEATHERED_POWERED_RAIL)) {
                multiplier = 0.6D;
            } else if (state.is(ModBlocks.OXIDIZED_POWERED_RAIL) || state.is(ModBlocks.WAXED_OXIDIZED_POWERED_RAIL)) {
                multiplier = 0.4D;
            }

            // [KR] 감속이 필요한 경우 현재 운동량에 곱함
            if (multiplier < 1.0D) {
                Vec3 velocity = this.getDeltaMovement();
                // [KR] Y축(중력)은 건드리지 않고 X, Z축 이동 속도만 제어
                this.setDeltaMovement(velocity.x * multiplier, velocity.y, velocity.z * multiplier);
            }
        }
    }
}