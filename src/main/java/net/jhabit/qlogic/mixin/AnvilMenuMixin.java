package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.util.ForgeHelper;
import net.jhabit.qlogic.items.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow @Final private DataSlot cost;
    @Shadow public abstract void createResult();

    @Unique
    private long lastSoundTick = -1;

    public AnvilMenuMixin(int i, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.inventory.ContainerLevelAccess acc) {
        super(null, i, inv, acc, null);
    }

    // [1] 결과창 생성 로직 (기존과 동일)
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void quitelogical$priorityCarbonizedIronRecipe(CallbackInfo ci) {
        ItemCombinerMenuAccessor acc = (ItemCombinerMenuAccessor) this;
        if (ForgeHelper.isSteelRecipe(acc.getInputSlots().getItem(0), acc.getInputSlots().getItem(1))) {
            acc.getResultSlots().setItem(0, new ItemStack(ModItems.CARBONIZED_IRON, 1));
            this.cost.set(0);
            this.broadcastChanges();
            ci.cancel();
        }
    }

    // [2] 집기 허용 (기존과 동일)
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    public void quitelogical$allowPick(Player player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        ItemCombinerMenuAccessor acc = (ItemCombinerMenuAccessor) this;
        if (ForgeHelper.isSteelRecipe(acc.getInputSlots().getItem(0), acc.getInputSlots().getItem(1))) {
            cir.setReturnValue(true);
        }
    }

    // [3] 터보 제련 로직: 딜레이를 제거함
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void quitelogical$handleTake(Player player, ItemStack stack, CallbackInfo ci) {
        ItemCombinerMenuAccessor acc = (ItemCombinerMenuAccessor) this;
        ItemStack left = acc.getInputSlots().getItem(0);
        ItemStack right = acc.getInputSlots().getItem(1);

        if (ForgeHelper.isSteelRecipe(left, right)) {
            // 1. 즉시 재료 소모
            left.shrink(1);
            right.shrink(1);

            // 2. [딜레이 제거의 핵심] 결과 슬롯을 즉시 비움 처리
            acc.getResultSlots().setItem(0, ItemStack.EMPTY);



            // 3. 다음 틱을 기다리지 않고 즉시 다음 아이템 계산
            this.createResult();

            // 4. 변화된 내용을 클라이언트에 즉시 전송 (패킷 강제 동기화)
            this.broadcastChanges();

            // 5. 소리는 서버 부하를 줄이기 위해 별도 실행 (선택 사항)
            acc.getAccess().execute((level, pos) -> {
                long currentTick = level.getGameTime();
                // 현재 틱이 마지막 재생 틱과 다를 때만 소리를 재생합니다.
                if (currentTick != lastSoundTick) {
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_USE,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                    lastSoundTick = currentTick; // 재생 시간 업데이트
                }

                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FIREWORK,
                        pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        3, 0.1, 0.1, 0.1, 0.05);
                    }
                });
            ci.cancel();
        }
    }
}