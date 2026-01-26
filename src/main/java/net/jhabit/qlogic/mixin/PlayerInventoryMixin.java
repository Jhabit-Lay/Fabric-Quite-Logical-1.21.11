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
     * [업무 처리] 1.21.11 꾸러미 시스템에 맞춘 자동 아이템 수집 로직
     */
    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onAdd(ItemStack pickedUpStack, CallbackInfoReturnable<Boolean> cir) {
        if (pickedUpStack.isEmpty()) return;

        Inventory inventory = (Inventory) (Object) this;

        // 1 & 2. 인벤토리를 순회하며 꾸러미(Bundle)가 있는지 확인
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);

            if (slotStack.getItem() instanceof BundleItem) {
                // 3 & 4. 꾸러미 내용물(BundleContents) 및 용량 확인
                BundleContents contents = slotStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

                // 5. 중복된 아이템이 꾸러미 안에 있는지 확인 (Record의 items() 리스트 활용)
                boolean hasDuplicate = false;
                for (ItemStack innerStack : contents.items()) {
                    if (ItemStack.isSameItemSameComponents(innerStack, pickedUpStack)) {
                        hasDuplicate = true;
                        break;
                    }
                }

                // 중복된 아이템이 있는 꾸러미를 찾았을 때만 실행
                if (hasDuplicate) {
                    // 1.21.11 핵심: Mutable 객체를 생성하여 수정을 준비함
                    BundleContents.Mutable mutable = new BundleContents.Mutable(contents);

                    // 6. tryInsert를 통해 용량이 허용하는 만큼 삽입 시도
                    // 이 메서드는 내부적으로 용량을 계산하고 pickedUpStack의 개수를 줄임
                    int insertedCount = mutable.tryInsert(pickedUpStack);

                    if (insertedCount > 0) {
                        // 변경된 내용을 다시 Immutable(Record)로 변환하여 아이템에 적용
                        slotStack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());

                        // 아이템이 꾸러미에 모두 들어갔다면(개수가 0이 됨) 원래 로직 중단 및 성공 반환
                        if (pickedUpStack.isEmpty()) {
                            cir.setReturnValue(true);
                            return;
                        }
                        // 아직 아이템이 남아있다면(가득 참), 다음 중복 아이템이 있는 꾸러미를 계속 찾음
                    }
                }
            }
        }

        // 7. 중복 아이템이 없거나 모든 꾸러미가 가득 차서 들어갈 공간이 없는 경우,
        // 자연스럽게 원래의 인벤토리 삽입 로직(Default)으로 진행됨
    }
}