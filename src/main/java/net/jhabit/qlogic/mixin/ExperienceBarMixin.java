package net.jhabit.qlogic.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jhabit.qlogic.CompassData;
import net.jhabit.qlogic.QuiteLogicalClient;
import net.jhabit.qlogic.network.PingPayload;
import net.jhabit.qlogic.util.CompassManager;
import net.jhabit.qlogic.util.SpyglassZoomManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
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

@Mixin(Gui.class)
public abstract class ExperienceBarMixin {

    // Shadowing vanilla method to check XP priority
    @Shadow protected abstract boolean willPrioritizeExperienceInfo();

    // Shadow to check if the horse is charging a jump
    @Shadow protected abstract boolean willPrioritizeJumpInfo();

    /**
     * Helper: Check if XP bar should be active
     */
    @Unique
    private boolean qlogic$isXpBarActive() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        // Show XP bar when using Enchantment/Anvil or recently gained XP
        boolean isWorkingContainer = client.player.containerMenu instanceof EnchantmentMenu
                || client.player.containerMenu instanceof AnvilMenu;
        return isWorkingContainer || this.willPrioritizeExperienceInfo();
    }

    /**
     * Only render XP level number when the XP bar is active
     */
    @Redirect(
            method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V")
    )
    private void qlogic$redirectXpLevel(GuiGraphics guiGraphics, Font font, int level) {
        if (qlogic$isXpBarActive()) {
            ContextualBarRenderer.renderExperienceLevel(guiGraphics, font, level);
        }
    }

    /**
     * Choose between Experience, Locator, or Empty bar
     */
    @Inject(method = "nextContextualInfoState", at = @At("RETURN"), cancellable = true)
    private void qlogic$adjustContextualBar(CallbackInfoReturnable<Gui.ContextualInfo> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Priority 1: Experience Bar (Enchanting, Anvil, or gained XP)
        if (qlogic$isXpBarActive()) {
            cir.setReturnValue(Gui.ContextualInfo.EXPERIENCE);
            return;
        }

        // Priority 2: Horse Jump Bar (Only when charging or on cooldown)
        // This solves the issue of Locator hiding the jump gauge
        if (this.willPrioritizeJumpInfo()) {
            cir.setReturnValue(Gui.ContextualInfo.JUMPABLE_VEHICLE);
            return;
        }

        // Priority 3: Locator Bar (If lodestone compass exists)
        if (!CompassManager.targetMap.isEmpty()) {
            cir.setReturnValue(Gui.ContextualInfo.LOCATOR);
            return;
        }

        // Default: Hide if it was supposed to be a static XP bar
        if (cir.getReturnValue() == Gui.ContextualInfo.EXPERIENCE) {
            cir.setReturnValue(Gui.ContextualInfo.EMPTY);
        }
    }
    /**
     * Pre-render logic: Collect compass data and handle pings
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        // Clean expired pings and collect new compass data
        CompassManager.targetMap.entrySet().removeIf(entry -> entry.getValue().expiryTime() == -1L);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            qlogic$collectCompassData(player.getInventory().getItem(i));
        }
        if (player.containerMenu != null) qlogic$collectCompassData(player.containerMenu.getCarried());

        // Handle Spyglass ping key press
        while (QuiteLogicalClient.pingKey.consumeClick()) {
            if (client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS)) {
                HitResult hit = client.player.pick(128.0D, 0.0F, false);
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    GlobalPos gPos = GlobalPos.of(client.player.level().dimension(), pos);

                    // Toggle ping: Add if new, remove if already exists
                    if (CompassManager.targetMap.containsKey(gPos)) {
                        CompassManager.targetMap.remove(gPos);
                        client.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, -1.0F);
                    } else {
                        ClientPlayNetworking.send(new PingPayload(pos));
                        CompassManager.addPing(pos);
                        client.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 0.5F);
                    }
                }
            }
        }
        CompassManager.update();
    }

    /**
     * Post-render logic: Draw custom waypoint dots and spyglass HUD
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void injectAllHUDLogic(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || client.options.hideGui) return;

        // Position of the bar background
        int barX = (guiGraphics.guiWidth() - 182) / 2;
        int barY = guiGraphics.guiHeight() - 27;

        // Render each waypoint on the bar
        CompassManager.targetMap.forEach((pos, data) -> {
            if (player.level().dimension().equals(pos.dimension())) {
                qlogic$renderMarker(guiGraphics, client, player, pos, data, barX, barY, 182);
            }
        });

        // Show detailed info when using Spyglass
        qlogic$renderSpyglassHUD(guiGraphics, client, deltaTracker);
    }

    /**
     * Render entity and block info through Spyglass
     */
    @Unique
    private void qlogic$renderSpyglassHUD(GuiGraphics graphics, Minecraft client, DeltaTracker delta) {
        if (!(client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS))) return;

        float partialTicks = delta.getGameTimeDeltaPartialTick(false);
        Entity camera = client.getCameraEntity();
        if (camera == null) return;

        // Perform raycast to find target
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

        // Draw basic coords and distance
        qlogic$drawInfoBox(graphics, client, String.format("X: %.1f / Y: %.1f / Z: %.1f", hit.getLocation().x, hit.getLocation().y, hit.getLocation().z), centerX, 10, 0xFFFFFFFF, 0.75f);
        qlogic$drawInfoBox(graphics, client, String.format("%.1fm", start.distanceTo(hit.getLocation())), centerX, 19, 0xFF55FFFF, 0.75f);

        // Analysis: Horse stats or Beehive info
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

        // Render zoom level text
        qlogic$drawInfoBox(graphics, client, String.format("Zoom: %.1fx", SpyglassZoomManager.getZoomLevel()), centerX, screenHeight - 60, 0xFFFFAA00, 0.85f);
    }

    /**
     * Helper to draw text with a background box
     */
    @Unique
    private void qlogic$drawInfoBox(GuiGraphics graphics, Minecraft client, String text, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)x, (float)y);
        graphics.pose().scale(scale, scale);
        int width = client.font.width(text);
        graphics.fill(-(width / 2) - 3, -2, (width / 2) + 3, 10, 0x90000000);
        graphics.drawCenteredString(client.font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    /**
     * Draw waypoint dot on the locator bar
     */
    @Unique
    private void qlogic$renderMarker(GuiGraphics graphics, Minecraft client, Player player, GlobalPos target, CompassData data, int barX, int barY, int barWidth) {
        double angle = Math.atan2(target.pos().getZ() + 0.5 - player.getZ(), target.pos().getX() + 0.5 - player.getX());
        float relYaw = Mth.wrapDegrees((float) (Math.toDegrees(angle) - 90.0D - player.getYRot()));

        // Render only when waypoint is within 90 degrees view
        if (Math.abs(relYaw) < 90.0F) {
            int dotX = (int) (barX + (barWidth / 2.0F) + (relYaw / 90.0F) * (barWidth / 2.0F));
            int color = (data.expiryTime() != -1L) ? 0xFFFFFFFF : qlogic$getVibrantColor(target);

            // Dot graphics: Outer border, Inner color, Center highlight
            graphics.fill(dotX - 3, barY - 3, dotX + 4, barY + 4, 0xFF000000);
            graphics.fill(dotX - 2, barY - 2, dotX + 3, barY + 3, color);
            graphics.fill(dotX - 1, barY - 1, dotX + 2, barY + 2, 0x80FFFFFF);

            // Show name if looking at it OR Tab key is held
            if (Math.abs(relYaw) < 8.0F || client.options.keyPlayerList.isDown()) {
                MutableComponent text = data.name().copy();
                if (data.count() > 1) text.append(" x" + data.count());
                int tw = client.font.width(text);
                graphics.fill(dotX - (tw / 2) - 2, barY - 14, dotX + (tw / 2) + 2, barY - 3, 0x90000000);
                graphics.drawCenteredString(client.font, text, dotX, barY - 12, 0xFFFFFFFF);
            }
        }
    }

    /**
     * Find Lodestone Compasses in inventory and bundles
     */
    @Unique
    private void qlogic$collectCompassData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        // Scan inside bundles
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) for (ItemStack inner : contents.items()) qlogic$collectCompassData(inner);

        // Scan for Lodestone tracker component
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && tracker.target().isPresent()) {
            CompassManager.targetMap.merge(tracker.target().get(), new CompassData(stack.getHoverName(), 1, -1L),
                    (old, val) -> new CompassData(old.name(), old.count() + 1, -1L));
        }
    }

    /**
     * Generate unique vibrant color based on position
     */
    @Unique
    private int qlogic$getVibrantColor(GlobalPos pos) {
        Random r = new Random((long) pos.pos().getX() * 3123456L ^ (long) pos.pos().getZ() * 1234567L ^ (long) pos.pos().getY());
        return Color.HSBtoRGB(r.nextFloat(), 0.8f, 0.95f);
    }
}