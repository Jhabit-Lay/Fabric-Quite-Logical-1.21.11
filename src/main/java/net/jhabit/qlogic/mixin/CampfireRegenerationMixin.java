package net.jhabit.qlogic.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Player.class)
public abstract class CampfireRegenerationMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void qlogic$updateCampfireRegen(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // 서버 사이드 로직 확인
        if (player.level().isClientSide()) return;

        // 2틱마다 검사
        if (player.tickCount % 2 == 0) {
            boolean hasCampfire = qlogic$findLitCampfire(player);

            if (hasCampfire) {
                // [해결] StatusEffectInstance -> MobEffectInstance
                // [해결] StatusEffects.REGENERATION -> MobEffects.REGENERATION
                player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, 40, 0, true, false, true
                ));
            } else {
                // [해결] 변수 타입 'MobEffectInstance' 명시
                MobEffectInstance currentRegen = player.getEffect(MobEffects.REGENERATION);

                // 모닥불용 효과인지 확인 후 제거
                if (currentRegen != null && currentRegen.isAmbient() && currentRegen.getDuration() <= 40) {
                    player.removeEffect(MobEffects.REGENERATION);
                }
            }
        }
    }

    @Unique
    private boolean qlogic$findLitCampfire(Player player) {
        BlockPos playerPos = player.blockPosition();

        Optional<BlockPos> campfirePos = BlockPos.findClosestMatch(playerPos, 5, 5, pos -> {
            BlockState state = player.level().getBlockState(pos);
            return state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT);
        });

        return campfirePos.isPresent();
    }
}