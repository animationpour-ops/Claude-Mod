package fr.athen.divinemod;

import fr.athen.divinemod.block.ModBlocks;
import fr.athen.divinemod.item.ModItems;
import fr.athen.divinemod.network.DivineActionPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DivineMod implements ModInitializer {
    public static final String MOD_ID = "divinemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.init();
        ModBlocks.init();
        ModItems.init();

        PayloadTypeRegistry.playC2S().register(DivineActionPayload.ID, DivineActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DivineActionPayload.ID, (payload, context) ->
                context.server().execute(() -> DivinePowers.handleAction(context.player(), payload.action())));

        ServerTickEvents.END_SERVER_TICK.register(DivinePowers::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(DivinePowers::allowDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register(DivinePowers::allowDeath);
        PlayerBlockBreakEvents.BEFORE.register(DivineMining::beforeBreak);
        PlayerBlockBreakEvents.AFTER.register(DivineMining::afterBreak);

        LOGGER.info("Items Divins : la Forge Celeste est prete.");
    }
}
