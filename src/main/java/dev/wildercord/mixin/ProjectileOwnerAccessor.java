package dev.wildercord.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the saved owner identity even after the shooter's live entity has left the world. */
@Mixin(Projectile.class)
public interface ProjectileOwnerAccessor {
	@Accessor("owner")
	EntityReference<Entity> wildercord$ownerReference();
}
