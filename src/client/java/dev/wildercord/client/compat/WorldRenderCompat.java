package dev.wildercord.client.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * World-space draw hooks. 1.21.8 still has fabric's {@code WorldRenderEvents}, so the hooks ride
 * fabric's AFTER_ENTITIES; the context is reshaped to the one the newer versions hand over
 * (see {@link LevelRenderState} and {@link CameraRenderState}), so the callers read the same way
 * on every version.
 */
public final class WorldRenderCompat {
	/** The pieces of the frame a world-space draw hook needs, shaped like fabric's 1.21.10 context. */
	public interface Context {
		PoseStack matrices();

		MultiBufferSource commandQueue();

		LevelRenderState worldState();
	}

	private static final List<Consumer<Context>> AFTER_ENTITIES = new ArrayList<>();
	private static boolean hooked;

	private WorldRenderCompat() {}

	public static void registerAfterEntities(Consumer<Context> hook) {
		AFTER_ENTITIES.add(hook);
		if (!hooked) {
			hooked = true;
			WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderCompat::fire);
		}
	}

	/** Called by fabric once vanilla has drawn entities, before the translucent passes. */
	private static void fire(WorldRenderContext context) {
		if (AFTER_ENTITIES.isEmpty()) {
			return;
		}
		PoseStack pose = context.matrixStack();
		MultiBufferSource buffers = context.consumers();
		LevelRenderState state = new LevelRenderState();
		state.cameraRenderState.pos = context.camera().getPosition();
		state.cameraRenderState.orientation = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
		Context view = new Context() {
			@Override
			public PoseStack matrices() {
				return pose;
			}

			@Override
			public MultiBufferSource commandQueue() {
				return buffers;
			}

			@Override
			public LevelRenderState worldState() {
				return state;
			}
		};
		for (Consumer<Context> hook : AFTER_ENTITIES) {
			hook.accept(view);
		}
	}
}
