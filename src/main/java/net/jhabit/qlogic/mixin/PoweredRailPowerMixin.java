package net.jhabit.qlogic.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * [KR] 바닐라 및 구리 파워레일 간에 레드스톤 신호가 끊기지 않고 전달되도록 수정합니다.
 * [EN] Fixes redstone signal propagation between vanilla and copper powered rails.
 */
@Mixin(PoweredRailBlock.class)
public class PoweredRailPowerMixin {

    /**
     * [KR] 바닐라의 'isSameRailWithPower' 내부에서 자기 자신과 같은 블록인지 체크하는 로직을 가로챕니다.
     * [EN] Redirects the self-block check in 'isSameRailWithPower' to support all PoweredRailBlocks.
     */
    @Redirect(
            method = "isSameRailWithPower",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private boolean qlogic$supportCopperRailPropagation(BlockState state, Block block) {
        // [KR] 대상 블록이 PoweredRailBlock 계열이라면 전력 전달이 가능한 레일로 인정합니다.
        // [EN] Recognizes any PoweredRailBlock as compatible for power propagation.
        return state.getBlock() instanceof PoweredRailBlock;
    }
}