package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.util.CompassData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.LodestoneTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Mixin(Gui.class)
public class ExperienceBarMixin {

    @Unique
    private final Map<GlobalPos, CompassData> qlogic$targetMap = new HashMap<>();

    @Inject(method = "render", at = @At("TAIL"))
    private void injectLocatorLogic(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || client.options.hideGui) return;

        // 1. 데이터 초기화 및 수집
        qlogic$targetMap.clear();

        // 인벤토리 전체 슬롯 (0~40) 탐색
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            qlogic$collectCompassData(player.getInventory().getItem(i));
        }
        // 마우스로 잡고 있는 아이템 탐색
        if (player.containerMenu != null) {
            qlogic$collectCompassData(player.containerMenu.getCarried());
        }

        if (qlogic$targetMap.isEmpty()) return;

        // 2. 렌더링 수치 설정
        int barWidth = 182;
        int barX = (guiGraphics.guiWidth() - barWidth) / 2;
        int barY = guiGraphics.guiHeight() - 27;

        qlogic$targetMap.forEach((pos, data) -> {
            if (player.level().dimension() == pos.dimension()) {
                qlogic$renderMarker(guiGraphics, client, player, pos, data, barX, barY, barWidth);
            }
        });
    }

    @Unique
    private void qlogic$collectCompassData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        // 자석석 나침반인 경우 맵에 데이터 합산
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && tracker.target().isPresent()) {
            GlobalPos pos = tracker.target().get();
            qlogic$targetMap.merge(pos, new CompassData(stack.getHoverName(), 1), (old, val) ->
                    new CompassData(old.name(), old.count() + 1));
            return;
        }

        // 꾸러미 컴포넌트가 있다면 내부 재귀 탐색
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            for (ItemStack inner : contents.items()) {
                qlogic$collectCompassData(inner); // 꾸러미 속의 꾸러미까지 탐색 가능
            }
        }
    }

    @Unique
    private void qlogic$renderMarker(GuiGraphics graphics, Minecraft client, Player player, GlobalPos target, CompassData data, int barX, int barY, int barWidth) {
        double angle = Math.atan2(target.pos().getZ() + 0.5 - player.getZ(), target.pos().getX() + 0.5 - player.getX());
        float relativeYaw = Mth.wrapDegrees((float) (Math.toDegrees(angle) - 90.0D - player.getYRot()));

        if (Math.abs(relativeYaw) < 90.0F) {
            float xOffset = (relativeYaw / 90.0F) * (barWidth / 2.0F);
            int dotX = (int) (barX + (barWidth / 2.0F) + xOffset);

            int color = qlogic$getVibrantColor(target);

            // 5-3-1 픽셀 홀수 점 렌더링
            graphics.fill(dotX - 2, barY - 2, dotX + 3, barY + 3, 0xFF000000); // 5x5 외곽
            graphics.fill(dotX - 1, barY - 1, dotX + 2, barY + 2, color);      // 3x3 랜덤 색상
            graphics.fill(dotX, barY, dotX + 1, barY + 1, 0xFF000000);         // 1x1 중앙 검정 점

            // 이름 및 개수 표시 (중앙 8도 이내)
            if (Math.abs(relativeYaw) < 8.0F) {
                MutableComponent displayName = data.name().copy();
                if (data.count() > 1) displayName.append(" x" + data.count());

                int textWidth = client.font.width(displayName);
                int textX = dotX - (textWidth / 2);
                int textY = barY - 12;

                // 배경 박스 및 텍스트
                graphics.fill(textX - 2, textY - 2, textX + textWidth + 1, textY + 9, 0x90000000);
                graphics.drawCenteredString(client.font, displayName, dotX, textY, 0xFFFFFFFF);
            }
        }
    }

    @Unique
    private int qlogic$getVibrantColor(GlobalPos pos) {
        long seed = (long)pos.pos().getX() * 3123456L ^ (long)pos.pos().getZ() * 1234567L ^ (long)pos.pos().getY();
        Random random = new Random(seed);
        // HSB를 사용하여 색상 스펙트럼이 겹치지 않도록 분산
        return Color.HSBtoRGB(random.nextFloat(), 0.8f, 0.95f);
    }
}