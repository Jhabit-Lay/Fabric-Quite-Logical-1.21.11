package net.jhabit.qlogic.util;

import net.jhabit.qlogic.CompassData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;

public class CompassManager {
    // [핵심] Mixin과 Renderer가 이 맵을 공통으로 참조해야 합니다.
    public static final Map<GlobalPos, CompassData> targetMap = new HashMap<>();

    public static void addPing(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        GlobalPos pingPos = GlobalPos.of(client.player.level().dimension(), pos);
        Component displayName = Component.translatable("text.qlogic.ping_marker");
        // 1분간 유지
        long expiry = System.currentTimeMillis() + 60000L;

        targetMap.put(pingPos, new CompassData(displayName, 1, expiry));
    }

    public static void update() {
        long now = System.currentTimeMillis();
        targetMap.entrySet().removeIf(entry ->
                entry.getValue().expiryTime() != -1L && entry.getValue().expiryTime() < now
        );
    }
}