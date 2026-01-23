package net.jhabit.qlogic.util;

import net.jhabit.qlogic.CompassData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;

public class CompassManager {
    // 모든 렌더러가 이 하나의 맵을 공유하여 데이터 불일치를 방지합니다.
    public static final Map<GlobalPos, CompassData> targetMap = new HashMap<>();

    public static void addPing(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        GlobalPos pingPos = GlobalPos.of(client.player.level().dimension(), pos);
        Component displayName = Component.translatable("text.qlogic.ping_marker");
        long expiry = System.currentTimeMillis() + 60000L; // 60초 유지
        targetMap.put(pingPos, new CompassData(displayName, 1, expiry));
    }

    public static void update() {
        long now = System.currentTimeMillis();
        // 만료된 핑 마커만 제거 (나침반 데이터는 유지)
        targetMap.entrySet().removeIf(entry ->
                entry.getValue().expiryTime() != -1L && entry.getValue().expiryTime() < now
        );
    }
}