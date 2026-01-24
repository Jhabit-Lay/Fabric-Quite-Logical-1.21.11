package net.jhabit.qlogic.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jhabit.qlogic.CompassData;
import net.jhabit.qlogic.QuiteLogicalClient;
import net.jhabit.qlogic.network.PingPayload;
import net.jhabit.qlogic.util.CompassManager;
import net.jhabit.qlogic.util.SpyglassZoomManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;
import java.util.Random;

@Mixin(Gui.class)
public class ExperienceBarMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void injectAllHUDLogic(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || client.options.hideGui) return;

        // 1. 데이터 관리: 나침반 및 핑 데이터 갱신
        CompassManager.targetMap.entrySet().removeIf(entry -> entry.getValue().expiryTime() == -1L);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            qlogic$collectCompassData(player.getInventory().getItem(i));
        }
        if (player.containerMenu != null) qlogic$collectCompassData(player.containerMenu.getCarried());

        // 2. 경험치 바 위 도트 렌더링
        int barWidth = 182;
        int barX = (guiGraphics.guiWidth() - barWidth) / 2;
        int barY = guiGraphics.guiHeight() - 27;

        CompassManager.targetMap.forEach((pos, data) -> {
            if (player.level().dimension().equals(pos.dimension())) {
                qlogic$renderMarker(guiGraphics, client, player, pos, data, barX, barY, barWidth);
            }
        });

        // 3. 분석 HUD (작아진 텍스트 및 벌집 정보 포함)
        qlogic$renderSpyglassHUD(guiGraphics, client, deltaTracker);
    }

    @Unique
    private void qlogic$renderSpyglassHUD(GuiGraphics graphics, Minecraft client, DeltaTracker delta) {
        if (!(client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS))) return;

        float partialTicks = delta.getGameTimeDeltaPartialTick(false);
        Entity camera = client.getCameraEntity();
        if (camera == null) return;

        double range = 128.0D;
        Vec3 start = camera.getEyePosition(partialTicks);
        Vec3 look = camera.getViewVector(partialTicks);
        Vec3 end = start.add(look.x * range, look.y * range, look.z * range);

        HitResult hit = client.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, camera));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(camera, start, end, camera.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D), e -> !e.isSpectator() && e.isPickable(), hit.getLocation().distanceToSqr(start));
        if (entityHit != null) hit = entityHit;
        if (hit.getType() == HitResult.Type.MISS) return;

        int centerX = graphics.guiWidth() / 2;
        int screenHeight = graphics.guiHeight();

        // [크기 조정] 상단 좌표 및 거리는 0.75f로 작게 표시
        String coords = String.format("X: %.1f / Y: %.1f / Z: %.1f", hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        qlogic$drawInfoBox(graphics, client, coords, centerX, 10, 0xFFFFFFFF, 0.75f);

        double dist = start.distanceTo(hit.getLocation());
        qlogic$drawInfoBox(graphics, client, String.format("%.1fm", dist), centerX, 19, 0xFF55FFFF, 0.75f);

        // 3단 상세 분석 (0.75f 적용)
        if (hit instanceof EntityHitResult entHit && entHit.getEntity() instanceof AbstractHorse horse) {
            String speed = String.format("%.1f", horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17);
            String jump = String.format("%.1f", horse.getAttributeValue(Attributes.JUMP_STRENGTH));
            String stats = Component.translatable("text.qlogic.horse_stats", (int) horse.getHealth(), (int) horse.getMaxHealth(), speed, jump).getString();
            qlogic$drawInfoBox(graphics, client, stats, centerX, 30, 0xFFFFFFFF, 0.75f);
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockPos blockPos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(blockPos);

            if (state.is(BlockTags.BEEHIVES)) {
                int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
                int bees = 0;

                if (client.level.getBlockEntity(blockPos) instanceof BeehiveBlockEntity beehive) {
                    bees = beehive.getOccupantCount();
                }
                String beeInfo = Component.translatable("text.qlogic.beehive_info", bees, honey).getString();
                qlogic$drawInfoBox(graphics, client, beeInfo, centerX, 30, 0xFFFFFF55, 0.75f);
            }
        }

        // [크기/위치 조정] 줌 텍스트는 0.85f로, 하트 위(screenHeight - 60)에 배치
        String zoomText = String.format("Zoom: %.1fx", SpyglassZoomManager.getZoomLevel());
        qlogic$drawInfoBox(graphics, client, zoomText, centerX, screenHeight - 60, 0xFFFFAA00, 0.85f);
    }

    /**
     * [해결] 1.21.11 Matrix3x2fStack 전용 렌더링 로직
     */
    @Unique
    private void qlogic$drawInfoBox(GuiGraphics graphics, Minecraft client, String text, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix(); // 1.21.11 UI 전용

        // 2D 행렬이므로 float 타입의 X, Y만 받습니다.
        graphics.pose().translate((float)x, (float)y);
        graphics.pose().scale(scale, scale);

        int width = client.font.width(text);
        graphics.fill(-(width / 2) - 3, -2, (width / 2) + 3, 10, 0x90000000);
        graphics.drawCenteredString(client.font, text, 0, 0, color);

        graphics.pose().popMatrix();
    }

    @Unique
    private void qlogic$renderMarker(GuiGraphics graphics, Minecraft client, Player player, GlobalPos target, CompassData data, int barX, int barY, int barWidth) {
        double angle = Math.atan2(target.pos().getZ() + 0.5 - player.getZ(), target.pos().getX() + 0.5 - player.getX());
        float relYaw = Mth.wrapDegrees((float) (Math.toDegrees(angle) - 90.0D - player.getYRot()));

        if (Math.abs(relYaw) < 90.0F) {
            int dotX = (int) (barX + (barWidth / 2.0F) + (relYaw / 90.0F) * (barWidth / 2.0F));
            int color = (data.expiryTime() != -1L) ? 0xFFFFFFFF : qlogic$getVibrantColor(target);

            // 7x7 검정 테두리 및 5x5 본체
            graphics.fill(dotX - 3, barY - 3, dotX + 4, barY + 4, 0xFF000000);
            graphics.fill(dotX - 2, barY - 2, dotX + 3, barY + 3, color);
            // [복구] 중앙 3x3 반투명 흰색 점
            graphics.fill(dotX - 1, barY - 1, dotX + 2, barY + 2, 0x80FFFFFF);

            if (Math.abs(relYaw) < 8.0F) {
                MutableComponent text = data.name().copy();
                if (data.count() > 1) text.append(" x" + data.count());
                int tw = client.font.width(text);
                graphics.fill(dotX - (tw / 2) - 2, barY - 14, dotX + (tw / 2) + 2, barY - 3, 0x90000000);
                graphics.drawCenteredString(client.font, text, dotX, barY - 12, 0xFFFFFFFF);
            }
        }
    }

    @Unique
    private void qlogic$collectCompassData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        // 꾸러미 내부 재귀 탐색 유지
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            for (ItemStack inner : contents.items()) qlogic$collectCompassData(inner);
        }
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && tracker.target().isPresent()) {
            CompassManager.targetMap.merge(tracker.target().get(), new CompassData(stack.getHoverName(), 1, -1L), (old, val) -> new CompassData(old.name(), old.count() + 1, -1L));
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        while (QuiteLogicalClient.pingKey.consumeClick()) {
            if (client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS)) {
                HitResult hit = client.player.pick(128.0D, 0.0F, false);
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    ClientPlayNetworking.send(new PingPayload(pos));
                    CompassManager.addPing(pos);
                }
            }
        }
        CompassManager.update();
    }

    @Unique
    private int qlogic$getVibrantColor(GlobalPos pos) {
        Random r = new Random((long) pos.pos().getX() * 3123456L ^ (long) pos.pos().getZ() * 1234567L ^ (long) pos.pos().getY());
        return Color.HSBtoRGB(r.nextFloat(), 0.8f, 0.95f);
    }
}