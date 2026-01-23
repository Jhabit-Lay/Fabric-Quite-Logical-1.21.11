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
    /** 렌더링 시스템에 월드 마커 렌더러를 등록합니다. */
    public static void register() {
        // AFTER_ENTITIES는 엔티티 렌더링 후, 지형을 투과해 그리기에 가장 적합한 시점입니다.
        WorldRenderEvents.AFTER_ENTITIES.register(WorldMarkerRenderer::renderMarkers);
    }

    private static void renderMarkers(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 사용자 스크린샷에서 확인된 matrices()와 consumers() 사용
        PoseStack matrices = context.matrices();
        MultiBufferSource consumers = context.consumers();

        // Camera.java 소스 기반: position() 및 rotation() 메서드 호출
        Camera camera = context.gameRenderer().getMainCamera();
        Vec3 cameraPos = camera.position();
        Font font = client.font;

        CompassManager.targetMap.forEach((pos, data) -> {
            // 핑 마커(expiryTime != -1)이고 같은 차원(Overworld 등)일 때만 렌더링
            if (data.expiryTime() != -1L && client.player.level().dimension().equals(pos.dimension())) {
                matrices.pushPose();

                // 1. 월드 좌표를 카메라 기준 상대 좌표로 변환하여 이동 (블록 중심)
                double x = pos.pos().getX() + 0.5 - cameraPos.x;
                double y = pos.pos().getY() + 1.2 - cameraPos.y;
                double z = pos.pos().getZ() + 0.5 - cameraPos.z;
                matrices.translate(x, y, z);

                // 2. 빌보드 회전: 카메라의 현재 회전값 적용 (항상 플레이어를 바라봄)
                matrices.mulPose(camera.rotation());

                // 3. 거리 계산 및 정수 변환
                double dist = client.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos.pos()));

                // 거리에 상관없이 마커 크기를 일정하게 유지하기 위한 스케일 조정
                float scale = (float) (0.025f * (Math.max(1.0, dist / 10.0)));
                matrices.scale(-scale, -scale, scale);
                Matrix4f matrix = matrices.last().pose();

                // 4. [요청 사항] 소수점 없는 실수(정수) 거리 텍스트
                String distText = (int) dist + "m";
                int textWidth = font.width(distText);

                // 5. 렌더링: SEE_THROUGH 모드로 벽 너머에서도 가시성 확보
                // 마커 점 (▪)
                font.drawInBatch("▪", -font.width("▪") / 2f, 0, 0xFFFFFFFF, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0xFF000000, 15728880);

                // 거리 텍스트 및 반투명 배경 (마커 위 12픽셀 위치)
                font.drawInBatch(distText, -textWidth / 2f, -12, 0xFFFFFFFF, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0x90000000, 15728880);

                matrices.popPose();
            }
        });
    }
}