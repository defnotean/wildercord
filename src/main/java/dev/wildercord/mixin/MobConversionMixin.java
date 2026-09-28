package dev.wildercord.mixin;

import dev.wildercord.runesmith.DuplicateSwap;
import dev.wildercord.runesmith.Runesmith;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Runesmith turned into something else (a zombie villager keeps its trades) leaves its buyback and reroll offers behind. */
@Mixin(Mob.class)
public abstract class MobConversionMixin {
	@Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
		at = @At("HEAD"))
	private <T extends Mob> void wildercord$stripSwaps(EntityType<T> type, ConversionParams params, EntitySpawnReason reason,
			ConversionParams.AfterConversion<T> after, CallbackInfoReturnable<T> cir) {
		if ((Object) this instanceof Villager villager && Runesmith.is(villager)) {
			DuplicateSwap.strip(villager);
		}
	}
}
