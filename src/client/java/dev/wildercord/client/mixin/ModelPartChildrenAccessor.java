package dev.wildercord.client.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Adds one original subtree to a baked model; the existing model and fallback parts remain intact. */
@Mixin(ModelPart.class)
public interface ModelPartChildrenAccessor {
	@Accessor("children")
	Map<String, ModelPart> wildercord$children();
}
