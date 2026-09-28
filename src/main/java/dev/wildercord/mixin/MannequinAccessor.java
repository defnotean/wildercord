package dev.wildercord.mixin;

import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Phantom's afterimage is a mannequin wearing the caster's skin: no label under it, and it stands still. */
@Mixin(Mannequin.class)
public interface MannequinAccessor {
	@Invoker("setHideDescription")
	void wildercord$setHideDescription(boolean hide);

	@Invoker("setImmovable")
	void wildercord$setImmovable(boolean immovable);
}
