package dev.wildercord.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A storm spell may charge a creeper as lightning would, without lightning's own burn and damage on top. */
@Mixin(Creeper.class)
public interface CreeperAccessor {
	@Accessor("DATA_IS_POWERED")
	static EntityDataAccessor<Boolean> wildercord$powered() {
		throw new AssertionError();
	}
}
