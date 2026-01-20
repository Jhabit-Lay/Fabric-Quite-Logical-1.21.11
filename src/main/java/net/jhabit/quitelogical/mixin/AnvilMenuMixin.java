package net.jhabit.quitelogical.mixin;

import net.jhabit.quitelogical.items.CopperGoatHornItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    // createResult: 모루에서 왼쪽 두 칸에 아이템을 놓았을 때 결과물을 계산하는 메서드
    @Inject(method = "createResult", at = @At("TAIL"))
    private void quitelogical$removeRepairPenaltyOnRepair(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;

        // 결과 슬롯(2번 슬롯)에 있는 아이템을 가져옵니다.
        ItemStack resultStack = menu.getSlot(2).getItem();

        // 결과물이 우리가 만든 구리 염소 뿔이라면
        if (!resultStack.isEmpty() && resultStack.getItem() instanceof CopperGoatHornItem) {
            // 수리 비용 패널티를 강제로 0으로 설정합니다.
            // 이렇게 하면 모루에서 꺼내는 순간 패널티가 0인 상태가 됩니다.
            resultStack.set(DataComponents.REPAIR_COST, 0);
        }
    }
}