package net.jhabit.qlogic.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class TorchflowerLightMixin {

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void makeTorchflowerGlow(CallbackInfoReturnable<Integer> cir) {
        // 현재 블록 상태를 객체로 가져옴
        BlockState state = (BlockState) (Object) this;

        // only fully grown flower can glow
        if (state.is(Blocks.TORCHFLOWER)) {
            cir.setReturnValue(12);
        }
    }
}