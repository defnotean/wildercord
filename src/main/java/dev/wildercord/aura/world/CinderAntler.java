package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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
 * The Cinder Antler: one the Ashen Stag shed, lying smouldering at the foot of the burnt stump. Once the stag is dead, anyone may take
 * it up, once: from then on, after a good run, the first thing hostile they strike is gored the way the stag gores, thrown back and
 * set burning, and then the antler must cool before it will again. The antler itself stays for whoever comes after.
 */
public final class CinderAntler {
	private CinderAntler() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("cinder_antler"));
	public static final CinderAntlerBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new CinderAntlerBlock(BlockBehaviour.Properties.of()
		 || !entity.isAlive() || !bound(player)) return;
			long since = player.level().getServer().getTickCount() - LAST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
			if (AshenStagRules.gores(SPRINT.getOrDefault(player.getUUID(), 0), since)) gore(player, entity);
		});
	}

	private static boolean ready(ServerPlayer player) {
		return player.level().getServer().getTickCount() - LAST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) >= AshenStagRules.ANTLER_RECHARGE;
	}

	/** Gored: thrown back off them and set burning, with the stag's own crack of antlers. */
	private static void gore(ServerPlayer player, LivingEntity mob) {
		var level = player.level();
		LAST.put(player.getUUID(), (long) level.getServer().getTickCount());
		SPRINT.remove(player.getUUID());
		mob.knockback(AshenStagRules.ANTLER_SHOVE, player.getX() - mob.getX(), player.getZ() - mob.getZ());
		mob.setDeltaMovement(mob.getDeltaMovement().add(0, .35, 0));
		mob.hasImpulse = true;
		mob.igniteForTicks(AshenStagRules.ANTLER_BURN);
		Feels.sound((ServerLevel) level, mob.position(), "aura_ashen_stag_gore", 1.0F, 1.2F);
		Feels.sound((ServerLevel) level, mob.position(), "aura_ashen_stag_smoulder", .8F, 1.3F);
		((ServerLevel) level).sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 16, .3, .4, .3, .06);
		((ServerLevel) level).sendParticles(ParticleTypes.LAVA, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 4, .2, .2, .2, 0);
		player.displayClientMessage(Component.translatable("message.wildercord.cinder_antler.gore").withColor(0xF08A3C), true);
	}

	private static net.minecraft.world.damagesource.DamageSource source(ServerPlayer player) {
		return player.damageSources().playerAttack(player);
	}
}
