package fr.athen.divinemod.client;

import fr.athen.divinemod.network.DivineActionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DivineModClient implements ClientModInitializer {
    private static KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.divinemod." + name, InputUtil.Type.KEYSYM, code, "category.divinemod"));
    }

    @Override
    public void onInitializeClient() {
        KeyBinding gaze = key("gaze", GLFW.GLFW_KEY_R);
        KeyBinding clones = key("clones", GLFW.GLFW_KEY_B);
        KeyBinding up = key("speed_up", GLFW.GLFW_KEY_UP);
        KeyBinding down = key("speed_down", GLFW.GLFW_KEY_DOWN);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (gaze.wasPressed()) ClientPlayNetworking.send(new DivineActionPayload(DivineActionPayload.GAZE));
            while (clones.wasPressed()) ClientPlayNetworking.send(new DivineActionPayload(DivineActionPayload.CLONES));
            while (up.wasPressed()) ClientPlayNetworking.send(new DivineActionPayload(DivineActionPayload.SPEED_UP));
            while (down.wasPressed()) ClientPlayNetworking.send(new DivineActionPayload(DivineActionPayload.SPEED_DOWN));
        });
    }
}
