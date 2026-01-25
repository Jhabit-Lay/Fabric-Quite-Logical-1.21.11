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

@Mixin(Gui.class)
public abstract class ExperienceBarMixin {

    @Shadow protected abstract boolean willPrioritizeExperienceInfo();
    @Shadow protected abstract boolean willPrioritizeJumpInfo();

    // Flag: Player has a compass in inventory
    @Unique private boolean qlogic$hasAnyCompass = false;

    /**
     * Check if XP bar is needed for Gain/Anvil/Enchant
     */
    @Unique
    private boolean qlogic$isXpBarActive() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        boolean isWorkingContainer = client.player.containerMenu instanceof EnchantmentMenu
                || client.player.containerMenu instanceof AnvilMenu;
        return isWorkingContainer || this.willPrioritizeExperienceInfo();
    }

    /**
     * Show XP Level number logic
     */
    @Redirect(
            method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V")
    )
    private void qlogic$redirectXpLevel(GuiGraphics guiGraphics, Font font, int level) {
        // Show level if: No compass OR gaining XP OR compass exists but no active waypoints
        if (!this.qlogic$hasAnyCompass || qlogic$isXpBarActive() || CompassManager.targetMap.isEmpty()) {
            ContextualBarRenderer.renderExperienceLevel(guiGraphics, font, level);
        }
    }

    /**
     * Set HUD priority: Experience > Jump > Locator
     */
    @Inject(method = "nextContextualInfoState", at = @At("RETURN"), cancellable = true)
    private void qlogic$adjustContextualBar(CallbackInfoReturnable<Gui.ContextualInfo> cir) {
        // Use vanilla logic if no compass
        if (!this.qlogic$hasAnyCompass) return;

        // Force XP Bar if recently gained XP or using tables
        if (qlogic$isXpBarActive()) {
            cir.setReturnValue(Gui.ContextualInfo.EXPERIENCE);
            return;
        }

        // Show Jump Bar if charging
        if (this.willPrioritizeJumpInfo()) {
            cir.setReturnValue(Gui.ContextualInfo.JUMPABLE_VEHICLE);
            return;
        }

        // Show Locator ONLY if there are registered lodestones
        if (!CompassManager.targetMap.isEmpty()) {
            cir.setReturnValue(Gui.ContextualInfo.LOCATOR);
        }
        // Else: Revert to Experience Bar (even with compass)
        else {
            cir.setReturnValue(Gui.ContextualInfo.EXPERIENCE);
        }
    }

    /**
     * Head render: Collect data and handle pings
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        this.qlogic$hasAnyCompass = false;
        CompassManager.targetMap.entrySet().removeIf(entry -> entry.getValue().expiryTime() == -1L);

        // Scan inventory for compasses and bundles
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            qlogic$collectCompassData(player.getInventory().getItem(i));
        }
        if (player.containerMenu != null) qlogic$collectCompassData(player.containerMenu.getCarried());

        // Spyglass ping handling
        while (QuiteLogicalClient.pingKey.consumeClick()) {
            if (client.player.isUsingItem() && client.player.getUseItem().is(Items.SPYGLASS)) {
                HitResult hit = client.player.pick(128.0D, 0.0F, false);
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    GlobalPos gPos = GlobalPos.of(client.player.level().dimension(), pos);
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
     * Tail render: Custom HUD elements
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void injectAllHUDLogic(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null || client.options.hideGui) return;

        // Show Coords only if compass is present
        if (this.qlogic$hasAnyCompass) {
            qlogic$renderBedrockCoords(guiGraphics, client, player);
        }

        // Show Markers only if waypoints exist
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

    /**
     * Bedrock-style HUD logic (User customized)
     */
    @Unique
    private void qlogic$renderBedrockCoords(GuiGraphics graphics, Minecraft client, Player player) {
        int x = Mth.floor(player.getX());
        int y = Mth.floor(player.getY());
        int z = Mth.floor(player.getZ());

        String coordsText = Component.translatable("text.qlogic.xyz", x, y, z).getString();
        Direction dir = player.getDirection();
        String directionText = Component.translatable("text.qlogic.facing", Component.translatable("text.qlogic.direction." + dir.getName().toLowerCase())).getString();

        graphics.pose().pushMatrix();
        // Position: User defined (5, 30)
        graphics.pose().translate(5.0F, 30.0F);
        // Scale: User defined (0.8x)
        graphics.pose().scale(0.8F);

        // Draw Text: Coords first (0,0), then Facing (0,10)
        graphics.drawString(client.font, directionText, 0, 0, 0xFFFFFFFF);
        graphics.drawString(client.font, coordsText, 0, 10, 0xFFFFFFFF);


        graphics.pose().popMatrix();
    }

    /**
     * Spyglass analysis HUD
     */
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

    /**
     * Draw text with background box
     */
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

    /**
     * Render waypoint marker dots
     */
    @Unique
    private void qlogic$renderMarker(GuiGraphics graphics, Minecraft client, Player player, GlobalPos target, CompassData data, int barX, int barY, int barWidth) {
        double angle = Math.atan2(target.pos().getZ() + 0.5 - player.getZ(), target.pos().getX() + 0.5 - player.getX());
        float relYaw = Mth.wrapDegrees((float) (Math.toDegrees(angle) - 90.0D - player.getYRot()));
        if (Math.abs(relYaw) < 90.0F) {
            int dotX = (int) (barX + (barWidth / 2.0F) + (relYaw / 90.0F) * (barWidth / 2.0F));
            int color = (data.expiryTime() != -1L) ? 0xFFFFFFFF : qlogic$getVibrantColor(target);
            graphics.fill(dotX - 3, barY - 3, dotX + 4, barY + 4, 0xFF000000);
            graphics.fill(dotX - 2, barY - 2, dotX + 3, barY + 3, color);
            graphics.fill(dotX - 1, barY - 1, dotX + 2, barY + 2, 0x80FFFFFF);
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
     * Scan items for compass and lodestone data
     */
    @Unique
    private void qlogic$collectCompassData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (stack.is(Items.COMPASS)) this.qlogic$hasAnyCompass = true;
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) for (ItemStack inner : contents.items()) qlogic$collectCompassData(inner);
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && tracker.target().isPresent()) {
            CompassManager.targetMap.merge(tracker.target().get(), new CompassData(stack.getHoverName(), 1, -1L),
                    (old, val) -> new CompassData(old.name(), old.count() + 1, -1L));
        }
    }

    /**
     * Generate color from position
     */
    @Unique
    private int qlogic$getVibrantColor(GlobalPos pos) {
        Random r = new Random((long) pos.pos().getX() * 3123456L ^ (long) pos.pos().getZ() * 1234567L ^ (long) pos.pos().getY());
        return Color.HSBtoRGB(r.nextFloat(), 0.8f, 0.95f);
    }
}