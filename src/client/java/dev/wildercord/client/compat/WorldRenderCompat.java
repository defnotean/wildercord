package dev.wildercord.client.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;

/**
 * 1.21.9 has no fabric world render events (fabric's {@code WorldRenderEvents} only returns with the
 * 1.21.10 rendering-v1 line): {@code WorldRenderCompatMixin} fires the registered hooks right after
 * vanilla submits entities, handing them the frame's pose stack, level render state and submit collector.
 */
public final class WorldRenderCompat {
	/** The pieces of the frame a world-space submit hook draws with, shaped like fabric's 1.21.10 context. */
	public interface Context {
		PoseStack matrices();

		SubmitNodeCollector commandQueue();

		LevelRenderState worldState();
	}

	private static final List<Consumer<Context>> AFTER_ENTITIES = new ArrayList<>();

	private WorldRenderCompat() {}

	public static void registerAfterEntities(Consumer<Context> hook) {
		AFTER_ENTITIES.add(hook);
	}

	/** Called by the mixin once vanilla has submitted entities, before the translucent passes. */
	public static void fireAfterEntities(PoseStack pose, LevelRenderState state, SubmitNodeCollector collector) {
		if (AFTER_ENTITIES.isEmpty()) {
			return;
		}
		Context context = new Context() {
			@Override
			public PoseStack matrices() {
				return pose;
			}

			@Override
			public SubmitNodeCollector commandQueue() {
				return collector;
			}

			@Override
			public LevelRenderState worldState() {
				return state;
			}
		};
		for (Consumer<Context> hook : AFTER_ENTITIES) {
			hook.accept(context);
		}
	}
}
