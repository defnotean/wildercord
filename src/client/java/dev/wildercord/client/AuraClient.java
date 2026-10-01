package dev.wildercord.client;

import dev.wildercord.aura.AuraSense;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Aura on the client: the creatures aura sense has outlined (the server says which, with each breath of the breathing stance;
 * they're drawn outlined in the aura's colour for this player alone, see {@code mixin.EntityRendererAuraSenseMixin}).
 */
public final class AuraClient {
	private AuraClient() {}

	/** Sensed creatures by network id, and the game time each stops being outlined. */
	private static final Map<Integer, Long> SENSED = new HashMap<>();
	private static int color = 0xFFFFFF;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(AuraSense.Sensed.TYPE, (payload, context) -> receive(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SENSED.clear());
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null) {
				SENSED.clear();
				return;
			}
			long now = client.level.getGameTime();
			SENSED.values().removeIf(until -> until < now);
		});
	}

	static void receive(AuraSense.Sensed payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		long until = mc.level.getGameTime() + payload.ticks();
		color = payload.color();
		for (int id : payload.ids()) {
			SENSED.merge(id, until, Math::max);
		}
	}

	/** Whether {@code entity} is outlined by aura sense now. */
	public static boolean sensed(Entity entity) {
		Long until = SENSED.get(entity.getId());
		return until != null && entity.level() != null && until >= entity.level().getGameTime();
	}

	/** The colour aura sense outlines in. */
	public static int color() {
		return color;
	}

	/** How many creatures are outlined now (the game tests read it). */
	public static int sensedCount() {
		return SENSED.size();
	}
}
