package net.jhabit.qlogic.util;

import net.minecraft.util.Mth;

public class SpyglassZoomManager {
    private static double targetZoom = 1.0;
    private static double currentZoom = 1.0;

    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_STEP = 0.2; // scroll per x0.2 zoom
    private static final double SMOOTHNESS = 0.05; // lower this number makes zoom smoother (0.05 ~ 0.3 추천)

    public static void onScroll(double amount) {
        // change target zoom first
        targetZoom = Mth.clamp(targetZoom + amount * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);

        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null) {
            // pitch difference zoom in and zoom out
            float pitch = (float) (1.5F + (targetZoom / MAX_ZOOM) * 0.4F);
            client.player.playSound(net.minecraft.sounds.SoundEvents.SPYGLASS_STOP_USING, 1.0F, pitch);
        }
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