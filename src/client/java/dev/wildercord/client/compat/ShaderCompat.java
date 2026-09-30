package dev.wildercord.client.compat;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Magic under a shader pack. Wildercord's light is blended its own ways (light added to what's behind
 * it, darkness taken away), and a shader pack draws everything into several buffers at once
 * (colour, surface direction, material) that its later passes read back: blended like that, those
 * buffers fill with nonsense, and the pack paints squares, smears and wrong lighting where the magic was.
 * So while a pack is in use, magic is drawn the way vanilla draws its own translucent particles and
 * glowing eyes, which every pack is written for (see QuadParticleRenderStateMixin and WispRenderer),
 * and glows cast no shadows. Without Iris none of this is touched.
 */
public final class ShaderCompat {
	private ShaderCompat() {}

	private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
	/** Whether a shader pack is drawing the world, checked once a tick. */
	private static boolean active;
	/** Set once something about Iris failed (an Iris too old or too new for this): shaders are left alone then. */
	private static boolean broken;

	public static void init() {
		if (!IRIS) {
			return;
		}
		try {
			IrisBridge.assignPipelines();
		} catch (Throwable e) {
			Wildercord.LOGGER.warn("Couldn't tell Iris how to draw Wildercord's magic; it may look wrong under shader packs", e);
		}
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			if (broken) {
				return;
			}
			try {
				active = IrisBridge.shaderPackInUse();
			} catch (Throwable e) {
				broken = true;
				active = false;
				Wildercord.LOGGER.warn("Couldn't ask Iris whether a shader pack is on", e);
			}
		});
	}

	/** Whether a shader pack is in use: magic is drawn the plain way while one is. */
	public static boolean active() {
		return active;
	}

	/** Whether this is the shader pack's shadow pass, where glows (light, not things) are left out. */
	public static boolean shadowPass() {
		if (!active) {
			return false;
		}
		try {
			return IrisBridge.shadowPass();
		} catch (Throwable e) {
			return false;
		}
	}
}
