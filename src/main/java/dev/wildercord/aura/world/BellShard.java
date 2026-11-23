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
 * The Bell Shard: the broken lip of the Drowned Bell-Warden's bell, laid on the chapel's altar. Once the Warden is dead, anyone may
 * lay a hand on it, once: from then on, a blow that leaves them near death makes it toll for them, throwing back whatever hostile is
 * close and shielding them a few seconds, then it must ring itself quiet again. The shard itself stays for whoever comes down after.
 */
public final class BellShard {
	private BellShard() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("bell_shard"));
	public static final BellShardBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new BellShardBlock(BlockBehaviour.Properties.of()
		;
		}
		((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 30, 1.2, 0.1, 1.2, 0.3);
		double r = BellWardenRules.KNELL_RADIUS;
		for (var mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r), m -> m instanceof Enemy && m.isAlive() && m.distanceToSqr(player) <= r * r)) {
			mob.knockback(BellWardenRules.KNELL_SHOVE, player.getX() - mob.getX(), player.getZ() - mob.getZ());
			mob.hasImpulse = true;
		}
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BellWardenRules.KNELL_SHIELD, 1, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BellWardenRules.KNELL_GUARD_TICKS, 0, false, true));
		player.displayClientMessage(Component.translatable("message.wildercord.bell_shard.knell").withColor(0x9FE0CC), true);
	}
}
