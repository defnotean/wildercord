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
import net.minecraft.world.level.material.PushReaction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
		.setId(KEY).noOcclusion().strength(-1, 3600000).noLootTable().sound(SoundType.GLASS).lightLevel(state -> 5)
		.pushReaction(PushReaction.IMMOVEABLE)));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("rimescale"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix().rarity(Rarity.EPIC)));

	/** When each player's scale last bit back, in server ticks. Forgotten on restart, which only ever lets it bite sooner. */
	private static final Map<UUID, Long> LAST = new HashMap<>();

	/** Whether the scale is with this player. */
	public static boolean bound(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.RIMESCALE, false);
	}

	public static void init() {
		// The cold cannot touch its bearer.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(entity instanceof ServerPlayer player && source.is(DamageTypes.FREEZE) && bound(player)));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (!(entity instanceof ServerPlayer player) || taken <= 0 || !player.isAlive() || !bound(player)) return;
			// Only a blow struck by hand, tooth or claw: not an arrow, not a fall.
			if (!(source.getEntity() instanceof Mob mob) || mob != source.getDirectEntity() || !(mob instanceof Enemy) || !mob.isAlive()) return;
			long now = player.level().getServer().getTickCount();
			if (!FrostWyrmlingRules.rimes(now - LAST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2))) return;
			LAST.put(player.getUUID(), now);
			bite(player, mob);
		});
	}

	/** Frost bites back at whatever struck them: it is slowed, chilled through and stung, white with rime. */
	private static void bite(ServerPlayer player, Mob mob) {
		var level = player.level();
		var at = mob.position().add(0, mob.getBbHeight() / 2, 0);
		Feels.sound(level, at, "aura_frost_wyrmling_frost", 1.1F, 1.15F);
		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 24, mob.getBbWidth() * .5, mob.getBbHeight() * .4, mob.getBbWidth() * .5, .04);
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, 10, .3, .3, .3, .08);
		var from = player.position().add(0, 1, 0);
		var step = at.subtract(from);
		for (int i = 0; i <= 6; i++) {
			var p = from.add(step.scale(i / 6.0));
			level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 1, .05, .05, .05, 0);
		}
		mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FrostWyrmlingRules.RIME_SLOW, 1, false, true));
		if (mob.canFreeze()) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), FrostWyrmlingRules.RIME_FROST));
		mob.hurtServer(level, player.damageSources().freeze(), (float) FrostWyrmlingRules.RIME_DAMAGE);
		player.sendOverlayMessage(Component.translatable("message.wildercord.rimescale.rime").withColor(0xA8DCF4));
	}
}
