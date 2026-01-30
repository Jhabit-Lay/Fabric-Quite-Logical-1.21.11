package net.jhabit.qlogic.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    /**
     * [KR] 아이템 습득 시 인벤토리에 넣기 전 꾸러미(Bundle) 확인 로직
     * [EN] Check bundles before adding items to inventory slots
     */
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void qlogic$onAddWithSlot(int slot, ItemStack pickedUpStack, CallbackInfoReturnable<Boolean> cir) {
        if (pickedUpStack.isEmpty()) return;

        Inventory inventory = (Inventory) (Object) this;

        // [KR] 동일한 아이템이 이미 들어있는 꾸러미가 있는지 확인하고 채웁니다 (Tidy Mode 전용)
        // [EN] Only fill bundles that already contain the same item (Tidy Mode only)
        qlogic$tryFillBundles(inventory, pickedUpStack);

        // [KR] 만약 아이템이 꾸러미에 모두 들어갔다면, 바닐라 로직을 실행하지 않고 종료
        // [EN] If the stack is fully consumed by bundles, cancel vanilla logic
        if (pickedUpStack.isEmpty()) {
            cir.setReturnValue(true);
        }

        // [KR] 아이템이 남아있거나 중복된 꾸러미가 없다면 여기서 메서드가 종료되어
        // 자연스럽게 바닐라의 인벤토리 삽입 로직이 실행됩니다.
    }

    /**
     * [KR] 인벤토리 내의 꾸러미들을 순회하며 중복 아이템 삽입을 시도합니다.
     */
    @Unique
    private void qlogic$tryFillBundles(Inventory inventory, ItemStack targetStack) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);

            // 1. 해당 슬롯이 꾸러미인지 확인
            if (slotStack.getItem() instanceof BundleItem) {
                BundleContents contents = slotStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

                // 2. 꾸러미 내부에 동일한 아이템이 있는지 확인 (중복 체크)
                boolean hasDuplicate = false;
                for (ItemStack inner : contents.items()) {
                    if (ItemStack.isSameItemSameComponents(inner, targetStack)) {
                        hasDuplicate = true;
                        break;
                    }
                }

                // 3. 중복 아이템이 있는 경우에만 삽입 시도
                if (hasDuplicate) {
                    BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
                    int inserted = mutable.tryInsert(targetStack);

                    if (inserted > 0) {
                        slotStack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
                        // 아이템을 다 넣었다면 루프 종료
                        if (targetStack.isEmpty()) break;
                    }
                }
            }
        }
    }
}