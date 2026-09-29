package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Affinity;
import dev.wildercord.spell.Bestiary;
import dev.wildercord.spell.Leaning;
import dev.wildercord.spell.PlayerAffinity;
import dev.wildercord.spell.PlayerAffinity.Source;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players' own affinities with the elements, at runtime (the rules are {@link PlayerAffinity}). Points
 * come in through {@link #gain}, which keeps each way of earning them to its daily allowance, scales them by
 * the server's {@code affinity.gain_multiplier}, and announces each new level (a message, a toast, and the
 * first level of each element in the Grimoire). What earns them:
 * <ul>
 *   <li>casting ({@link #onCast}, by the mana each element's effects cost), reactions ({@link #reaction})
 *       and the Bestiary's weaknesses and Torn Pages ({@link #discovered}), all told by {@code SpellCaster}
 *       and {@link Grimoire};</li>
 *   <li>blocks broken (stone and ores with the right tool, ripe crops), deaths (kills with fire, in
 *       melee, of the End's creatures) and harm survived (lightning, a long fall, a heavy blow), through
 *       Fabric's events;</li>
 *   <li>a check every {@link PlayerAffinity#SAMPLE_TICKS} ticks per player (spread over the ticks) of where
 *       they stand and what they're doing, and of the game's own statistics for what it already counts
 *       (fish caught, animals bred, enchantments, ender pearls thrown, elytra flight);</li>
 *   <li>and a few small hooks: {@code FurnaceResultSlotMixin} (smelting), {@code TameAnimalTriggerMixin}
 *       (taming), {@code LivingEntityHealMixin} (a spell healing someone else), {@code LightningRodBlockMixin},
 *       {@code RuneItem} (a rune learned) and {@code WorldMagic} (time aging the world).</li>
 * </ul>
 * What they give is read where it applies: {@link #power} in {@code Effects} (every effect's power),
 * {@link #resistance} in {@link Affinities} (others' spells landing on a player), and the price at V in
 * {@code Heart}. Creative and spectating players earn nothing; with {@code features.player_affinity} off,
 * nobody earns anything and nothing gives anything.
 */
public final class PlayerAffinities {
	private PlayerAffinities() {}

	/** Server to client: an affinity reached a new level (the client shows a toast in the element's colour). */
	public record Rise(String element, int level) implements CustomPacketPayload {
		public static final Type<Rise> TYPE = new Type<>(Wildercord.id("affinity_rise"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Rise> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32), Rise::element, ByteBufCodecs.VAR_INT, Rise::level, Rise::new).cast();

		@Override
		public Type<Rise> type() {
			return TYPE;
		}
	}

	/** At most one reaction a second earns points: a crowd shattering at once is one reaction's worth. */
	private static final int REACTION_TICKS = 20;
	/** One lightning strike counts once, however it touches you. */
	private static final int STRUCK_TICKS = 40;
	/** A fall must hurt this much (3 hearts) to count, survived. */
	private static final float LONG_FALL = 6.0F;
	/** A blow must hurt this much (4 hearts) to count, survived. */
	private static final float HEAVY_BLOW = 8.0F;
	/** Lava within this many blocks, in the Nether. */
	private static final int LAVA_REACH = 3;
	/** A lightning rod struck within this many blocks. */
	private static final double ROD_REACH = 16;
	/** Above this height, the wind. */
	private static final int HIGH = 200;
	/** Elytra flight for one unit of gliding: 100 blocks, in the centimetres the statistic counts. */
	private static final int GLIDE_CM = 10000;
	/** Points never go past this (far past level V: nothing to overflow). */
	private static final int MAX_POINTS = 1_000_000;

	/** Fractions of a point not yet whole, per player and element (lost on leaving, like condensing: always under one). */
	private static final Map<UUID, double[]> REMAINDER = new ConcurrentHashMap<>();
	/** The statistics last read for each player: fish caught, animals bred, enchantments, pearls thrown, elytra flight. */
	private static final Map<UUID, int[]> STATS = new ConcurrentHashMap<>();
	/** Checks spent out under the night sky so far tonight, per player (for watching a whole night). */
	private static final Map<UUID, Integer> NIGHT = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_REACTION = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_STRUCK = new ConcurrentHashMap<>();

	private static final Set<EntityType<?>> OF_THE_END = Set.of(EntityTypes.ENDERMAN, EntityTypes.ENDERMITE, EntityTypes.SHULKER);

	// ------------------------------------------------------------------ what affinities give

	/** The power on an effect of {@code element} its caster's affinity gives: 1 for monsters, and with affinities off. */
	public static double power(LivingEntity caster, String element) {
		if (element.isEmpty() || !(caster instanceof ServerPlayer player) || !Config.get().playerAffinity()) {
			return 1.0;
		}
		return PlayerAffinity.power(Heart.affinityLevel(player, element));
	}

	/**
	 * What of a spell hit of {@code element} lands on a player: from level III they shrug a little off. Like
	 * a creature's resistance, it doesn't hold against a hit that set off a reaction ({@code reacted}), and
	 * there's no callout: it's quiet on purpose.
	 */
	static double resistance(Player target, LivingEntity caster, String element, boolean reacted) {
		if (reacted || caster == target || !Config.get().playerAffinity()) {
			return 1.0;
		}
		return PlayerAffinity.damageTaken(Heart.affinityLevel(target, element));
	}

	// ------------------------------------------------------------------ what earns points

	/**
	 * A spell paid for: each element earns by the mana its effects made up of what was spent (a Blood Price
	 * counts as the mana it stood for), and a Blood Price's health feeds blood besides.
	 */
	public static void onCast(ServerPlayer player, SpellPlan.Segment root, int spent, int health) {
		if (spent > 0) {
			for (Map.Entry<String, Double> share : SpellCompiler.elementShares(root).entrySet()) {
				gain(player, Source.CAST, share.getKey(), spent * share.getValue());
			}
		}
		if (health > 0) {
			gain(player, Source.BLOOD_PRICE, "", health);
		}
	}

	/** A reaction set off: each element in it earns (at most one reaction a second counts). */
	static void reaction(ServerPlayer player, String reaction) {
		long now = player.level().getGameTime();
		Long last = LAST_REACTION.get(player.getUUID());
		if (last != null && now - last < REACTION_TICKS && now >= last) {
			return;
		}
		LAST_REACTION.put(player.getUUID(), now);
		for (String element : PlayerAffinity.REACTION_ELEMENTS.getOrDefault(reaction, java.util.List.of())) {
			gain(player, Source.REACTION, element, 1);
		}
	}

	/** A new Grimoire entry: a creature's weakness found feeds that element, and a Torn Page's riddle arcane. */
	static void discovered(ServerPlayer player, String key) {
		if (key.startsWith("hint:")) {
			gain(player, Source.PAGE, "", 1);
			return;
		}
		Bestiary.parse(key).filter(entry -> entry.kind() == Bestiary.Kind.WEAK).ifPresent(entry -> gain(player, Source.BESTIARY, entry.element(), 1));
	}

	/** A rune learned for the first time. */
	public static void learnedRune(ServerPlayer player) {
		gain(player, Source.RUNE, "", 1);
	}

	/** Items taken out of a furnace, blast furnace or smoker (by hand: a hopper earns nobody anything). */
	public static void smelted(ServerPlayer player, int items) {
		gain(player, Source.SMELT, "", items);
	}

	/** An animal tamed (a wolf, a cat, a horse, a parrot...). */
	public static void tamed(ServerPlayer player) {
		gain(player, Source.TAME, "", 1);
	}

	/** {@code healer}'s spell restored {@code amount} health to someone else. */
	public static void healed(ServerPlayer healer, LivingEntity target, float amount) {
		if (target != healer && amount > 0) {
			gain(healer, Source.HEAL, "", amount);
		}
	}

	/** Time magic aged {@code things} (crops, babies, copper, furnaces) where it landed. */
	static void aged(LivingEntity caster, int things) {
		if (caster instanceof ServerPlayer player && things > 0) {
			gain(player, Source.AGE, "", things);
		}
	}

	/** A lightning rod was struck: everyone near it feels the storm. */
	public static void rodStruck(ServerLevel level, BlockPos pos) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= ROD_REACH * ROD_REACH) {
				gain(player, Source.ROD, "", 1);
			}
		}
	}

	/**
	 * Earns {@code units} of {@code source} (for a general source, toward {@code element}): as much of it as
	 * today's allowance lets through, times the server's gain multiplier.
	 *
	 * @return whether anything was earned
	 */
	public static boolean gain(ServerPlayer player, Source source, String element, double units) {
		String e = source.general() ? element : source.element;
		if (units <= 0 || !PlayerAffinity.isElement(e) || !Config.get().playerAffinity() || player.isCreative() || player.isSpectator()) {
			return false;
		}
		MinecraftServer server = player.level().getServer();
		long today = PlayerAffinity.day(server.overworld().getGameTime());
		String key = source.tallyKey(e);
		WildercordAttachments.AffinityTally tally = player.getAttachedOrElse(WildercordAttachments.AFFINITY_TALLY, WildercordAttachments.AffinityTally.NONE);
		double offered = units * source.points;
		double earned = tally.earned(today, key);
		double granted = PlayerAffinity.grant(source, earned, offered);
		player.setAttached(WildercordAttachments.AFFINITY_TALLY, tally.with(today, key, earned + offered));
		double scaled = granted * Config.get().affinityGain();
		if (scaled <= 0) {
			return false;
		}
		add(player, e, scaled);
		return true;
	}

	/** Adds points (whole ones to the attachment, the rest kept for next time) and announces any level reached. */
	private static void add(ServerPlayer player, String element, double points) {
		int index = Affinity.ELEMENTS.indexOf(element);
		double[] remainder = REMAINDER.computeIfAbsent(player.getUUID(), id -> new double[Affinity.ELEMENTS.size()]);
		double total = remainder[index] + points;
		int whole = (int) Math.min(MAX_POINTS, Math.floor(total));
		remainder[index] = Math.min(1, total - whole);
		if (whole <= 0) {
			return;
		}
		Map<String, Integer> before = Heart.affinity(player);
		int had = before.getOrDefault(element, 0);
		int now = (int) Math.min(MAX_POINTS, (long) had + whole);
		if (now == had) {
			return;
		}
		Map<String, Integer> after = new HashMap<>(before);
		after.put(element, now);
		player.setAttached(WildercordAttachments.AFFINITY, Map.copyOf(after));
		for (int level = PlayerAffinity.level(had) + 1; level <= PlayerAffinity.level(now); level++) {
			rise(player, element, level);
		}
		String leanedBefore = Leaning.of(before);
		String leaning = Leaning.of(after);
		if (!leaning.isEmpty() && !leaning.equals(leanedBefore)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.leaning", Component.translatable("element.wildercord." + leaning))
				.withColor(RuneColors.element(leaning)));
			Grimoire.feat(player, dev.wildercord.spell.Feats.LEANING);
		}
	}

	/** A new level: a message saying what it gives, a toast, a chime, and at level I an entry in the Grimoire. */
	private static void rise(ServerPlayer player, String element, int level) {
		Component name = Component.translatable("element.wildercord." + element);
		int power = (int) Math.round(PlayerAffinity.POWER_PER_LEVEL * level * 100);
		int resist = (int) Math.round(PlayerAffinity.resistance(level) * 100);
		Component message;
		if (level == 1) {
			message = Component.translatable("message.wildercord.affinity.first", name, power);
		} else if (level >= PlayerAffinity.MAX_LEVEL) {
			message = Component.translatable("message.wildercord.affinity.mastered", name, power, Math.round(PlayerAffinity.DISCOUNT * 100), resist);
		} else if (level >= PlayerAffinity.RESIST_FROM) {
			message = Component.translatable("message.wildercord.affinity.resist", name, dev.wildercord.content.RuneItem.roman(level), power, resist);
		} else {
			message = Component.translatable("message.wildercord.affinity.rise", name, dev.wildercord.content.RuneItem.roman(level), power);
		}
		player.sendSystemMessage(message.copy().withColor(RuneColors.element(element)));
		Fx.sound(player.level(), player.position(), dev.wildercord.content.WildercordSounds.DISCOVERY, 0.8F, 0.9F + 0.08F * level);
		Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 0.8F + 0.1F * level);
		if (level == 1) {
			// A small entry, like a feat: some mana toward the next circle, and no second toast.
			Grimoire.unlock(player, PlayerAffinity.KEY_PREFIX + element, false);
		}
		if (ServerPlayNetworking.canSend(player, Rise.TYPE)) {
			ServerPlayNetworking.send(player, new Rise(element, level));
		}
	}

	/**
	 * A caster from before affinities: the casts leaning counted per element become a start (never past
	 * level II), quietly, with each first level written into the Grimoire. Once only: afterwards the
	 * affinity attachment exists.
	 */
	static void seed(ServerPlayer player) {
		if (player.hasAttached(WildercordAttachments.AFFINITY)) {
			return;
		}
		Map<String, Integer> points = new HashMap<>();
		for (Map.Entry<String, Integer> casts : Heart.elementCasts(player).entrySet()) {
			if (PlayerAffinity.isElement(casts.getKey()) && PlayerAffinity.seed(casts.getValue()) > 0) {
				points.put(casts.getKey(), PlayerAffinity.seed(casts.getValue()));
			}
		}
		player.setAttached(WildercordAttachments.AFFINITY, Map.copyOf(points));
		for (Map.Entry<String, Integer> entry : points.entrySet()) {
			if (PlayerAffinity.level(entry.getValue()) >= 1) {
				Grimoire.unlock(player, PlayerAffinity.KEY_PREFIX + entry.getKey(), false);
			}
		}
	}

	// ------------------------------------------------------------------ the world's events

	private static void afterBreak(Level level, Player who, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(who instanceof ServerPlayer player)) {
			return;
		}
		if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
				|| state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
			gain(player, Source.HARVEST, "", 1);
			return;
		}
		// Stone and ores that need a pickaxe, mined with one.
		if (!state.requiresCorrectToolForDrops() || !player.hasCorrectToolForDrops(state)) {
			return;
		}
		if (state.is(ConventionalBlockTags.ORES)) {
			gain(player, Source.ORE, "", 1);
		} else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
			gain(player, Source.STONE, "", 1);
		}
	}

	private static void afterDamage(LivingEntity entity, DamageSource source, float damage) {
		if (!(entity instanceof ServerPlayer player) || !player.isAlive() || damage <= 0) {
			return;
		}
		if (source.is(DamageTypes.LIGHTNING_BOLT) && source.getEntity() == null) {
			// A real bolt (a spell's lightning has its caster behind it), once per strike.
			long now = player.level().getGameTime();
			Long last = LAST_STRUCK.get(player.getUUID());
			if (last == null || now - last >= STRUCK_TICKS || now < last) {
				LAST_STRUCK.put(player.getUUID(), now);
				gain(player, Source.STRUCK, "", 1);
			}
		} else if (source.is(DamageTypeTags.IS_FALL)) {
			if (damage >= LONG_FALL) {
				gain(player, Source.FALL, "", 1);
			}
		} else if (damage >= HEAVY_BLOW && source.getEntity() != null && source.getEntity() != player) {
			gain(player, Source.HEAVY_HIT, "", 1);
		}
	}

	private static void afterDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof Player) {
			// Other players are never a way to grow.
			return;
		}
		// Whoever struck it, or failing that (it burnt, or fell), whoever the game credits with the kill.
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer striker ? striker
			: entity.getKillCredit() instanceof ServerPlayer credited ? credited : null;
		if (killer == null) {
			return;
		}
		if (source.is(DamageTypeTags.IS_FIRE) || entity.isOnFire()) {
			gain(killer, Source.FIRE_KILL, "", 1);
		}
		if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == killer) {
			gain(killer, Source.MELEE_KILL, "", 1);
		}
		if (OF_THE_END.contains(entity.getType())) {
			gain(killer, Source.VOID_KILL, "", 1);
		}
	}

	// ------------------------------------------------------------------ the regular check

	/** Where the player is and what they're doing, every {@link PlayerAffinity#SAMPLE_TICKS} ticks. */
	private static void check(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		statistics(player);
		if (player.isCreative()) {
			return;
		}
		ServerLevel level = player.level();
		BlockPos feet = player.blockPosition();
		Set<dev.wildercord.spell.ClimateRules.Condition> here = Climate.conditions(player);
		// Fire: burning (and not shielded from it), or close to lava in the Nether.
		if (player.isOnFire() && !player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			gain(player, Source.BURNING, "", 1);
		}
		if (here.contains(dev.wildercord.spell.ClimateRules.Condition.NETHER) && nearLava(level, feet)) {
			gain(player, Source.LAVA, "", 1);
		}
		// Frost: snowy lands or ice underfoot; frozen stiff.
		if (here.contains(dev.wildercord.spell.ClimateRules.Condition.SNOW) || level.getBlockState(feet.below()).is(BlockTags.ICE)) {
			gain(player, Source.COLD, "", 1);
		}
		if (player.isFullyFrozen()) {
			gain(player, Source.FROZEN, "", 1);
		}
		// Storm: out under a thunderstorm.
		if (here.contains(dev.wildercord.spell.ClimateRules.Condition.THUNDER)) {
			gain(player, Source.THUNDER, "", 1);
		}
		// Wind: high over the world.
		if (level.dimension() == Level.OVERWORLD && player.getY() > HIGH) {
			gain(player, Source.HEIGHTS, "", 1);
		}
		// Earth: down in the deep.
		if (here.contains(dev.wildercord.spell.ClimateRules.Condition.DEEP)) {
			gain(player, Source.DEEP, "", 1);
		}
		// Void: the End, and the deep dark.
		if (here.contains(dev.wildercord.spell.ClimateRules.Condition.END)) {
			gain(player, Source.END, "", 1);
		}
		if (level.getBiome(feet).is(Biomes.DEEP_DARK)) {
			gain(player, Source.DEEP_DARK, "", 1);
		}
		// Arcane: meditating, and standing on a ley line.
		if (player.getAttachedOrElse(WildercordAttachments.MEDITATING, false)) {
			gain(player, Source.MEDITATE, "", 1);
		}
		if (LeyWalker.onLine(player)) {
			gain(player, Source.LEY, "", 1);
		}
		// Time: a clock in hand, and a whole night watched out under the sky.
		if (player.getMainHandItem().is(Items.CLOCK) || player.getOffhandItem().is(Items.CLOCK)) {
			gain(player, Source.CLOCK, "", 1);
		}
		nightWatch(player, here.contains(dev.wildercord.spell.ClimateRules.Condition.NIGHT), level);
	}

	private static boolean nearLava(ServerLevel level, BlockPos feet) {
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-LAVA_REACH, -2, -LAVA_REACH), feet.offset(LAVA_REACH, 2, LAVA_REACH))) {
			if (level.getFluidState(pos).is(FluidTags.LAVA)) {
				return true;
			}
		}
		return false;
	}

	/** Counts the checks out under the night sky; when day comes, a night spent out nearly whole earns its reward. */
	private static void nightWatch(ServerPlayer player, boolean outUnderNight, ServerLevel level) {
		UUID id = player.getUUID();
		if (outUnderNight) {
			NIGHT.merge(id, 1, Integer::sum);
			return;
		}
		long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
		boolean night = level.dimensionType().hasFixedTime() || time >= 13000 && time < 23000;
		if (night) {
			// Indoors, or somewhere without a sky, for a while: that check just doesn't count.
			return;
		}
		Integer watched = NIGHT.remove(id);
		if (watched != null && PlayerAffinity.nightWatched(watched, PlayerAffinity.NIGHT_CHECKS)) {
			gain(player, Source.NIGHT_WATCH, "", 1);
		}
	}

	/**
	 * What the game's own statistics counted since the last check: fish caught, animals bred, items
	 * enchanted, ender pearls thrown and elytra flight. The first check after joining only takes note.
	 */
	private static void statistics(ServerPlayer player) {
		var stats = player.getStats();
		int[] now = {
			stats.getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT)),
			stats.getValue(Stats.CUSTOM.get(Stats.ANIMALS_BRED)),
			stats.getValue(Stats.CUSTOM.get(Stats.ENCHANT_ITEM)),
			stats.getValue(Stats.ITEM_USED.get(Items.ENDER_PEARL)),
			stats.getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM))};
		int[] last = STATS.put(player.getUUID(), now);
		if (last == null) {
			return;
		}
		gain(player, Source.FISH, "", Math.max(0, now[0] - last[0]));
		gain(player, Source.BREED, "", Math.max(0, now[1] - last[1]));
		gain(player, Source.ENCHANT, "", Math.max(0, now[2] - last[2]));
		gain(player, Source.PEARL, "", Math.max(0, now[3] - last[3]));
		int flown = Math.max(0, now[4] - last[4]);
		if (flown > 0) {
			gain(player, Source.GLIDE, "", flown / (double) GLIDE_CM);
		}
	}

	private static void forget(UUID id) {
		REMAINDER.remove(id);
		STATS.remove(id);
		NIGHT.remove(id);
		LAST_REACTION.remove(id);
		LAST_STRUCK.remove(id);
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Rise.TYPE, Rise.CODEC);
		PlayerBlockBreakEvents.AFTER.register(PlayerAffinities::afterBreak);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> afterDamage(entity, source, damage));
		ServerLivingEntityEvents.AFTER_DEATH.register(PlayerAffinities::afterDeath);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				// Each player on a tick of their own, so a full server's checks are spread out.
				if (Math.floorMod(tick + player.getUUID().hashCode(), PlayerAffinity.SAMPLE_TICKS) == 0) {
					check(player);
				}
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> seed(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			REMAINDER.clear();
			STATS.clear();
			NIGHT.clear();
			LAST_REACTION.clear();
			LAST_STRUCK.clear();
		});
	}
}
