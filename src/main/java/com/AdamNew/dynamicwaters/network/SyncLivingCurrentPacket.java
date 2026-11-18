package com.AdamNew.dynamicwaters.network;

import com.AdamNew.dynamicwaters.LivingCurrent;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
//?}
import org.jetbrains.annotations.Nullable;

public record SyncLivingCurrentPacket(int chunkX, int chunkZ, @Nullable LivingCurrent.Field field) implements Payload {
    //? if >=1.20.5 {
    public static final Type<SyncLivingCurrentPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("dynamicwaters", "sync_living_current"));

    public static final StreamCodec<FriendlyByteBuf, SyncLivingCurrentPacket> STREAM_CODEC = StreamCodec.ofMember(
            SyncLivingCurrentPacket::write,
            SyncLivingCurrentPacket::new);
    //?}

    public SyncLivingCurrentPacket(FriendlyByteBuf buffer) {
        this(buffer.readInt(), buffer.readInt(), buffer.readBoolean() ? readField(buffer) : null);
    }

    private static LivingCurrent.Field readField(FriendlyByteBuf buffer) {
        final float[] scale = new float[256], turn = new float[256], addX = new float[256], addZ = new float[256];
        final short[] top = new short[256];
        final byte[] depth = new byte[256];
        for (int i = 0; i < 256; i++) {
            scale[i] = buffer.readFloat();
            turn[i] = buffer.readFloat();
            addX[i] = buffer.readFloat();
            addZ[i] = buffer.readFloat();
            top[i] = buffer.readShort();
            depth[i] = buffer.readByte();
        }
        return new LivingCurrent.Field(scale, turn, addX, addZ, top, depth);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(chunkX);
        buffer.writeInt(chunkZ);
        buffer.writeBoolean(field != null);
        if (field == null) return;
        for (int i = 0; i < 256; i++) {
            buffer.writeFloat(field.scale()[i]);
            buffer.writeFloat(field.turn()[i]);
            buffer.writeFloat(field.addX()[i]);
            buffer.writeFloat(field.addZ()[i]);
            buffer.writeShort(field.top()[i]);
            buffer.writeByte(field.depth()[i]);
        }
    }

    //? if >=1.20.5 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
