package dev.wildercord.client.trials;

import dev.wildercord.trials.CoopTrialContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Registers client entity renderers for Multiplayer Co-Op Trials. */
public final class CoopTrialsClient {
	private CoopTrialsClient() {}

	public static void init() {
		EntityRendererRegistry.register(CoopTrialContent.VAELKHOR, VaelkhorRenderer::new);
	}
}
