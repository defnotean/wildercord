package dev.wildercord.mixin;

import java.util.UUID;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the saved owner identity even after the shooter's live entity has left the world. */
@Mixin(Projectile.class)
public interface ProjectileOwnerAccessor {
	@Accessor("ownerUUID")
	UUID wildercord$ownerUuid();
}
