package fr.athen.divinemod.network;

import fr.athen.divinemod.DivineMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** 1 = regard autoritaire, 2 = clones, 3 = nuage plus vite, 4 = nuage moins vite */
public record DivineActionPayload(int action) implements CustomPayload {
    public static final CustomPayload.Id<DivineActionPayload> ID = new CustomPayload.Id<>(DivineMod.id("action"));
    public static final PacketCodec<ByteBuf, DivineActionPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, DivineActionPayload::action, DivineActionPayload::new);

    public static final int GAZE = 1;
    public static final int CLONES = 2;
    public static final int SPEED_UP = 3;
    public static final int SPEED_DOWN = 4;

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
