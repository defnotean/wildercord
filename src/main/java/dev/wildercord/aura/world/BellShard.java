package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.world.level.material.PushReaction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Bell Shard: the broken lip of the Drowned Bell-Warden's bell, laid on the chapel's altar. Once the Warden is dead, anyone may
 * lay a hand on it, once: from then on, a blow that leaves them near death makes it toll for them, throwing back whatever hostile is
 * close and shielding them a few seconds, then it must ring itself quiet again. The shard itself stays for whoever comes down after.
 */
public final class BellShard {
	private BellShard() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("bell_shard"));
	public static final BellShardBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new BellShardBlock(BlockBehaviour.Properties.of()
		.setId(KEY).noOcclusion().strength(-1, 3600000).noLootTable().sound(SoundType.METAL).lightLevel(state -> 4)
		.pushReaction(PushReaction.IMMOVEABLE)));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("bell_shard"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix().rarity(Rarity.EPIC)));

	/** When each player's knell last tolled, in server ticks. Forgotten on restart, which only ever lets it toll sooner. */
	private static final Map<UUID, Long> LAST = new HashMap<>();

	/** Whether the shard tolls for this player. */
	public static boolean bound(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.BELL_SHARD, false);
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (!(entity instanceof ServerPlayer player) || taken <= 0 || !player.isAlive() || !bound(player)) return;
			long now = player.level().getServer().getTickCount();
			long since = now - LAST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
			if (BellWardenRules.knells(player.getHealth(), player.getMaxHealth(), since)) {
				LAST.put(player.getUUID(), now);
				knell(player);
			}
		});
	}

	/** It tolls: everything hostile close by is thrown back off them, and they are shielded a few seconds. */
	private static void knell(ServerPlayer player) {
		var level = player.level();
		var at = player.position();
		Feels.sound(level, at.add(0, 1, 0), "aura_bell_warden_knell", 1.3F, 1.25F);
		for (int i = 0; i < 32; i++) {
			double a = i * Math.PI * 2 / 32;
			level.sendParticles(ParticleTypes.GLOW, at.x + Math.cos(a) * 1.5, at.y + 0.2, at.z + Math.sin(a) * 1.5, 1, 0, 0.05, 0, 0);
		}
		level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 30, 1.2, 0.1, 1.2, 0.3);
		double r = BellWardenRules.KNELL_RADIUS;
		for (var mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r), m -> m instanceof Enemy && m.isAlive() && m.distanceToSqr(player) <= r * r)) {
			mob.knockback(BellWardenRules.KNELL_SHOVE, player.getX() - mob.getX(), player.getZ() - mob.getZ(), player.damageSources().playerAttack(player), 0.0F);
			mob.needsSync = true;
		}
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BellWardenRules.KNELL_SHIELD, 1, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, BellWardenRules.KNELL_GUARD_TICKS, 0, false, true));
		player.sendOverlayMessage(Component.translatable("message.wildercord.bell_shard.knell").withColor(0x9FE0CC));
	}
}
