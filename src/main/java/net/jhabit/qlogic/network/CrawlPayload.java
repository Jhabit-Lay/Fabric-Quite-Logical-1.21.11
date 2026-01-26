package net.jhabit.qlogic.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CrawlPayload(boolean isCrawling) implements CustomPacketPayload {
    public static final Type<CrawlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qlogic", "crawl"));

    // [KR] 불린(boolean) 값을 위한 표준 스트림 코덱 정의
    // [EN] Define standard stream codec for boolean values
    public static final StreamCodec<RegistryFriendlyByteBuf, CrawlPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CrawlPayload::isCrawling,
            CrawlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}