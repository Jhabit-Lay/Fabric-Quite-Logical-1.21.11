package net.jhabit.qlogic.util;

import net.minecraft.util.Mth;

public class SpyglassZoomManager {
    private static double targetZoom = 1.0;
    private static double currentZoom = 1.0;

    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_STEP = 0.4; // 한 칸당 이동 거리
    private static final double SMOOTHNESS = 0.05; // 낮을수록 더 부드럽고 천천히 이동 (0.05 ~ 0.3 추천)

    public static void onScroll(double amount) {
        // 목표 배율만 먼저 변경합니다.
        targetZoom = Mth.clamp(targetZoom + amount * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
    }

    /**
     * 역할: 현재 줌 값을 목표 줌 값으로 부드럽게 이동시킵니다. (Lerp)
     */
    public static void update() {
        if (Math.abs(currentZoom - targetZoom) < 0.001) {
            currentZoom = targetZoom;
            return;
        }
        // 선형 보간(Lerp) 로직: 현재값 + (목표 - 현재) * 부드러움 계수
        currentZoom += (targetZoom - currentZoom) * SMOOTHNESS;
    }

    public static double getZoomLevel() {
        return currentZoom;
    }

    public static void reset() {
        targetZoom = 1.0;
        currentZoom = 1.0;
    }
}