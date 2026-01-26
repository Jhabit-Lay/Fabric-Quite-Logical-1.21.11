package net.jhabit.qlogic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PingPayload(BlockPos pos, String senderName) implements CustomPacketPayload {

    public static final Type<PingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qlogic", "ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PingPayload::pos,
            ByteBufCodecs.STRING_UTF8, PingPayload::senderName,
            PingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}