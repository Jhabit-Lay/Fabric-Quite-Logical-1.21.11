package net.jhabit.qlogic.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.jhabit.qlogic.util.CompassManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class WorldMarkerRenderer {
    public static void register() {
        // 엔티티 렌더링 후 3D 공간에 마커를 그립니다.
        WorldRenderEvents.AFTER_ENTITIES.register(WorldMarkerRenderer::renderMarkers);
    }

    private static void renderMarkers(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PoseStack matrices = context.matrices();
        MultiBufferSource consumers = context.consumers();

        // 1.21.11 Camera.java 소스 기반: position() 및 rotation() 메서드 사용
        Camera camera = context.camera();
        Vec3 cameraPos = camera.position();
        Font font = client.font;

        CompassManager.targetMap.forEach((pos, data) -> {
            // 핑 마커(expiryTime != -1)만 3D 월드에 표시
            if (data.expiryTime() != -1L && client.player.level().dimension().equals(pos.dimension())) {
                matrices.pushPose();

                // 1. 블록 위치로 이동 (상대 좌표)
                double x = pos.pos().getX() + 0.5 - cameraPos.x;
                double y = pos.pos().getY() + 1.2 - cameraPos.y;
                double z = pos.pos().getZ() + 0.5 - cameraPos.z;
                matrices.translate(x, y, z);

                // 2. 빌보드 회전 (카메라 방향 고정)
                matrices.mulPose(camera.rotation());

                // 3. 거리별 크기 보정 (멀어도 가독성 유지)
                double dist = client.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos.pos()));
                float scale = (float) (0.025f * (Math.max(1.0, dist / 10.0)));
                matrices.scale(-scale, -scale, scale);
                Matrix4f matrix = matrices.last().pose();

                // 4. [요청 사항] 소수점 없는 실수(정수) 거리
                String distText = (int) dist + "m";
                int textWidth = font.width(distText);

                // 5. 렌더링 (SEE_THROUGH로 지형 투과 보장)
                // 마커 중앙 도트
                font.drawInBatch("▪", -font.width("▪") / 2f, 0, 0xFFFFFFFF, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0xFF000000, 15728880);

                // 상단 정수 거리표시
                font.drawInBatch(distText, -textWidth / 2f, -12, 0xFFFFFFFF, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0x90000000, 15728880);

                matrices.popPose();
            }
        });
    }
}