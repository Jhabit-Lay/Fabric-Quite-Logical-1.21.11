package net.jhabit.qlogic.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

/**
 * [KR] 나침반 마커(핑)의 수명 및 거리를 관리하는 매니저
 * [EN] Manager that handles the lifespan and distance of compass markers (pings)
 */
public class CompassManager {
    // [KR] 위치 정보를 키로 하고 마커 데이터를 값으로 가지는 맵
    public static final Map<GlobalPos, CompassData> targetMap = new HashMap<>();

    /**
     * [KR] 새로운 핑을 추가합니다. (기본 색상: 흰색)
     * [EN] Adds a new ping. (Default color: White)
     */
    public static void addPing(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        GlobalPos pingPos = GlobalPos.of(client.player.level().dimension(), pos);

        // [KR] 60초 후 만료 설정 / [EN] Set to expire after 60 seconds
        long expiry = System.currentTimeMillis() + 60000L;

        // [KR] 수정: 4개의 인자 전달 (이름, 개수, 색상, 만료시간)
        // [EN] Fixed: Pass 4 arguments (Name, Count, Color, Expiry)
        targetMap.put(pingPos, new CompassData(
                Component.translatable("text.qlogic.ping_marker"),
                1,
                0xFFFFFFFF,
                expiry
        ));
    }

    /**
     * [KR] 매 틱마다 호출되어 만료되었거나 플레이어와 너무 가까운 핑을 제거합니다.
     * [EN] Called every tick to remove expired pings or those too close to the player.
     */
    public static void update() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        long now = System.currentTimeMillis();
        Vec3 playerPos = client.player.position();

        targetMap.entrySet().removeIf(entry -> {
            CompassData data = entry.getValue();

            // 1. [KR] 시간 제한 확인 / [EN] Check time limit
            if (data.expiryTime() != -1L && data.expiryTime() < now) return true;

            // 2. [KR] 플레이어 위치 확인 (충분히 가까워지면 삭제)
            // [EN] Check player position (Remove if close enough)
            if (data.expiryTime() != -1L) {
                double distSq = playerPos.distanceToSqr(Vec3.atCenterOf(entry.getKey().pos()));
                // [KR] 1.5블록(2.25 제곱미터) 이내로 접근 시 삭제
                if (distSq < 2.25) return true;
            }
            return false;
        });
    }
}