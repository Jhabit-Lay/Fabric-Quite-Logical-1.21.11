package net.jhabit.qlogic.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    /**
     * [KR] 1.21.11 서버/클라이언트 공용 아이템 수집 로직 보강
     * [EN] Enhanced item collection logic for 1.21.11 Server/Client
     */
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void qlogic$onAddWithSlot(int slot, ItemStack pickedUpStack, CallbackInfoReturnable<Boolean> cir) {
        if (pickedUpStack.isEmpty()) return;

        Inventory inventory = (Inventory) (Object) this;

        // [KR] 1차 순회: 동일한 아이템이 이미 들어있는 꾸러미를 먼저 채웁니다 (Tidy Mode)
        // [EN] 1st pass: Fill bundles that already contain the same item (Tidy Mode)
        if (qlogic$tryFillBundles(inventory, pickedUpStack, true)) {
            if (pickedUpStack.isEmpty()) {
                cir.setReturnValue(true);
                return;
            }
        }

        // [KR] 2차 순회: 남은 아이템을 빈 공간이 있는 아무 꾸러미에나 순서대로 넣습니다 (Greedy Mode)
        // [EN] 2nd pass: Fill any available bundles with space (Greedy Mode)
        if (qlogic$tryFillBundles(inventory, pickedUpStack, false)) {
            if (pickedUpStack.isEmpty()) {
                cir.setReturnValue(true);
                return;
            }
        }

        // [KR] 이후 과정은 자연스럽게 원래의 인벤토리 삽입 로직(Default)으로 진행됨
    }

    /**
     * [KR] 꾸러미 삽입 처리 공통 로직
     */
    private boolean qlogic$tryFillBundles(Inventory inventory, ItemStack targetStack, boolean mustMatch) {
        boolean modified = false;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);

            if (slotStack.getItem() instanceof BundleItem) {
                BundleContents contents = slotStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

                // [KR] 중복 체크 모드일 경우 내용물 확인
                if (mustMatch) {
                    boolean hasDuplicate = false;
                    for (ItemStack inner : contents.items()) {
                        if (ItemStack.isSameItemSameComponents(inner, targetStack)) {
                            hasDuplicate = true;
                            break;
                        }
                    }
                    if (!hasDuplicate) continue;
                }

                // [KR] Mutable 객체를 통한 삽입 시도
                BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
                int inserted = mutable.tryInsert(targetStack);

                if (inserted > 0) {
                    slotStack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
                    modified = true;
                    if (targetStack.isEmpty()) break;
                }
            }
        }
        return modified;
    }
}