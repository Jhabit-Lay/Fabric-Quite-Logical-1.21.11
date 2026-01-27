package net.jhabit.qlogic.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.jhabit.qlogic.util.CompassManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class WorldMarkerRenderer {
    // [KR] 신호기 빔 텍스처 경로 / [EN] Beacon beam texture location
    private static final Identifier BEAM_LOCATION = BeaconRenderer.BEAM_LOCATION;

    public static void register() {
        // [KR] 엔티티 렌더링 이후 단계에 마커 렌더링 등록
        // [EN] Register marker rendering after entity rendering stage
        WorldRenderEvents.AFTER_ENTITIES.register(WorldMarkerRenderer::renderMarkers);
    }

    private static void renderMarkers(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        PoseStack matrices = context.matrices();
        MultiBufferSource consumers = context.consumers();
        SubmitNodeCollector commandQueue = context.commandQueue();

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long worldTime = client.level.getGameTime();

        CompassManager.targetMap.forEach((pos, data) -> {
            // [KR] 만료되지 않았고 동일한 차원에 있는 마커만 렌더링 (자석석 마커는 빔을 그리지 않음)
            // [EN] Only render markers that haven't expired and are in the same dimension (Lodestones don't draw beams)
            if (data.expiryTime() != -1L && client.player.level().dimension().equals(pos.dimension())) {

                int userColor = data.color();

                // --- 1. 신호기 빔(Beacon Beam) 렌더링 ---
                matrices.pushPose();
                double bx = pos.pos().getX() - cameraPos.x;
                double by = pos.pos().getY() - cameraPos.y;
                double bz = pos.pos().getZ() - cameraPos.z;
                matrices.translate(bx, by, bz);

                BeaconRenderer.submitBeaconBeam(
                        matrices,
                        commandQueue,
                        BEAM_LOCATION,
                        1.0F,
                        (float) worldTime + partialTick,
                        0,
                        2048,
                        userColor,
                        0.15F,
                        0.2F
                );
                matrices.popPose();

                // --- 2. 3D 월드 마커 및 거리 텍스트 렌더링 ---
                matrices.pushPose();
                double mx = pos.pos().getX() + 0.5 - cameraPos.x;
                double my = pos.pos().getY() + 1.2 - cameraPos.y;
                double mz = pos.pos().getZ() + 0.5 - cameraPos.z;
                matrices.translate(mx, my, mz);

                matrices.mulPose(camera.rotation());

                double dist = client.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos.pos()));
                float scale = (float) (0.025f * (Math.max(1.0, dist / 12.0)));
                matrices.scale(-scale, -scale, scale);

                Matrix4f matrix = matrices.last().pose();
                String distText = (int) dist + "m";

                client.font.drawInBatch("▪", -client.font.width("▪") / 2f, 0, userColor, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
                client.font.drawInBatch(distText, -client.font.width(distText) / 2f, -12, userColor, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0x90000000, 15728880);

                matrices.popPose();
            }
        });
    }
}