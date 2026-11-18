package com.AdamNew.dynamicwaters.network;

import net.minecraft.network.FriendlyByteBuf;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
//?}

public record SyncSeedPacket(long seed, int seaLevel, double lowlandWidth, double lowlandDepth, double mountainWidth,
        double mountainDepth) implements Payload {
    //? if >=1.20.5 {
    public static final Type<SyncSeedPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dynamicwaters", "sync_seed"));

    public static final StreamCodec<FriendlyByteBuf, SyncSeedPacket> STREAM_CODEC = StreamCodec.ofMember(
        SyncSeedPacket::write,
        SyncSeedPacket::new
    );
    //?}

    public SyncSeedPacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(this.seed);
        buffer.writeInt(this.seaLevel);
        buffer.writeDouble(this.lowlandWidth);
        buffer.writeDouble(this.lowlandDepth);
        buffer.writeDouble(this.mountainWidth);
        buffer.writeDouble(this.mountainDepth);
    }

    //? if >=1.20.5 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
