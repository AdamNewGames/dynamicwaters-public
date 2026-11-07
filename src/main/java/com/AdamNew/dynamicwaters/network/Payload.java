package com.AdamNew.dynamicwaters.network;

//? if >=1.20.5 {
public interface Payload extends net.minecraft.network.protocol.common.custom.CustomPacketPayload {
}
//?} else {
/*public interface Payload {
    void write(net.minecraft.network.FriendlyByteBuf buffer);
}
*///?}
