package dev.wildercord.client.compat;

import java.util.function.Function;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.21.9's {@code Model<T>}, which is posed from a render state. 1.21.8's {@code Model} carries no type
 * parameter and is posed by hand, so our own geometry extends this instead: it keeps the newer shape
 * ({@code setupAnim(state)}) and {@link RenderCompat#submitModel} poses it the same way for every version.
 */
public abstract class McModel<T> extends Model {
	protected McModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
		super(root, renderType);
	}

	/** Poses the geometry for a frame's worth of data. */
	public abstract void setupAnim(T state);
}
