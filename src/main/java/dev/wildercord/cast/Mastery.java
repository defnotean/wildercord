package dev.wildercord.cast;

import dev.wildercord.api.SpellMasteryApi;
import dev.wildercord.config.Config;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.MasterySigil;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.MasteryTraits.Hook;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Spell mastery at runtime: spells that grow with their caster. Every spell cast from a Cord carries a {@link Tally} of
 * what it's learning (shared by its links, pulses and echoes), which this class fills from the cast's real outcomes:
 * {@link #afterDamage} for a foe struck, {@link #healed} for health restored, {@link #afterHit} for an ally helped or a
 * spell that moves you or works the world. What it earns waits here and is written into the {@link MasteryBook} once a
 * second, which is also when ranks are reached, traits offered (see {@link MasteryChoices}) and the public
 * {@link MasteryAttachments.Look} kept up to date.
 *
 * <p>The traits a player chose work through the ordinary cast: a price or cooldown factor at the gate
 * ({@link #costFactor}, {@link #cooldownFactor}), a bonus inside {@code Effects.hurt} that the spell-defence cap holds
 * ({@link #damageBonus}), and small extras on what the spell strikes or helps. The pure numbers are
 * {@link MasteryRules}; the catalogue is {@link MasteryTraits}.</p>
 */
public final class Mastery {
	private Mastery() {}

	// ------------------------------------------------------------------ what a cast is learning

	/** What one cast is learning: shared by every part of it, and every copy paid for with it. */
	public static final class Tally {
		final UUID owner;
		final String key;
		final List<RuneDef> runes;
		final Set<String> traits;
		/** False for a cast that learns nothing (a scroll's): its traits still work. */
		final boolean learns;
		final double situation;
		final boolean danger;
		final int foes;
		/** The circumstances it was cast in; struck kinds of foe (undead, a boss) are added as they come. */
		final Set<String> circumstances;
		final long place;
		double earned;
		boolean counted;
		final Set<String> countedIn = new HashSet<>();
		final Set<UUID> struck = new HashSet<>();
		final Set<UUID> helped = new HashSet<>();
		final Set<String> kinds = new HashSet<>();
		boolean selfHelped;
		boolean utility;
		boolean residue;
		double drunk;
		int harvested;
		int thanked;
		boolean chaining;

		Tally(UUID owner, String key, List<RuneDef> runes, Collection<String> traits, boolean learns, double situation, boolean danger, int foes,
				Set<String> circumstances, long place) {
			this.owner = owner;
			this.key = key;
			this.runes = List.copyOf(runes);
			this.traits = Set.copyOf(traits);
			this.learns = learns;
			this.situation = situation;
			this.danger = danger;
			this.foes = foes;
			this.circumstances = new LinkedHashSet<>(circumstances);
			this.place = place;
		}

		public String key() {
			return key;
		}

		public Set<String> traits() {
			return traits;
		}

		boolean has(Hook hook) {
			for (String id : traits) {
				if (MasteryTraits.get(id).map(t -> t.hook() == hook).orElse(false)) {
					return true;
				}
			}
			return false;
		}
	}

	/** A spell's identity: its rune ids as it fires (Knots untied), in order. The same on both sides. */
	public static String keyOf(List<RuneDef> runes) {
		return MasteryRules.key(Knots.flatten(runes).stream().map(RuneDef::id).toList());
	}

	/** {@code player}'s record of the spell made of {@code runes}, or null (works on both sides, from the synced book). */
	public static MasteryBook.Entry entry(Player player, List<RuneDef> runes) {
		if (runes.isEmpty()) {
			return null;
		}
		return MasteryAttachments.book(player).entry(keyOf(runes)).orElse(null);
	}

	/** The traits that work on {@code player}'s spell made of {@code runes} (none while the server has traits off). */
	public static List<String> traitsOf(Player player, List<RuneDef> runes) {
		if (!traitsOn(player)) {
			return List.of();
		}
		MasteryBook.Entry entry = entry(player, runes);
		return entry == null ? List.of() : entry.active();
	}

	private static boolean traitsOn(Player player) {
		return Config.masteryTraits(player);
	}

	// ------------------------------------------------------------------ factors at the gate (both sides, so the readout matches)

	/** The factor on the price of {@code player}'s spell made of {@code runes} from its traits (1 with none). */
	public static double costFactor(Player player, List<RuneDef> runes) {
		double factor = 1.0;
		for (MasteryTraits.Trait t : MasteryTraits.withHook(traitsOf(player, runes), Hook.COST)) {
			if (t.param().isEmpty() || inPlace(player, t.param())) {
				factor *= t.amount();
			}
		}
		return Math.max(MasteryRules.COST_FLOOR, factor);
	}

	/** The factor on its cooldown from its traits. */
	public static double cooldownFactor(Player player, List<RuneDef> runes) {
		return MasteryTraits.product(traitsOf(player, runes), Hook.COOLDOWN, MasteryRules.COOLDOWN_FLOOR, 1.0);
	}

	/** The factor on the power of a spell that only helps (Warm Hands), 1 for any other. */
	public static double powerFactor(Player player, List<RuneDef> runes) {
		MasteryTraits.Profile profile = MasteryTraits.Profile.of(runes);
		if (!profile.helpful() || profile.harmful()) {
			return 1.0;
		}
		return MasteryTraits.product(traitsOf(player, runes), Hook.HEALING, 1.0, 1.25);
	}

	/**
	 * How much faster {@code caster} charges the spell they're charging (Ready Breath). The server reads their record;
	 * a client reads the public look of the spell they have ready, so everyone's circle opens with theirs.
	 */
	public static double chargeSpeed(Entity caster) {
		if (!(caster instanceof Player player)) {
			return 1.0;
		}
		dev.wildercord.player.WildercordAttachments.Charge charge = player.getAttached(dev.wildercord.player.WildercordAttachments.CHARGE);
		if (charge == null) {
			return 1.0;
		}
		List<RuneDef> runes = new ArrayList<>();
		for (String id : charge.runes()) {
			dev.wildercord.spell.Runes.get(id).ifPresent(runes::add);
		}
		if (player instanceof ServerPlayer) {
			return MasteryTraits.product(traitsOf(player, runes), Hook.CHARGE, 1.0, MasteryRules.MAX_CHARGE_SPEED);
		}
		MasteryAttachments.Look look = MasteryAttachments.lookOf(player, runes);
		return look.has(MasteryAttachments.Look.QUICK) ? MasteryTraits.READY_BREATH.amount() : 1.0;
	}

	/** How much farther this cast's bolts, beams and aimed shapes reach (Far Reach). */
	public static double range(Cast cast) {
		Tally tally = cast.mastery();
		if (tally == null) {
			return 1.0;
		}
		return MasteryTraits.product(tally.traits, Hook.RANGE, 1.0, MasteryRules.MAX_RANGE);
	}

	/** Whether this cast's fire burns at full strength on the wet (Undying Flame). */
	public static boolean wetFire(Cast cast) {
		Tally tally = cast.mastery();
		return tally != null && tally.has(Hook.WET_FIRE);
	}

	private static boolean inPlace(Player player, String place) {
		Level level = player.level();
		BlockPos pos = player.blockPosition();
		return switch (place) {
			case "nether" -> level.dimension() == Level.NETHER;
			case "end" -> level.dimension() == Level.END;
			case "water" -> player.isInWater() || level.isRainingAt(pos.above());
			case "underground" -> underground(level, pos);
			default -> false;
		};
	}

	private static boolean underground(Level level, BlockPos pos) {
		return level.dimensionType().hasSkyLight() && !level.canSeeSky(pos.above()) && pos.getY() < level.getSeaLevel();
	}

	// ------------------------------------------------------------------ a cast begins

	/** The last spell title sent, for the game tests: who cast it, its name and everyone it was meant for. */
	public record Title(UUID caster, String name, int rank, List<UUID> viewers) {}

	private static volatile Title lastTitle;

	public static Title lastTitle() {
		return lastTitle;
	}

	/** How far a spoken spell's name carries. */
	public static final double TITLE_RANGE = 32.0;

	/**
	 * A player cast {@code runes} (spell slot {@code spell}) as {@code cast}, having paid {@code spent} mana: gives the cast
	 * its tally (so it learns, and its traits work), shows its look, rings its bell and speaks its name.
	 */
	public static void onCast(ServerPlayer player, int spell, List<RuneDef> runes, Cast cast, int spent) {
		if (runes.isEmpty()) {
			return;
		}
		var settings = Config.get().mastery();
		String key = keyOf(runes);
		MasteryBook.Entry entry = MasteryAttachments.book(player).entry(key).orElse(null);
		List<String> traits = settings.enabled() && settings.traits() && entry != null ? entry.active() : List.of();
		ServerLevel level = (ServerLevel) player.level();
		int foes = foes(player);
		boolean boss = bossNear(player);
		double health = player.getHealth() / Math.max(1F, player.getMaxHealth());
		Set<String> circumstances = circumstances(player, runes, foes, boss);
		double situation = MasteryRules.situation(health, foes, boss, circumstances.contains("dungeon"));
		boolean practice = level.dimension() == PracticeRoom.DIMENSION;
		// A creative player's spells don't grow: nothing they do is earned.
		Tally tally = new Tally(player.getUUID(), key, runes, traits, settings.enabled() && !player.isSpectator() && !player.isCreative(), situation,
			MasteryRules.danger(health, foes), foes, circumstances, place(player.blockPosition()));
		if (practice) {
			tally.circumstances.add("practice");
		}
		cast.mastery(tally);
		long seed = entry != null ? entry.seed() : MasterySigil.seed(player.getUUID(), key);
		int rank = entry != null ? entry.rank() : MasteryRules.FIRST;
		show(player, key, rank, seed, traits);
		if (tally.has(Hook.SECOND_WIND) && health < MasteryRules.LOW_HEALTH && spent > 0) {
			// Cast in danger: a share of its mana comes back.
			dev.wildercord.player.Mana.restore(player, (float) Math.floor(spent * MasteryTraits.SECOND_WIND.amount()));
		}
		cosmetics(level, player, traits);
		if (settings.spokenNames() && rank >= MasteryRules.ADEPT) {
			Spellbook book = Spellbooks.get(player);
			String name = spell >= 0 ? book.name(spell) : "";
			if (!name.isEmpty()) {
				speak(player, name, rank, runes);
			}
		}
	}

	/** Gives a scroll's cast the traits inscribed on it (it learns nothing), and the reader the scroll's look while it goes off. */
	public static void onScrollCast(ServerPlayer player, List<RuneDef> runes, Cast cast, List<String> traits, int rank, long seed) {
		if (!Config.get().mastery().traits()) {
			traits = List.of();
		}
		Tally tally = new Tally(player.getUUID(), keyOf(runes), runes, traits, false, 1.0, false, 0, Set.of(), 0);
		cast.mastery(tally);
		show(player, tally.key, rank, seed, traits);
		cosmetics((ServerLevel) player.level(), player, traits);
	}

	/** Sets the public look of the spell keyed {@code key}: everyone nearby draws its rank and sigil on its circle. */
	static void show(ServerPlayer player, String key, int rank, long seed, Collection<String> traits) {
		int flags = 0;
		for (String id : traits) {
			MasteryTraits.Trait t = MasteryTraits.get(id).orElse(null);
			if (t == null) {
				continue;
			}
			if (t.hook() == Hook.CHARGE) {
				flags |= MasteryAttachments.Look.QUICK;
			}
			if (t.hook() == Hook.COSMETIC) {
				flags |= switch (t.param()) {
					case "stars" -> MasteryAttachments.Look.STARS;
					case "hue" -> MasteryAttachments.Look.HUE;
					case "embers" -> MasteryAttachments.Look.EMBERS;
					case "frost" -> MasteryAttachments.Look.FROST;
					case "petals" -> MasteryAttachments.Look.PETALS;
					default -> 0;
				};
			}
		}
		MasteryAttachments.Look look = new MasteryAttachments.Look(MasteryRules.hash(key), Math.max(1, rank), seed, flags);
		if (!look.equals(player.getAttached(MasteryAttachments.LOOK))) {
			player.setAttached(MasteryAttachments.LOOK, look);
		}
		SHOWN_AT.put(player.getUUID(), player.level().getGameTime());
	}

	/** When each player's look was last set by a cast, so the once-a-second refresh leaves a fresh one alone a moment. */
	private static final Map<UUID, Long> SHOWN_AT = new HashMap<>();

	/** The look of {@code player}'s selected spell, kept up to date (a rank reached, an edit, a loadout loaded, a new Cord). */
	static void refreshLook(ServerPlayer player) {
		Long shown = SHOWN_AT.get(player.getUUID());
		if (shown != null && player.level().getGameTime() - shown < 40) {
			return;
		}
		var tier = Spellbooks.tier(player);
		Spellbook book = Spellbooks.get(player);
		List<RuneDef> runes = tier == null ? List.of() : SpellCaster.activeRunes(book, book.selected(), tier);
		if (runes.isEmpty() || !Config.get().mastery().enabled()) {
			if (player.hasAttached(MasteryAttachments.LOOK)) {
				player.removeAttached(MasteryAttachments.LOOK);
			}
			return;
		}
		String key = keyOf(runes);
		MasteryBook.Entry entry = MasteryAttachments.book(player).entry(key).orElse(null);
		show(player, key, entry == null ? MasteryRules.FIRST : entry.rank(), entry == null ? MasterySigil.seed(player.getUUID(), key) : entry.seed(),
			entry == null || !Config.get().mastery().traits() ? List.of() : entry.active());
		SHOWN_AT.remove(player.getUUID());
	}

	/** A bell, or motes from the circle: the looks and sounds a spell's traits give it, for everyone to see. */
	private static void cosmetics(ServerLevel level, ServerPlayer player, Collection<String> traits) {
		Vec3 at = player.position().add(0, 1.0, 0);
		for (MasteryTraits.Trait t : MasteryTraits.withHook(traits, Hook.COSMETIC)) {
			switch (t.param()) {
				case "bell" -> Fx.sound(level, player.position(), SoundEvents.BELL_BLOCK, 0.45F, 1.6F);
				case "stars" -> Motes.glows(level, at, 8, 0.7, 0xFFF2C8, 0.12, 30, new Vec3(0, 0.03, 0), 0.02);
				case "embers" -> Motes.glows(level, at, 8, 0.6, 0xFF8A3A, 0.1, 24, new Vec3(0, 0.05, 0), 0.03);
				case "frost" -> Motes.glows(level, at.add(0, 1.0, 0), 8, 0.8, 0xCFEFFF, 0.1, 30, new Vec3(0, -0.03, 0), 0.02);
				case "petals" -> {
					for (int i = 0; i < 3; i++) {
						Motes.butterfly(level, at.add((i - 1) * 0.4, 0.6, 0), 0xFFB8D8, 0.18, 40, new Vec3(0, -0.01, 0));
					}
				}
				default -> {}
			}
		}
	}

	/** Shows a named spell's name to everyone within {@link #TITLE_RANGE} blocks (the caster too), as a brief title by the caster. */
	private static void speak(ServerPlayer player, String name, int rank, List<RuneDef> runes) {
		int color = 0xE8C46A;
		for (RuneDef rune : Knots.flatten(runes)) {
			if (rune.family() == dev.wildercord.spell.RuneFamily.EFFECT) {
				color = dev.wildercord.spell.RuneColors.of(rune);
				break;
			}
		}
		MasteryChoices.Title payload = new MasteryChoices.Title(player.getId(), name, rank, color);
		List<UUID> viewers = new ArrayList<>();
		for (ServerPlayer other : ((ServerLevel) player.level()).players()) {
			if (other.distanceToSqr(player) > TITLE_RANGE * TITLE_RANGE) {
				continue;
			}
			viewers.add(other.getUUID());
			if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(other, MasteryChoices.Title.TYPE)) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(other, payload);
			}
		}
		lastTitle = new Title(player.getUUID(), name, rank, List.copyOf(viewers));
	}

	// ------------------------------------------------------------------ the moment of a cast

	/** Foes around a player: monsters, and anything hunting them. */
	private static int foes(ServerPlayer player) {
		double r = MasteryRules.CROWD_RADIUS;
		return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r),
			m -> m.isAlive() && (m instanceof Enemy || m.getTarget() == player)).size();
	}

	private static boolean bossNear(ServerPlayer player) {
		return !player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MasteryRules.BOSS_RADIUS),
			e -> e.isAlive() && Spirits.isBoss(e)).isEmpty();
	}

	/** Every circumstance {@code player} is casting in right now (the kinds of foe it strikes are added as it strikes them). */
	static Set<String> circumstances(ServerPlayer player, List<RuneDef> runes, int foes, boolean boss) {
		Set<String> out = new LinkedHashSet<>();
		ServerLevel level = (ServerLevel) player.level();
		BlockPos pos = player.blockPosition();
		boolean sky = level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime();
		if (level.isRainingAt(pos.above()) || level.isRainingAt(pos)) {
			out.add("rain");
			if (level.isThundering()) {
				out.add("thunder");
			}
		}
		if (sky) {
			long day = Math.floorMod(level.getOverworldClockTime(), 24000);
			boolean night = day >= 13000 && day < 23000;
			if (night) {
				out.add("night");
			} else if (level.canSeeSky(pos.above())) {
				out.add("day");
			}
		}
		if (underground(level, pos)) {
			out.add("underground");
		}
		if (level.dimension() == Level.OVERWORLD && pos.getY() < 0) {
			out.add("deep");
		}
		if (player.isInWater()) {
			out.add("water");
		}
		if (level.dimension() == Level.NETHER) {
			out.add("nether");
			out.add("hot");
		} else if (level.dimension() == Level.END) {
			out.add("end");
		} else {
			float temperature = level.getBiome(pos).value().getBaseTemperature();
			if (temperature < 0.15F) {
				out.add("cold");
			} else if (temperature >= 1.5F) {
				out.add("hot");
			}
		}
		if (player.getHealth() / Math.max(1F, player.getMaxHealth()) < MasteryRules.LOW_HEALTH) {
			out.add("low_health");
		}
		for (ServerPlayer other : level.players()) {
			if (other != player && other.isAlive() && !other.isSpectator() && other.distanceToSqr(player) < 12 * 12 && !Targets.canHarm(player, other)) {
				out.add("allies");
				break;
			}
		}
		if (foes >= MasteryRules.CROWD) {
			out.add("crowd");
		}
		if (boss) {
			out.add("boss");
		}
		if (inDungeon(level, pos)) {
			out.add("dungeon");
		}
		if (!player.onGround() && !player.isInWater() && !player.getAbilities().flying) {
			out.add("airborne");
		}
		for (Map.Entry<String, SpellMasteryApi.Circumstance> extra : SpellMasteryApi.circumstances().entrySet()) {
			try {
				if (extra.getValue().holds(player, List.copyOf(runes))) {
					out.add(extra.getKey());
				}
			} catch (RuntimeException e) {
				dev.wildercord.Wildercord.LOGGER.warn("A mastery circumstance ({}) threw; skipping it", extra.getKey(), e);
			}
		}
		return out;
	}

	private static final Set<net.minecraft.resources.ResourceKey<Structure>> DUNGEONS = Set.of(BuiltinStructures.ANCIENT_CITY,
		BuiltinStructures.TRIAL_CHAMBERS, BuiltinStructures.STRONGHOLD, BuiltinStructures.FORTRESS, BuiltinStructures.BASTION_REMNANT,
		BuiltinStructures.END_CITY, BuiltinStructures.WOODLAND_MANSION, BuiltinStructures.OCEAN_MONUMENT);

	/** Inside a dungeon: the mod's Archives and dungeons (their warded rooms too) and the great structures of the world. */
	public static boolean inDungeon(ServerLevel level, BlockPos pos) {
		if (dev.wildercord.world.dungeons.DungeonWards.warded(level, pos)) {
			return true;
		}
		return level.structureManager().getStructureWithPieceAt(pos, (Holder<Structure> holder) -> holder.is(dev.wildercord.world.WildercordWorldgen.ARCHIVES)
			|| holder.is(dev.wildercord.world.dungeons.DungeonWorldgen.DUNGEONS) || holder.unwrapKey().map(DUNGEONS::contains).orElse(false)).isValid();
	}

	/** A place, for repetition: the 16-block cube a position is in. */
	static long place(BlockPos pos) {
		int s = Integer.numberOfTrailingZeros(MasteryRules.PLACE);
		return BlockPos.asLong(pos.getX() >> s, pos.getY() >> s, pos.getZ() >> s);
	}

	// ------------------------------------------------------------------ what a cast does

	/** The kind of target {@code target} is to {@code caster}, for its worth: 0 for anything that isn't a foe. */
	static double worth(LivingEntity caster, LivingEntity target) {
		if (target instanceof Player) {
			return Targets.canHarm(caster, target) ? MasteryRules.PLAYER : 0;
		}
		if (Spirits.isBoss(target)) {
			return MasteryRules.BOSS;
		}
		boolean foe = target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == caster
			|| target instanceof NeutralMob neutral && neutral.getTarget() == caster;
		if (!foe || Targets.playerPet(target)) {
			return 0;
		}
		return target.hasAttached(dev.wildercord.player.WildercordAttachments.RUNEBOUND) ? MasteryRules.RUNEBOUND : 1.0;
	}

	/** Whether a hit is practice: a training dummy, or anything in the practice arena. */
	static boolean practice(Cast cast, LivingEntity target) {
		return target instanceof TrainingDummy || cast.level.dimension() == PracticeRoom.DIMENSION;
	}

	/**
	 * The damage bonus this cast's traits give a hit on {@code target}: the conditions each asks for checked, the product
	 * held to {@link MasteryRules#MAX_TRAIT_POWER} and halved against a player. Multiplied into the hit's bonuses in
	 * {@code Effects.hurt}, under the spell-defence cap.
	 */
	public static double damageBonus(Cast cast, LivingEntity target) {
		Tally tally = cast.mastery();
		if (tally == null || tally.traits.isEmpty()) {
			return 1.0;
		}
		double product = 1.0;
		for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.DAMAGE)) {
			if (holds(t.param(), cast, tally, target)) {
				product *= t.amount();
			}
		}
		return product == 1.0 ? 1.0 : MasteryRules.traitPower(product, target instanceof Player);
	}

	private static boolean holds(String condition, Cast cast, Tally tally, LivingEntity target) {
		return switch (condition) {
			case "" -> true;
			case "low_target" -> target.getHealth() < target.getMaxHealth() / 3F;
			case "full_target" -> target.getHealth() >= target.getMaxHealth() - 0.01F;
			case "undead" -> target.is(EntityTypeTags.UNDEAD);
			case "arthropod" -> target.is(EntityTypeTags.ARTHROPOD);
			case "boss" -> Spirits.isBoss(target);
			case "nether" -> cast.level.dimension() == Level.NETHER;
			case "end" -> cast.level.dimension() == Level.END;
			case "crowd" -> tally.foes >= MasteryRules.CROWD;
			case "low_health" -> cast.caster.getHealth() < cast.caster.getMaxHealth() / 3F;
			case "night", "day", "underground", "rain" -> tally.circumstances.contains(condition)
				|| condition.equals("rain") && tally.circumstances.contains("thunder");
			// Any other circumstance it was cast in, including those added through the API (a world's quirk holding).
			default -> tally.circumstances.contains(condition);
		};
	}

	/**
	 * A hit of this cast struck {@code target} for {@code dealt}, of which {@code taken} came off its health: experience for
	 * a real foe (or practice), and the traits that answer a strike (a drink of it, mana from a kill, a mark, a leap).
	 */
	static void afterDamage(Cast cast, LivingEntity target, float dealt, float taken) {
		if (cast.guardedImpact() && (!cast.alive() || target.isAlive() && !cast.admits(target))) return;
		Tally tally = cast.mastery();
		if (tally == null || !(cast.caster instanceof ServerPlayer player) || target == player) {
			return;
		}
		boolean killed = !target.isAlive() || target.isDeadOrDying();
		boolean practice = practice(cast, target);
		double worth = practice ? 1.0 : worth(player, target);
		if (worth > 0 && tally.learns) {
			boolean first = tally.struck.add(target.getUUID());
			double share = Math.max(taken, practice ? dealt : 0) / Math.max(1F, target.getMaxHealth());
			// The first strike on a foe in a cast is worth a strike; later ones (a Zone's pulses) only what they take and the kill.
			double xp = first ? MasteryRules.strike(worth, share, killed) : worth * (MasteryRules.DAMAGE * Math.min(1, share) + (killed ? MasteryRules.KILL : 0));
			String kind = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			if (!practice) {
				if (target.is(EntityTypeTags.UNDEAD)) {
					circumstance(tally, "undead");
				}
				if (target.is(EntityTypeTags.ARTHROPOD)) {
					circumstance(tally, "arthropod");
				}
				if (Spirits.isBoss(target)) {
					circumstance(tally, "boss");
				}
			}
			earn(player, tally, xp, kind, practice, Spirits.isBoss(target));
		}
		if (tally.traits.isEmpty() || cast.guardedImpact() && (!cast.alive() || target.isAlive() && !cast.admits(target))) {
			return;
		}
		// What a strike sets off: only for a foe that can be harmed (not an ally caught by a side effect).
		if (!Targets.canHarm(player, target) && !(target instanceof TrainingDummy)) {
			return;
		}
		for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.LEECH)) {
			double room = 4.0 - tally.drunk;
			double drink = Math.min(room, taken * t.amount());
			if (drink > 0 && player.isAlive()) {
				tally.drunk += drink;
				player.heal((float) drink);
			}
		}
		if (killed) {
			for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.MANA_ON_KILL)) {
				if (tally.harvested < 3) {
					tally.harvested++;
					dev.wildercord.player.Mana.restore(player, (float) t.amount());
				}
			}
		}
		for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.ON_STRIKE)) {
			strikeEffect(cast, target, t);
		}
		if (cast.guardedImpact() && (!cast.alive() || target.isAlive() && !cast.admits(target))) return;
		if (!tally.chaining && !killed) {
			for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.CHAIN)) {
				if (cast.level.getRandom().nextDouble() < t.amount()) {
					leap(cast, tally, target, dealt);
					break;
				}
			}
		}
		residue(cast, tally, target.getBoundingBox().getCenter());
	}

	/** How strong a leaping hit is, as a share of the hit it leapt from, and how far it may leap. */
	public static final double LEAP_SHARE = 0.4;
	public static final double LEAP_RANGE = 6.0;

	private static void leap(Cast cast, Tally tally, LivingEntity from, float dealt) {
		LivingEntity next = null;
		double best = LEAP_RANGE * LEAP_RANGE;
		Vec3 origin = from.getBoundingBox().getCenter();
		var candidates = cast.guardedImpact() ? RelayCircles.collateral(cast, origin, from.getBoundingBox().inflate(LEAP_RANGE), e -> e != from)
			: cast.level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(LEAP_RANGE),
				e -> e != from && e.isAlive() && Targets.canHarm(cast.caster, e));
		for (LivingEntity e : candidates) {
			double d = e.distanceToSqr(from);
			if (d < best) {
				best = d;
				next = e;
			}
		}
		if (next == null || cast.guardedImpact() && (!RelayCircles.admitsFrom(cast, origin, next) || cast.takeEntities(1) < 1)) {
			return;
		}
		LivingEntity target = next;
		Light.ray(cast.level, from.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), 0xBFE8FF, 0.05, 6);
		tally.chaining = true;
		try {
			RelayCircles.from(cast, origin, () -> Effects.hurt(cast, target, cast.level.damageSources().indirectMagic(cast.caster, cast.caster), dealt * LEAP_SHARE));
		} finally {
			tally.chaining = false;
		}
	}

	private static void strikeEffect(Cast cast, LivingEntity target, MasteryTraits.Trait t) {
		if (!cast.admits(target)) return;
		switch (t.param()) {
			case "slow" -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) Math.round(20 * t.amount()), 0, false, true), cast.caster);
			case "glow" -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false), cast.caster);
			case "ignite" -> Effects.feedOrIgnite(target, 40);
			case "push" -> {
				Vec3 away = target.position().subtract(cast.guardedImpact() ? cast.incoming() : cast.caster.position());
				Vec3 flat = new Vec3(away.x, 0, away.z);
				if (flat.lengthSqr() > 1.0E-4) {
					Effects.push(target, flat.normalize().scale(0.35).add(0, 0.1, 0));
				}
			}
			case "soak" -> Reactions.mark(target, Reactions.Mark.SOAKED);
			case "shadow" -> Reactions.mark(target, Reactions.Mark.SHADOWED);
			case "bleed" -> Reactions.mark(target, Reactions.Mark.BLEEDING);
			default -> {}
		}
	}

	/** A Lingering Mark trait: the cast's magic left where it first lands, through the residue hook. */
	private static void residue(Cast cast, Tally tally, Vec3 at) {
		if (tally.residue || cast.guardedImpact() && !cast.alive() || !(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.RESIDUE)) {
			tally.residue = true;
			String element = "";
			MasteryTraits.Profile profile = MasteryTraits.Profile.of(tally.runes);
			if (!profile.elements().isEmpty()) {
				element = profile.elements().iterator().next();
			}
			SpellMasteryApi.leaveResidue(cast.level, at, player, tally.runes, element, t);
		}
	}

	/** Health a spell being applied restored to {@code target} (from {@code mixin.LivingEntityHealMixin}). */
	public static void healed(Cast cast, LivingEntity target, float restored) {
		if (cast == null || restored <= 0.01F) {
			return;
		}
		Tally tally = cast.mastery();
		if (tally == null || !tally.learns || !(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		if (target == player) {
			if (tally.danger) {
				earn(player, tally, MasteryRules.heal(restored) * 0.5, "", false, false);
			}
			return;
		}
		if (Targets.canHelp(player, target)) {
			earn(player, tally, MasteryRules.heal(restored), "", false, false);
		}
	}

	/**
	 * A group of this cast landed on {@code hit}: experience for allies it helped who needed it (and for helping yourself in
	 * danger), a little for a spell that moves you or works the world, and the traits that answer helping.
	 */
	static void afterHit(Cast cast, SpellPlan.Group g, Cast.Hit hit) {
		Tally tally = cast.mastery();
		if (tally == null || !(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		boolean helps = false;
		boolean moves = false;
		boolean works = false;
		for (SpellPlan.EffectNode node : g.effects) {
			EffectKind kind = node.effect.kind();
			helps |= kind == EffectKind.HELPFUL;
			moves |= kind == EffectKind.MOVEMENT;
			works |= kind == EffectKind.WORLD;
		}
		if (helps) {
			for (Entity e : hit.entities()) {
				if (!(e instanceof LivingEntity living) || !Targets.canHelp(player, living)) {
					continue;
				}
				help(cast, tally, player, living);
			}
			if (hit.self() && !hit.entities().contains(player)) {
				help(cast, tally, player, player);
			}
		}
		if ((moves && hit.self() || works && hit.block() != null) && !tally.utility) {
			tally.utility = true;
			if (tally.learns) {
				earn(player, tally, MasteryRules.UTILITY * (tally.danger ? 2 : 1), "", cast.level.dimension() == PracticeRoom.DIMENSION, false);
			}
			if (moves) {
				for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.FEATHER)) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, (int) t.amount(), 0, false, false));
				}
			}
		}
		if (hit.point() != null && (helps || works)) {
			residue(cast, tally, hit.point());
		}
	}

	private static void help(Cast cast, Tally tally, ServerPlayer player, LivingEntity living) {
		if (living == player) {
			if (!tally.selfHelped) {
				tally.selfHelped = true;
				if (tally.learns && tally.danger) {
					earn(player, tally, MasteryRules.SELF_HELP, "", false, false);
				}
				allyTraits(tally, player, living, false);
			}
			return;
		}
		if (!tally.helped.add(living.getUUID())) {
			return;
		}
		boolean needs = living.getHealth() < living.getMaxHealth() - 0.5F || living.getLastHurtByMob() != null
			&& living.tickCount - living.getLastHurtByMobTimestamp() < 200 || living instanceof Mob mob && mob.getTarget() != null;
		if (tally.learns && needs) {
			earn(player, tally, MasteryRules.HELP, "", false, false);
		}
		allyTraits(tally, player, living, true);
	}

	private static void allyTraits(Tally tally, ServerPlayer player, LivingEntity living, boolean ally) {
		for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.ALLY_EFFECT)) {
			int ticks = (int) t.amount();
			switch (t.param()) {
				case "regeneration" -> living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true), player);
				case "speed" -> living.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, true), player);
				case "resistance" -> living.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0, false, true), player);
				case "night_vision" -> living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0, false, false), player);
				case "cleanse" -> {
					living.removeEffect(MobEffects.POISON);
					living.removeEffect(MobEffects.WITHER);
				}
				default -> {}
			}
		}
		if (ally) {
			for (MasteryTraits.Trait t : MasteryTraits.withHook(tally.traits, Hook.ALLY_MANA)) {
				if (tally.thanked < 2) {
					tally.thanked++;
					dev.wildercord.player.Mana.restore(player, (float) t.amount());
				}
			}
		}
	}

	private static void circumstance(Tally tally, String circumstance) {
		if (tally.circumstances.add(circumstance) && tally.counted && tally.countedIn.add(circumstance)) {
			pending(tally.owner, tally.key, tally.runes).counts.merge(circumstance, 1, Integer::sum);
		}
	}

	// ------------------------------------------------------------------ earning

	/** What a player's spells have earned since the last write, by spell. */
	static final class Pending {
		final List<RuneDef> runes;
		double xp;
		double practice;
		int casts;
		final Map<String, Integer> counts = new HashMap<>();

		Pending(List<RuneDef> runes) {
			this.runes = runes;
		}
	}

	private static final Map<UUID, Map<String, Pending>> PENDING = new HashMap<>();

	private static Pending pending(UUID owner, String key, List<RuneDef> runes) {
		return PENDING.computeIfAbsent(owner, k -> new HashMap<>()).computeIfAbsent(key, k -> new Pending(runes));
	}

	/** A spell's memory of where it was cast lately, and at what: for repetition and variety. */
	private static final class Memory {
		final Map<Long, double[]> places = new HashMap<>();
		final ArrayDeque<String> kinds = new ArrayDeque<>();
	}

	private static final Map<UUID, Map<String, Memory>> MEMORY = new HashMap<>();

	/**
	 * Adds {@code xp} (before the moment, variety, repetition and the server's rate) to what the cast has earned, held to
	 * {@link MasteryRules#MAX_PER_CAST} for the whole cast; practice goes to its own, capped, pool.
	 *
	 * @param kind the kind of foe struck ("" for a heal, a ward or a utility spell), for variety and repetition
	 */
	private static void earn(ServerPlayer player, Tally tally, double xp, String kind, boolean practice, boolean boss) {
		if (xp <= 0 || !tally.learns) {
			return;
		}
		double rate = Config.get().mastery().xpMultiplier();
		if (rate <= 0) {
			return;
		}
		Pending pending = pending(tally.owner, tally.key, tally.runes);
		if (practice) {
			pending.practice += xp * rate;
			return;
		}
		Memory memory = MEMORY.computeIfAbsent(tally.owner, k -> new HashMap<>()).computeIfAbsent(tally.key, k -> new Memory());
		double factor = tally.situation;
		if (!kind.isEmpty()) {
			if (!memory.kinds.contains(kind)) {
				factor *= MasteryRules.NOVELTY;
			}
			if (tally.kinds.add(kind)) {
				memory.kinds.remove(kind);
				memory.kinds.addLast(kind);
				while (memory.kinds.size() > MasteryRules.REMEMBERED_KINDS) {
					memory.kinds.removeFirst();
				}
			}
		}
		if (!boss) {
			// The same thing in the same place: each cast like it lately is worth a little less (a boss fight isn't a farm).
			long now = player.level().getGameTime();
			long where = tally.place * 31 + kind.hashCode();
			double[] seen = memory.places.computeIfAbsent(where, k -> new double[] {0, now});
			double recent = MasteryRules.forget(seen[0], now - (long) seen[1]);
			factor *= MasteryRules.repetition(recent);
			if (!tally.kinds.contains("@" + where)) {
				tally.kinds.add("@" + where);
				seen[0] = recent + 1;
				seen[1] = now;
			}
			if (memory.places.size() > 64) {
				memory.places.entrySet().removeIf(e -> MasteryRules.forget(e.getValue()[0], now - (long) e.getValue()[1]) < 0.05);
			}
		}
		double room = MasteryRules.MAX_PER_CAST - tally.earned;
		double gained = Math.min(room, xp * factor * rate);
		if (gained <= 0) {
			return;
		}
		tally.earned += gained;
		pending.xp += gained;
		if (!tally.counted) {
			tally.counted = true;
			pending.casts++;
			for (String c : tally.circumstances) {
				if (!c.equals("practice")) {
					tally.countedIn.add(c);
					pending.counts.merge(c, 1, Integer::sum);
				}
			}
		}
	}

	/** Grants experience straight to a spell (commands, the game tests): written at once, ranks and offers included. */
	public static void grant(ServerPlayer player, List<RuneDef> runes, double xp) {
		pending(player.getUUID(), keyOf(runes), runes).xp += xp;
		flush(player);
	}

	/** Writes everything {@code player}'s spells have earned into their book: ranks reached, offers made, toasts shown. */
	public static void flush(ServerPlayer player) {
		Map<String, Pending> waiting = PENDING.remove(player.getUUID());
		if (waiting == null || waiting.isEmpty()) {
			return;
		}
		MasteryBook book = MasteryAttachments.book(player);
		long now = player.level().getGameTime();
		Set<String> keep = threaded(player);
		for (Map.Entry<String, Pending> e : waiting.entrySet()) {
			Pending p = e.getValue();
			MasteryBook.Entry before = book.entry(e.getKey()).orElse(null);
			MasteryBook.Entry entry = before != null ? before : MasteryBook.Entry.fresh(e.getKey(), MasterySigil.seed(player.getUUID(), e.getKey()), now);
			double practice = entry.practice() + MasteryRules.practice(entry.practice(), p.practice);
			entry = entry.withXp(entry.xp() + p.xp, practice).usedAt(now);
			if (p.casts > 0 || !p.counts.isEmpty()) {
				entry = entry.counted(p.counts, p.casts);
			}
			int oldRank = before == null ? MasteryRules.FIRST : before.rank();
			entry = MasteryChoices.offerIfDue(player, entry, p.runes);
			book = book.with(entry, keep);
			if (entry.rank() > oldRank) {
				MasteryChoices.rose(player, entry, p.runes, oldRank);
			}
		}
		player.setAttached(MasteryAttachments.MASTERY, book);
	}

	/** The keys of every spell threaded on {@code player}'s Cord (never forgotten). */
	static Set<String> threaded(ServerPlayer player) {
		Set<String> keys = new HashSet<>();
		var tier = Spellbooks.tier(player);
		if (tier == null) {
			return keys;
		}
		Spellbook book = Spellbooks.get(player);
		for (int i = 0; i < book.spells().size(); i++) {
			List<RuneDef> runes = SpellCaster.activeRunes(book, i, tier);
			if (!runes.isEmpty()) {
				keys.add(keyOf(runes));
			}
		}
		return keys;
	}

	// ------------------------------------------------------------------ lifecycle

	public static void init() {
		MasteryAttachments.init();
		MasteryChoices.init();
		Inscriptions.init();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 7) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				flush(player);
				refreshLook(player);
			}
		});
		// What was earned is written before a player is saved: leaving, dying, the server stopping.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			flush(handler.player);
			SHOWN_AT.remove(handler.player.getUUID());
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				flush(player);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(Mastery::flushAll);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PENDING.clear();
			MEMORY.clear();
			SHOWN_AT.clear();
			lastTitle = null;
		});
	}

	private static void flushAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			flush(player);
		}
	}
}
