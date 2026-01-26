package net.jhabit.qlogic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RemovePingPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RemovePingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qlogic", "remove_ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemovePingPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemovePingPayload::pos,
            RemovePingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}