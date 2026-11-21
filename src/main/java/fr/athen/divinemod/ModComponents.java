package fr.athen.divinemod;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModComponents {
    /** Pouvoir actuellement sélectionné sur l'item (épée, arc). */
    public static final ComponentType<Integer> MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            DivineMod.id("mode"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    private ModComponents() {}

    public static void init() {}
}
