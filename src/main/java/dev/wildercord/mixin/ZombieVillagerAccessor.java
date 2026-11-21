package dev.wildercord.mixin;

import net.minecraft.world.entity.monster.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

/** A life spell on a weakened zombie villager starts its cure, as a golden apple would. */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerAccessor {
	@Invoker("startConverting")
	void wildercord$startConverting( UUID player, int time);
}
