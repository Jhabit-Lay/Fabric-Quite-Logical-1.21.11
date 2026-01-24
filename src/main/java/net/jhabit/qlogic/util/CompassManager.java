package net.jhabit.qlogic.util;

import net.jhabit.qlogic.CompassData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

public class CompassManager {
    public static final Map<GlobalPos, CompassData> targetMap = new HashMap<>();

    public static void addPing(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) return;
        GlobalPos pingPos = GlobalPos.of(client.player.level().dimension(), pos);

        // 60초 후 사라짐
        long expiry = System.currentTimeMillis() + 60000L;
        targetMap.put(pingPos, new CompassData(Component.translatable("text.qlogic.ping_marker"), 1, expiry));
    }

    public static void update() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        long now = System.currentTimeMillis();
        // get players xyz
        Vec3 playerPos = client.player.position();

        targetMap.entrySet().removeIf(entry -> {
            // 1. check time limit (60s)
            if (entry.getValue().expiryTime() != -1L && entry.getValue().expiryTime() < now) return true;

            // 2. check player position (disappear when close enough)
            if (entry.getValue().expiryTime() != -1L) {
                // 핑 찍힌 블록의 중심점과의 거리를 계산 (제곱 거리 사용으로 연산 최적화)
                double distSq = playerPos.distanceToSqr(Vec3.atCenterOf(entry.getKey().pos()));
                // 2.25 = 1.5블록의 제곱. 이 거리 안에 들어오면 사라집니다.
                if (distSq < 2.25) return true;
            }
            return false;
        });
    }
}