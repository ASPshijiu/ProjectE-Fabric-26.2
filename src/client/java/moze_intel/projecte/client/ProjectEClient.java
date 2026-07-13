package moze_intel.projecte.client;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.client.screen.AlchemicalBagScreen;
import moze_intel.projecte.client.screen.TransmutationTableScreen;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.items.IItemCharge;
import moze_intel.projecte.content.items.PhilosophersStoneItem;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.network.payloads.ChargeItemPayload;
import moze_intel.projecte.network.payloads.EmcMappingSyncPayload;
import moze_intel.projecte.network.payloads.PhilosophersStoneActionPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectEClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID + "/client");

    public static final KeyMapping.Category PROJECTE_CATEGORY =
          KeyMapping.Category.register(ProjectEAPI.id("category"));

    public static final KeyMapping CHARGE_KEY = new KeyMapping(
          "key.projecte.charge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, PROJECTE_CATEGORY);

    public static final KeyMapping MODE_KEY = new KeyMapping(
          "key.projecte.mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, PROJECTE_CATEGORY);

    public static final KeyMapping EXTRA_FUNCTION_KEY = new KeyMapping(
          "key.projecte.extra_function", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, PROJECTE_CATEGORY);

    public static final KeyMapping FIRE_PROJECTILE_KEY = new KeyMapping(
          "key.projecte.fire_projectile", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, PROJECTE_CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(CHARGE_KEY);
        KeyMappingHelper.registerKeyMapping(MODE_KEY);
        KeyMappingHelper.registerKeyMapping(EXTRA_FUNCTION_KEY);
        KeyMappingHelper.registerKeyMapping(FIRE_PROJECTILE_KEY);

        MenuScreens.register(ModMenuTypes.TRANSMUTATION_TABLE, TransmutationTableScreen::new);
        MenuScreens.register(ModMenuTypes.ALCHEMICAL_BAG, AlchemicalBagScreen::new);

        // Receive the authoritative EMC mapping from the server and publish it into the client's
        // display cache. Must run on the client thread; the Fabric handler already dispatches there.
        ClientPlayNetworking.registerGlobalReceiver(EmcMappingSyncPayload.TYPE,
              (payload, ctx) -> ctx.client().execute(() -> {
                  int size = payload.values().size();
                  LOGGER.info("Received EMC mapping sync payload with {} values", size);
                  ProjectEEmc.service().replace(payload.values());
              }));

        // Charge keybind (V): adjust the held item's charge. Shift = discharge. Find the chargeable
        // item in either hand, send the C2S payload so the server applies it authoritatively, and
        // apply locally for snappy feedback.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CHARGE_KEY.consumeClick()) {
                InteractionHand hand = findChargeableHand(client);
                if (hand == null) continue;
                ItemStack stack = client.player.getItemInHand(hand);
                if (!(stack.getItem() instanceof IItemCharge chargeable)) continue;
                boolean negative = (org.lwjgl.glfw.GLFW.glfwGetKey(
                      client.getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == 1
                      || org.lwjgl.glfw.GLFW.glfwGetKey(
                      client.getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == 1);
                chargeable.changeCharge(client.player, stack, negative ? -1 : 1);
                ClientPlayNetworking.send(new ChargeItemPayload(hand, negative));
            }
            sendStoneAction(client, MODE_KEY, PhilosophersStoneActionPayload.Action.MODE);
            sendStoneAction(
                  client, EXTRA_FUNCTION_KEY,
                  PhilosophersStoneActionPayload.Action.EXTRA_FUNCTION);
            sendStoneAction(
                  client, FIRE_PROJECTILE_KEY,
                  PhilosophersStoneActionPayload.Action.PROJECTILE);
        });

        LOGGER.info("Initializing ProjectE client for Fabric 26.2");
    }

    private static void sendStoneAction(
          Minecraft client,
          KeyMapping key,
          PhilosophersStoneActionPayload.Action action
    ) {
        while (key.consumeClick()) {
            InteractionHand hand = findPhilosophersStoneHand(client);
            if (hand != null) {
                ClientPlayNetworking.send(new PhilosophersStoneActionPayload(hand, action));
            }
        }
    }

    private static InteractionHand findPhilosophersStoneHand(Minecraft client) {
        if (client.player == null) return null;
        for (InteractionHand hand : InteractionHand.values()) {
            if (client.player.getItemInHand(hand).getItem() instanceof PhilosophersStoneItem) {
                return hand;
            }
        }
        return null;
    }

    private static InteractionHand findChargeableHand(Minecraft client) {
        if (client.player == null) return null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = client.player.getItemInHand(hand);
            if (stack.getItem() instanceof IItemCharge) {
                return hand;
            }
        }
        return null;
    }
}
