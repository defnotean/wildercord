package dev.wildercord.mixin;

import dev.wildercord.runesmith.DuplicateSwap;
import dev.wildercord.runesmith.Runesmith;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Runesmith turned into something else (a zombie villager keeps its trades) leaves its buyback and reroll offers behind. */
@Mixin(Mob.class)
public abstract class MobConversionMixin {
	@Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;",
		at = @At("HEAD"))
	private <T extends Mob> void wildercord$stripSwaps(EntityType<T> type, boolean persistent, CallbackInfoReturnable<T> cir) {
		if ((Object) this instanceof Villager villager && Runesmith.is(villager)) {
			DuplicateSwap.strip(villager);
		}
	}
}
