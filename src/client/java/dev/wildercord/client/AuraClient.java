package dev.wildercord.client;

import dev.wildercord.aura.AuraSense;
import dev.wildercord.aura.AuraStep;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aura on the client: the creatures aura sense has outlined (the server says which, with each breath of the breathing stance;
 * they're drawn outlined in the aura's colour for this player alone, see {@code mixin.EntityRendererAuraSenseMixin}), and the
 * afterimages Aura Step leaves along its way (the server says where each step went; {@code render.AuraShellLayer} draws them).
 */
public final class AuraClient {
	private AuraClient() {}

	/** Sensed creatures by network id, and the game time each stops being outlined. */
	private static final Map<Integer, Long> SENSED = new HashMap<>();
	private static int color = 0xFFFFFF;

	/** An afterimage of a step: where it stands, which way it faces, its colour, the game time it was left, and how long it lasts (ticks). */
	public record Afterimage(Vec3 at, float yaw, int color, long born, int life) {}

	/** How long an afterimage takes to fade (ticks), and how many a step leaves. */
	public static final int AFTERIMAGE_TICKS = 12;
	private static final int AFTERIMAGES_PER_STEP = 4;
	/** Afterimages by the network id of whoever stepped. */
	private static final Map<Integer, List<Afterimage>> AFTERIMAGES = new HashMap<>();

	public static void init() {
		AuraSocialClient.init();
		MasterFormsClient.init();
		// A bonded blade's tooltip tells its story; one lying on the ground lets motes drift up.
		BladeTooltip.init();
		ClientTickEvents.END_CLIENT_TICK.register(dev.wildercord.client.fx.BondGlow::motes);
		ClientPlayNetworking.registerGlobalReceiver(AuraSense.Sensed.TYPE, (payload, context) -> receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(AuraStep.Stepped.TYPE, (payload, context) -> stepped(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SENSED.clear();
			AFTERIMAGES.clear();
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null) {
				SENSED.clear();
				AFTERIMAGES.clear();
				return;
			}
			long now = client.level.getGameTime();
			SENSED.values().removeIf(until -> until < now);
			AFTERIMAGES.values().removeIf(list -> {
				list.removeIf(image -> now - image.born() > image.life());
				return list.isEmpty();
			});
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

	/**
	 * A step was taken: afterimages left along the way, each as the body passes it, the first where it set off (lingering there longer
	 * when the step's afterimage strikes: the Way of the Shadowstep's).
	 */
	static void stepped(AuraStep.Stepped payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		long now = mc.level.getGameTime();
		List<Afterimage> list = AFTERIMAGES.computeIfAbsent(payload.entity(), k -> new ArrayList<>());
		if (payload.from().distanceToSqr(payload.to()) < 0.01) {
			// Left standing where it was (a technique's afterimage): one image, for as long as it lingers.
			list.add(new Afterimage(payload.from(), payload.yaw(), payload.color(), now, AFTERIMAGE_TICKS + Math.max(0, payload.linger())));
			return;
		}
		for (int i = 0; i < AFTERIMAGES_PER_STEP; i++) {
			double t = i / (double) AFTERIMAGES_PER_STEP;
			Vec3 at = payload.from().lerp(payload.to(), t);
			int life = i == 0 ? AFTERIMAGE_TICKS + Math.max(0, payload.linger()) : AFTERIMAGE_TICKS;
			list.add(new Afterimage(at, payload.yaw(), payload.color(), now + Math.round(i * dev.wildercord.aura.AuraRules.STEP_TICKS / (double) AFTERIMAGES_PER_STEP),
				life));
		}
	}

	/** The afterimages of whoever has network id {@code entity} that show at {@code time} (game time and partial tick). */
	public static List<Afterimage> afterimages(int entity, float time) {
		List<Afterimage> list = AFTERIMAGES.get(entity);
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		List<Afterimage> out = new ArrayList<>(list.size());
		for (Afterimage image : list) {
			float age = time - image.born();
			if (age >= 0 && age < image.life()) {
				out.add(image);
			}
		}
		return out;
	}

	/** How many afterimages are showing now, everyone's together (the game tests read it). */
	public static int afterimageCount() {
		return AFTERIMAGES.values().stream().mapToInt(List::size).sum();
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
