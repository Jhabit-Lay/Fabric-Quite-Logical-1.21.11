package net.jhabit.qlogic.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jhabit.qlogic.QuiteLogicalClient;
import net.jhabit.qlogic.network.PingPayload;
import net.jhabit.qlogic.network.RemovePingPayload;
import net.jhabit.qlogic.util.CompassData;
import net.jhabit.qlogic.util.CompassManager;
import net.jhabit.qlogic.util.SpyglassZoomManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;
import java.util.Random;
import java.util.UUID;

@Mixin(Gui.class)
public abstract class ExperienceBarMixin {

    @Shadow protected abstract boolean willPrioritizeExperienceInfo();
    @Shadow protected abstract boolean willPrioritizeJumpInfo();

    @Unique private boolean qlogic$hasAnyCompass = false;
    @Unique private boolean qlogic$hasClock = false;

    /**
     * [KR] 경험치 바 강제 활성화 조건 확인
     */
    @Unique
    private boolean qlogic$isXpBarActive() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        return client.player.containerMenu instanceof EnchantmentMenu
                || client.player.containerMenu instanceof AnvilMenu
                || this.willPrioritizeExperienceInfo();
    }

    /**
     * [KR] 경험치 레벨 숫자 렌더링 리다이렉트
     */
    @Redirect(
            method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V")
    )
    private void qlogic$redirectXpLevel(GuiGraphics guiGraphics, Font font, int level) {
        if (!this.qlogic$hasAnyCompass || qlogic$isXpBarActive() || CompassManager.targetMap.isEmpty()) {
            ContextualBarRenderer.renderExperienceLevel(guiGraphics, font, level);
        }
    }

    /**
     * [KR] 하단 컨텍스트 바 우선순위 조정
     */
    @Inject(method = "nextContextualInfoState", at = @At("RETURN"), cancellable = true)
    private void qlogic$adjustContextualBar(CallbackInfoReturnable<Gui.ContextualInfo> cir) {
        Minecraft client = Minecraft.getInstance();
        if (!this.qlogic$hasAnyCompass || (client.player != null && client.player.isCreative())) return;

        if (qlogic$isXpBarActive()) {
            cir.setReturnValue(Gui.ContextualInfo.EXPERIENCE);
            return;
        }

        if (this.willPrioritizeJumpInfo()) {
            cir.setReturnValue(Gui.ContextualInfo.JUMPABLE_VEHICLE);
            return;
        }

        if (!CompassManager.targetMap.isEmpty()) {
            cir.setReturnValue(Gui.ContextualInfo.LOCATOR);
        }
    }

    /**
     * [KR] 렌더링 시작 시 데이터 수집 및 핑 처리
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        this.qlogic$hasAnyCompass = false;
        this.qlogic$hasClock = false;

        // [KR] 수정 사항: 인벤토리 기반 마커(-1L)를 매 프레임 초기화하여 카운트 중복 증가 방지
        // [EN] Fix: Clear inventory markers (-1L) every frame to prevent duplicate count increases
        CompassManager.targetMap.entrySet().removeIf(entry -> entry.getValue().expiryTime() == -1L);

        // [KR] 만료된 핑 제거
        CompassManager.targetMap.entrySet().removeIf(entry ->
                entry.getValue().expiryTime() != -1L && System.currentTimeMillis() > entry.getValue().expiryTime()
        );

        // [KR] 인벤토리 조사
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            qlogic$collectCompassData(player.getInventory().getItem(i));
        }
        if (player.containerMenu != null) qlogic$collectCompassData(player.containerMenu.getCarried());

        // [KR] 망원경 핑 단축키 처리
        while (QuiteLogicalClient.pingKey.consumeClick()) {
            if (client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS)) {
                HitResult hit = client.player.pick(160.0D, 0.0F, false);
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    int myColor = qlogic$getStableColor(player.getUUID());
                    ClientPlayNetworking.send(new PingPayload(pos, player.getName().getString(), player.getUUID(), myColor));
                    player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 0.5F);
                }
            }
        }
        CompassManager.update();
    }

    /**
     * [KR] 렌더링 종료 시 HUD 요소 출력
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void injectAllHUDLogic(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || client.options.hideGui) return;

        if (this.qlogic$hasAnyCompass || this.qlogic$hasClock) {
            qlogic$renderBedrockCoords(guiGraphics, client, player);
        }

        if (this.qlogic$hasAnyCompass && !CompassManager.targetMap.isEmpty()) {
            int barX = (guiGraphics.guiWidth() - 182) / 2;
            int barY = guiGraphics.guiHeight() - 27;
            CompassManager.targetMap.forEach((pos, data) -> {
                if (player.level().dimension().equals(pos.dimension())) {
                    qlogic$renderMarker(guiGraphics, client, player, pos, data, barX, barY, 182);
                }
            });
        }
        qlogic$renderSpyglassHUD(guiGraphics, client, deltaTracker);
    }

    @Unique
    private void qlogic$renderBedrockCoords(GuiGraphics graphics, Minecraft client, Player player) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(5.0F, 30.0F);
        graphics.pose().scale(0.8F);

        int currentY = 0;
        if (this.qlogic$hasAnyCompass) {
            int x = Mth.floor(player.getX());
            int y = Mth.floor(player.getY());
            int z = Mth.floor(player.getZ());
            String coordsText = Component.translatable("text.qlogic.xyz", x, y, z).getString();
            Direction dir = player.getDirection();
            String directionText = Component.translatable("text.qlogic.facing", Component.translatable("text.qlogic.direction." + dir.getName().toLowerCase())).getString();

            graphics.drawString(client.font, directionText, 0, currentY, 0xFFFFFFFF);
            currentY += 10;
            graphics.drawString(client.font, coordsText, 0, currentY, 0xFFFFFFFF);
            currentY += 10;
        }

        if (this.qlogic$hasClock) {
            long totalMinutes = (player.level().getDayTime() + 6000) % 24000;
            String timeText = String.format("%02d:%02d", (int)(totalMinutes / 1000) % 24, (int)((totalMinutes % 1000) * 60 / 1000));
            graphics.drawString(client.font, timeText, 0, currentY, 0xFFFFFFFF);
        }
        graphics.pose().popMatrix();
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

        qlogic$drawInfoBox(graphics, client, String.format("X: %.1f / Y: %.1f / Z: %.1f", hit.getLocation().x, hit.getLocation().y, hit.getLocation().z), centerX, 10, 0xFFFFFFFF, 0.75f);
        qlogic$drawInfoBox(graphics, client, String.format("%.1fm", start.distanceTo(hit.getLocation())), centerX, 19, 0xFF55FFFF, 0.75f);

        if (hit instanceof EntityHitResult entHit && entHit.getEntity() instanceof AbstractHorse horse) {
            String speed = String.format("%.1f", horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17);
            String jump = String.format("%.1f", horse.getAttributeValue(Attributes.JUMP_STRENGTH));
            String stats = Component.translatable("text.qlogic.horse_stats", (int) horse.getHealth(), (int) horse.getMaxHealth(), speed, jump).getString();
            qlogic$drawInfoBox(graphics, client, stats, centerX, 28, 0xFFFFFFFF, 0.75f);
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockPos blockPos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(blockPos);
            if (state.is(BlockTags.BEEHIVES)) {
                int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
                int bees = (client.level.getBlockEntity(blockPos) instanceof BeehiveBlockEntity beehive) ? beehive.getOccupantCount() : 0;
                qlogic$drawInfoBox(graphics, client, Component.translatable("text.qlogic.beehive_info", bees, honey).getString(), centerX, 28, 0xFFFFFF55, 0.75f);
            }
        }
        qlogic$drawInfoBox(graphics, client, String.format("Zoom: %.1fx", SpyglassZoomManager.getZoomLevel()), centerX, screenHeight - 60, 0xFFFFAA00, 0.85f);
    }

    @Unique
    private void qlogic$drawInfoBox(GuiGraphics graphics, Minecraft client, String text, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)x, (float)y);
        graphics.pose().scale(scale);
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
            graphics.fill(dotX - 3, barY - 3, dotX + 4, barY + 4, 0xFF000000);
            graphics.fill(dotX - 2, barY - 2, dotX + 3, barY + 3, data.color() | 0xFF000000);
            graphics.fill(dotX - 1, barY - 1, dotX + 2, barY + 2, 0x80FFFFFF);

            if (Math.abs(relYaw) < 8.0F || client.options.keyPlayerList.isDown()) {
                // [KR] 수정 사항: 실제 핑(expiry != -1)일 때만 "의 핑"을 붙임
                // [EN] Fix: Only append "의 핑" for actual Pings (expiry != -1)
                MutableComponent text = (data.expiryTime() != -1L)
                        ? Component.translatable("text.qlogic.player_ping", data.name())
                        : data.name().copy();

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
        if (stack.is(Items.COMPASS)) this.qlogic$hasAnyCompass = true;
        if (stack.is(Items.CLOCK)) this.qlogic$hasClock = true;
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) for (ItemStack inner : contents.items()) qlogic$collectCompassData(inner);

        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && tracker.target().isPresent()) {
            int lodeColor = qlogic$getVibrantColor(tracker.target().get());
            // [KR] 수정 사항: 병합 시 기존 마커가 '핑'이면 핑의 만료 시간과 색상을 유지하도록 개선
            // [EN] Fix: Preserve Ping's expiry and color when merging with inventory Lodestones
            CompassManager.targetMap.merge(tracker.target().get(), CompassData.permanent(stack.getHoverName(), 1, lodeColor),
                    (old, val) -> new CompassData(old.name(), old.count() + 1, old.color(), old.expiryTime()));
        }
    }

    @Unique
    private int qlogic$getVibrantColor(GlobalPos pos) {
        Random r = new Random((long) pos.pos().getX() * 3123456L ^ (long) pos.pos().getZ() * 1234567L ^ (long) pos.pos().getY());
        return Color.HSBtoRGB(r.nextFloat(), 0.8f, 0.95f);
    }

    @Unique
    private int qlogic$getStableColor(UUID uuid) {
        Random r = new Random(uuid.getMostSignificantBits());
        float h = r.nextFloat();
        return Color.HSBtoRGB(h, 0.7f, 0.9f);
    }
}