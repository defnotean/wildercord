package dev.wildercord.client.combat;

import net.minecraft.client.renderer.entity.layers.RenderLayer;
import java.util.List;

/** Renderer-local installation access; preserves the original armor layer's exact list position. */
public interface ArticulatedRendererAccess {
	List<RenderLayer<?, ?>> wildercord$layers();
}
