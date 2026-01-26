package net.jhabit.qlogic.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Item.class)
public abstract class ItemTooltipMixin {

    /**
     * [업무 처리] translatable component를 사용하여 국가별 언어 대응이 가능하도록 구현했습니다.
     */
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void addBundleFullnessTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag tooltipFlag, CallbackInfo ci) {

        if (!((Object) this instanceof BundleItem)) {
            return;
        }

        BundleContents contents = itemStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

        // 1.21.11 분수 값을 정수형 무게로 변환
        int currentWeight = (int) (contents.weight().floatValue() * 64);
        int maxWeight = 64;

        // 색상 결정 (가득 차면 빨간색, 아니면 금색)
        ChatFormatting valueColor = (currentWeight >= maxWeight) ? ChatFormatting.RED : ChatFormatting.GOLD;

        // 번역 키 사용: text.qlogic.bundle_capacity
        // 매개변수 %s 위치에 들어갈 값들을 순서대로 전달합니다.
        Component fullnessText = Component.translatable("text.qlogic.bundle_capacity",
                        Component.literal(String.valueOf(currentWeight)).withStyle(valueColor),
                        Component.literal(String.valueOf(maxWeight)).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY); // 전체적인 문구는 회색 스타일 적용

        consumer.accept(fullnessText);
    }
}