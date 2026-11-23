package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * The Rimescale: a scale the Frost Wyrmling shed, standing up in the snow of its mound, white with frost. Once the Wyrmling is dead,
 * anyone may take it up, once: from then on the cold cannot touch them, and whatever hostile strikes them in melee is bitten by frost
 * in return, slowed and chilled through; then the scale must rime over again before it will bite. The scale itself stays for whoever
 * comes after.
 */
public final class Rimescale {
	private Rimescale() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("rimescale"));
	public static final RimescaleBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new RimescaleBlock(BlockBehaviour.Properties.of()
		 {
		var level = player.level();
		var at = mob.position().add(0, mob.getBbHeight() / 2, 0);
		Feels.sound((ServerLevel) level, at, "aura_frost_wyrmling_frost", 1.1F, 1.15F);
		((ServerLevel) level).sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 24, mob.getBbWidth() * .5, mob.getBbHeight() * .4, mob.getBbWidth() * .5, .04);
		((ServerLevel) level).sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, 10, .3, .3, .3, .08);
		var from = player.position().add(0, 1, 0);
		var step = at.subtract(from);
		for (int i = 0; i <= 6; i++) {
			var p = from.add(step.scale(i / 6.0));
			((ServerLevel) level).sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 1, .05, .05, .05, 0);
		}
		mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FrostWyrmlingRules.RIME_SLOW, 1, false, true));
		if (mob.canFreeze()) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), FrostWyrmlingRules.RIME_FROST));
		mob.hurt(player.damageSources().freeze(), (float) FrostWyrmlingRules.RIME_DAMAGE);
		player.displayClientMessage(Component.translatable("message.wildercord.rimescale.rime").withColor(0xA8DCF4), true);
	}
}
