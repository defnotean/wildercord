package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.world.level.material.PushReaction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Cinder Antler: one the Ashen Stag shed, lying smouldering at the foot of the burnt stump. Once the stag is dead, anyone may take
 * it up, once: from then on, after a good run, the first thing hostile they strike is gored the way the stag gores, thrown back and
 * set burning, and then the antler must cool before it will again. The antler itself stays for whoever comes after.
 */
public final class CinderAntler {
	private CinderAntler() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("cinder_antler"));
	public static final CinderAntlerBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new CinderAntlerBlock(BlockBehaviour.Properties.of()
		.setId(KEY).noOcclusion().strength(-1, 3600000).noLootTable().sound(SoundType.BONE_BLOCK).lightLevel(state -> 6)
		.pushReaction(PushReaction.IMMOVEABLE)));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("cinder_antler"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix().rarity(Rarity.EPIC)));

	/** How long each player has been sprinting, and when their antler last gored, in server ticks. Forgotten on restart. */
	private static final Map<UUID, Integer> SPRINT = new HashMap<>();
	private static final Map<UUID, Long> LAST = new HashMap<>();

	/** Whether the antler runs with this player. */
	public static boolean bound(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.CINDER_ANTLER, false);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var player : server.getPlayerList().getPlayers()) {
				if (!bound(player)) continue;
				if (player.isSprinting()) {
					int run = SPRINT.merge(player.getUUID(), 1, Integer::sum);
					// Ready: embers kicked up off its heels.
					if (run >= AshenStagRules.ANTLER_SPRINT && ready(player) && run % 3 == 0) {
						player.level().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + .1, player.getZ(), 2, .2, .05, .2, .01);
					}
				} else {
					SPRINT.remove(player.getUUID());
				}
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player || taken <= 0) return;
			if (!(entity instanceof Enemy) || !entity.isAlive() || !bound(player)) return;
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
		mob.knockback(AshenStagRules.ANTLER_SHOVE, player.getX() - mob.getX(), player.getZ() - mob.getZ(), source(player), 0.0F);
		mob.setDeltaMovement(mob.getDeltaMovement().add(0, .35, 0));
		mob.needsSync = true;
		mob.igniteForTicks(AshenStagRules.ANTLER_BURN);
		Feels.sound(level, mob.position(), "aura_ashen_stag_gore", 1.0F, 1.2F);
		Feels.sound(level, mob.position(), "aura_ashen_stag_smoulder", .8F, 1.3F);
		level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 16, .3, .4, .3, .06);
		level.sendParticles(ParticleTypes.LAVA, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 4, .2, .2, .2, 0);
		player.sendOverlayMessage(Component.translatable("message.wildercord.cinder_antler.gore").withColor(0xF08A3C));
	}

	private static net.minecraft.world.damagesource.DamageSource source(ServerPlayer player) {
		return player.damageSources().playerAttack(player);
	}
}
