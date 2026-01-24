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
    // BeaconRenderer.java 소스에 정의된 상수를 그대로 사용합니다.
    private static final Identifier BEAM_LOCATION = BeaconRenderer.BEAM_LOCATION;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldMarkerRenderer::renderMarkers);
    }

    private static void renderMarkers(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        // [해결] WorldRenderContext 소스 기반: matrices, consumers, commandQueue 사용
        PoseStack matrices = context.matrices();
        MultiBufferSource consumers = context.consumers();
        SubmitNodeCollector commandQueue = context.commandQueue();

        // [요청 사항 반영] Minecraft.java 소스의 public 필드 gameRenderer를 통해 카메라 접근
        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        // [해결] getTimer() 대신 Minecraft.java의 getDeltaTracker() 사용
        // partialTick을 구하기 위해 getGameTimeDeltaPartialTick(false)를 호출합니다.
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long worldTime = client.level.getGameTime();

        CompassManager.targetMap.forEach((pos, data) -> {
            if (data.expiryTime() != -1L && client.player.level().dimension().equals(pos.dimension())) {

                // --- 1. 신호기 빔(Beacon Beam) 렌더링 ---
                // [해결] BeaconRenderer.java 소스의 10개 인수 submitBeaconBeam 호출
                matrices.pushPose();
                double bx = pos.pos().getX() - cameraPos.x;
                double by = pos.pos().getY() - cameraPos.y;
                double bz = pos.pos().getZ() - cameraPos.z;
                matrices.translate(bx, by, bz);

                // 인자(10개): poseStack, submitNodeCollector, identifier, f(scale), g(time), i(yOffset), j(height), k(color), h(innerR), l(outerR)
                BeaconRenderer.submitBeaconBeam(
                        matrices,                               // 1. PoseStack
                        commandQueue,                           // 2. SubmitNodeCollector
                        BEAM_LOCATION,                          // 3. Identifier
                        1.0F,                                   // 4. f (beamRadiusScale)
                        (float)worldTime + partialTick,         // 5. g (animationTime)
                        0,                                      // 6. i (yOffset)
                        2048,                                   // 7. j (height / MAX_RENDER_Y)
                        0xFFFFFFFF,                             // 8. k (color - White)
                        0.2F,                                   // 9. h (innerRadius / SOLID_BEAM_RADIUS)
                        0.25F                                   // 10. l (outerRadius / BEAM_GLOW_RADIUS)
                );
                matrices.popPose();

                // --- 2. 3D 월드 마커 및 거리 텍스트 렌더링 ---
                matrices.pushPose();
                double mx = pos.pos().getX() + 0.5 - cameraPos.x;
                double my = pos.pos().getY() + 1.2 - cameraPos.y;
                double mz = pos.pos().getZ() + 0.5 - cameraPos.z;
                matrices.translate(mx, my, mz);

                // [요청 사항 반영] camera.rotation() 사용
                matrices.mulPose(camera.rotation());

                double dist = client.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos.pos()));
                float scale = (float) (0.025f * (Math.max(1.0, dist / 12.0)));
                matrices.scale(-scale, -scale, scale);

                Matrix4f matrix = matrices.last().pose();
                String distText = (int) dist + "m";

                client.font.drawInBatch("▪", -client.font.width("▪") / 2f, 0, 0xFFFFFFFF, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
                client.font.drawInBatch(distText, -client.font.width(distText) / 2f, -12, 0xFFFFFFFF, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0x90000000, 15728880);

                matrices.popPose();
            }
        });
    }
}