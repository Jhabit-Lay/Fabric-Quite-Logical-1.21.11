package net.jhabit.qlogic;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.jhabit.qlogic.network.PingPayload;

public class QuiteLogicalServer implements ModInitializer {
    @Override
    public void onInitialize() {
        // C2S(클라->서버)와 S2C(서버->클라) 구조 등록
        PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PingPayload.ID, PingPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                context.server().getPlayerList().getPlayers().forEach(player -> {
                    if (player != context.player()) {
                        ServerPlayNetworking.send(player, payload);
                    }
                });
            });
        });
    }
}