package net.jhabit.qlogic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PingPayload(BlockPos pos) implements CustomPacketPayload {
    // 소스 코드에 정의된 CustomPacketPayload.Type<T>을 사용합니다.
    public static final CustomPacketPayload.Type<PingPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qlogic", "ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PingPayload::pos,
            PingPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}