package net.jhabit.qlogic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * [KR] 핑 정보를 담는 패킷 (위치, 이름, 유저ID, 색상 포함)
 * [EN] Packet containing ping info (Pos, Name, UUID, Color)
 */
public record PingPayload(BlockPos pos, String senderName, UUID senderUuid, int color) implements CustomPacketPayload {
    public static final Type<PingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qlogic", "ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PingPayload::pos,
            ByteBufCodecs.STRING_UTF8, PingPayload::senderName,
            UUIDUtil.STREAM_CODEC, PingPayload::senderUuid,
            ByteBufCodecs.INT, PingPayload::color,
            PingPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}