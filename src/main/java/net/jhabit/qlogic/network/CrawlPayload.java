package net.jhabit.qlogic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CrawlPayload(boolean isCrawling) implements CustomPacketPayload {
    public static final Type<CrawlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qlogic", "crawl"));
    public static final StreamCodec<FriendlyByteBuf, CrawlPayload> CODEC = CustomPacketPayload.codec(CrawlPayload::write, CrawlPayload::new);

    public CrawlPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isCrawling);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}