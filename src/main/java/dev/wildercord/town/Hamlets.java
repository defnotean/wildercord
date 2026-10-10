package dev.wildercord.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.town.BountyRules.Tier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every village as a hamlet with its own name and standing. See {@link HamletRules}. */
public final class Hamlets {
	private Hamlets() {}

	/** One hamlet's standing with a traveller: its reputation, and what it gave them on {@code day}. */
	public record Standing(int reputation, long day, int today) {
		public static final Standing NONE = new Standing(0, -1, 0);
		public static final Codec<Standing> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("reputation").forGetter(Standing::reputation),
			Codec.LONG.fieldOf("day").forGetter(Standing::day),
			Codec.INT.fieldOf("today").forGetter(Standing::today)).apply(i, Standing::new));

		public Tier tier() {
			return Tier.of(reputation);
		}
	}

	/** Each hamlet's standing, by its bell ({@code dimension|pos}). Kept through death. */
	public static final AttachmentType<Map<String, Standing>> STANDINGS = AttachmentRegistry.create(Wildercord.id("hamlet_standings"),
		builder -> builder.initializer(Map::of).persistent(Codec.unboundedMap(Codec.STRING, Standing.CODEC)).copyOnDeath());

	/** The hamlet each traveller was last in, so they're greeted only on the way in. */
	private static final Map<UUID, String> IN = new HashMap<>();

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Enemy && source.getEntity() instanceof ServerPlayer player && entity.level() instanceof ServerLevel level) {
				BlockPos bell = bell(level, entity.blockPosition());
				if (bell != null) earn(player, level, bell, HamletRules.DEFEND);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) visit(player);
		});
	}

	/** The bell of the hamlet {@code pos} lies in, or null outside any. */
	public static BlockPos bell(ServerLevel level, BlockPos pos) {
		BlockPos bell = level.getPoiManager().findClosest((Holder<PoiType> t) -> t.is(PoiTypes.MEETING), pos, HamletRules.RADIUS, PoiManager.Occupancy.ANY)
			.orElse(null);
		if (bell == null || !level.getBlockState(bell).is(Blocks.BELL)) return null;
		return level.getEntitiesOfClass(Villager.class, new AABB(bell).inflate(HamletRules.RADIUS / 2.0), Villager::isAlive).size() >= HamletRules.VILLAGERS
			? bell : null;
	}

	static String key(ServerLevel level, BlockPos bell) {
		return level.dimension().identifier() + "|" + bell.asLong();
	}

	public static String name(BlockPos bell) {
		return HamletRules.name(bell.asLong());
	}

	public static Standing standing(ServerPlayer player, ServerLevel level, BlockPos bell) {
		return player.getAttachedOrElse(STANDINGS, Map.of()).getOrDefault(key(level, bell), Standing.NONE);
	}

	/** A villager traded with {@code player}: their hamlet thinks a little better of them. */
	public static void traded(ServerPlayer player, Villager villager) {
		if (!(villager.level() instanceof ServerLevel level)) return;
		BlockPos bell = bell(level, villager.blockPosition());
		if (bell != null) earn(player, level, bell, HamletRules.TRADE);
	}

	/** Adds reputation with the hamlet at {@code bell}, up to its daily limit, and says so when the standing rises. */
	public static void earn(ServerPlayer player, ServerLevel level, BlockPos bell, int gain) {
		long day = level.getGameTime() / BountyRules.DAY;
		Standing old = standing(player, level, bell);
		int today = old.day() == day ? old.today() : 0;
		int earned = HamletRules.earned(today, gain);
		if (earned <= 0) return;
		Standing now = new Standing(old.reputation() + earned, day, today + earned);
		Map<String, Standing> all = new HashMap<>(player.getAttachedOrElse(STANDINGS, Map.of()));
		all.put(key(level, bell), now);
		player.setAttached(STANDINGS, all);
		if (now.tier() != old.tier()) {
			player.sendSystemMessage(Component.translatable("message.wildercord.hamlet.tier_up", name(bell),
				BountyBoardBlock.tierName(now.tier())).withStyle(ChatFormatting.LIGHT_PURPLE));
			favour(player, now.tier());
		}
	}

	/** Greets a traveller coming into a hamlet, and keeps its favour on them while they stay. */
	static void visit(ServerPlayer player) {
		if (player.isSpectator()) return;
		BlockPos bell = bell(player.level(), player.blockPosition());
		if (bell == null) {
			IN.remove(player.getUUID());
			return;
		}
		Standing standing = standing(player, player.level(), bell);
		String key = key(player.level(), bell);
		if (!key.equals(IN.put(player.getUUID(), key))) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.hamlet.enter", name(bell),
				BountyBoardBlock.tierName(standing.tier())).withStyle(ChatFormatting.GOLD));
		}
		favour(player, standing.tier());
	}

	private static void favour(ServerPlayer player, Tier tier) {
		int level = HamletRules.favour(tier);
		if (level < 0) return;
		MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
		if (hero != null && hero.getAmplifier() > level) return;
		player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, HamletRules.FAVOUR_TICKS, level, true, false, true));
	}
}
