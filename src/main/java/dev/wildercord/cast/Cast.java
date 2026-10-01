package dev.wildercord.cast;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One cast in flight. Children (link continuations, echoes) share the same budgets, so a
 * single press can never touch more than {@link #MAX_ENTITIES} creatures or {@link #MAX_BLOCKS}
 * blocks, however it is chained. Shapes that strike again and again (Domain, Zone, Totem,
 * Orbit...) get a fresh budget for each {@link #pulse()}, so a big one keeps working to the
 * end; the Siphon cap and once-per-cast costs still count for the whole cast.
 */
public final class Cast {
	/** The default caps; a server sets its own in {@code config/wildercord.json} (casting.max_creatures_per_cast, max_blocks_per_cast). */
	public static final int MAX_ENTITIES = 64;
	public static final int MAX_BLOCKS = 32;
	public static final int MAX_DEPTH = 8;
	/**
	 * Segments a whole cast may run, links, pulses and echoes included. Repeating shapes give each
	 * strike a fresh creature budget, so without this a chain of linked repeating shapes would grow
	 * exponentially (six pulses × eight hits × ... per level). A Zone with On Hit Burst uses 49.
	 */
	public static final int MAX_SEGMENTS = 128;

	/**
	 * Limits for one payment: every copy of a spell that was paid for once (a storm's echo, Twin Star,
	 * a Focus of Echoes, a wild surge's second go) shares them, so a copy can't Siphon past the cap
	 * again or do a once-per-cast thing (Imbue) a second time.
	 */
	private static final class Paid {
		int siphon = dev.wildercord.player.Mana.SIPHON_CAP_PER_CAST;
		int siphonLevel = -1;
		/** Whose Cord {@link #siphonLevel} was read from: a parried spell changes hands but keeps its payment. */
		LivingEntity siphoner;
		final java.util.Set<String> once = new java.util.HashSet<>();
	}

	/** Limits for the whole cast, pulses included. */
	private static final class Shared {
		final Paid paid;
		/** Set when the whole cast is cut short, e.g. a Domain shattered in a clash. */
		boolean cancelled;
		int segments = MAX_SEGMENTS;
		/** How charged the spell was when released, 0 to 1 (for its feel). */
		double charge;
		/** The feel of each group of the plan, worked out once. */
		final java.util.Map<dev.wildercord.spell.SpellPlan.Group, dev.wildercord.cast.feel.Feel> feels = new java.util.IdentityHashMap<>();
		final java.util.Map<dev.wildercord.spell.SpellPlan.Group, Double> circleConditions = new java.util.IdentityHashMap<>();
		/** The mana the spell asks, as a Shield weighs it; worked out from the plan when nobody set it. */
		double weight = -1;
		/** The casting gear in the caster's hands when it was cast (staffs and foci). */
		dev.wildercord.gear.GearBonuses gear = dev.wildercord.gear.GearBonuses.NONE;
		/** A stored (imbued) spell's release: where it was set off (see {@link #origin()}); null for a spell cast from a Cord. */
		Trigger origin;
		/** Whether the caster's affinities count on this spell (see {@link #withAffinity}). */
		boolean affinity;
		/** What how it was cast added to its power (overchannel, the beat, a traced glyph), already in {@link #power}. */
		double performance = 1.0;

		Shared() {
			this(new Paid());
		}

		Shared(Paid paid) {
			this.paid = paid;
		}

		/** A fresh cast's limits keeping this one's weight, gear and origin, and its payment ({@code samePayment}) or a new one. */
		Shared copy(boolean samePayment) {
			Shared copy = new Shared(samePayment ? paid : new Paid());
			copy.weight = weight;
			copy.charge = charge;
			copy.gear = gear;
			copy.origin = origin;
			copy.affinity = affinity;
			copy.performance = performance;
			return copy;
		}
	}

	/** What a spell with no plan to price weighs (a flourish of an innate rune, say): a small spell. */
	public static final double DEFAULT_WEIGHT = 8.0;

	/**
	 * What was cast: the spell itself (so Mirrorfrost can cast it back), how many runes it has
	 * (a kill with six or more is a feat), and the element the caster leans toward.
	 */
	/** What was cast: its plan, how many runes, the caster's leaning, and the runes themselves (for its magic circles). */
	public record Info(dev.wildercord.spell.SpellPlan.Segment root, int runes, String leaning, java.util.List<dev.wildercord.spell.RuneDef> spell) {
		public static final Info NONE = new Info(null, 0, "");

		public Info(dev.wildercord.spell.SpellPlan.Segment root, int runes, String leaning) {
			this(root, runes, leaning, java.util.List.of());
		}
	}

	private static final class Budget {
		int entities = dev.wildercord.config.Config.get().maxCreatures();
		int blocks = dev.wildercord.config.Config.get().maxBlocks();
		final Shared shared;

		Budget(Shared shared) {
			this.shared = shared;
		}
	}

	public final LivingEntity caster;
	public final ServerLevel level;
	public final int depth;
	/** How many times this spell has been cast (1 = the first time), for Combo. */
	public final int castNumber;
	/** Heart Circles and Cord enchantments: multipliers on every effect's power and duration. */
	public final double power;
	public final double duration;
	/** A passive renewing itself: shapes skip their arrival and farewell flourishes. */
	public final boolean passive;
	/** For a passive: whether it's still on. Its lingering shapes stop the moment it isn't. */
	private final java.util.function.BooleanSupplier wanted;
	private final Budget budget;
	public final Info info;
	/**
	 * The repeats after an On Hit or On Kill (an Echo or a Pulse there) that have gone off in this paid
	 * run: the whole cast, or one of a Pulse's runs or an Echo's (see {@link #repeat()}). Children and
	 * pulses share it, so every hit of one run sees the same.
	 */
	private final java.util.Set<dev.wildercord.spell.SpellPlan.Link> repeated;

	public Cast(LivingEntity caster) {
		this(caster, 1, dev.wildercord.player.Heart.Bonuses.NONE, false, null, Info.NONE);
	}

	public Cast(LivingEntity caster, int castNumber, dev.wildercord.player.Heart.Bonuses bonuses, boolean passive, java.util.function.BooleanSupplier wanted,
			Info info) {
		this(caster, (ServerLevel) caster.level(), 0, new Budget(new Shared()), castNumber, bonuses.power(), bonuses.duration(), passive, wanted, info,
			new java.util.HashSet<>());
	}

	private Cast(LivingEntity caster, ServerLevel level, int depth, Budget budget, int castNumber, double power, double duration, boolean passive,
			java.util.function.BooleanSupplier wanted, Info info, java.util.Set<dev.wildercord.spell.SpellPlan.Link> repeated) {
		this.caster = caster;
		this.level = level;
		this.depth = depth;
		this.budget = budget;
		this.castNumber = castNumber;
		this.power = power;
		this.duration = duration;
		this.passive = passive;
		this.wanted = wanted;
		this.info = info;
		this.repeated = repeated;
	}

	/** Which press this belongs to: children, pulses and echoes of one cast share it (a new press has another). */
	public int id() {
		return System.identityHashCode(budget.shared);
	}

	/** Sets how charged the spell was (0 to 1), for its {@link dev.wildercord.cast.feel.Feel}. */
	public Cast charge(double charge) {
		budget.shared.charge = Math.max(0, Math.min(1, charge));
		return this;
	}

	/** How charged the spell was when released, 0 to 1. */
	public double charge() {
		return budget.shared.charge;
	}

	/** Sample environmental/stance choices once when this group releases. */
	public void prepareCircle(dev.wildercord.spell.SpellPlan.Group group) {
		if(dev.wildercord.spell.CircleDisciplines.selected(group.shapeMods)==null)return;
		budget.shared.circleConditions.computeIfAbsent(group,g -> {
			Vec3 velocity=caster.getDeltaMovement();
			long day=Math.floorMod(level.getOverworldClockTime(),24000);
			boolean night=level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime() && day>=13000 && day<23000;
			return dev.wildercord.spell.CircleDisciplines.conditional(g,
				velocity.x*velocity.x+velocity.z*velocity.z>=.0025,caster.isShiftKeyDown(),
				caster.isInWater() || level.isRainingAt(caster.blockPosition()),night);
		});
	}
	/** An effect-local view sharing payment and budgets; its bonuses never leak into a linked group. */
	public Cast circleEffect(dev.wildercord.spell.SpellPlan.Group group, dev.wildercord.spell.EffectKind kind) {
		if(dev.wildercord.spell.CircleDisciplines.selected(group.shapeMods)==null)return this;
		prepareCircle(group);
		double factor=budget.shared.circleConditions.get(group)*dev.wildercord.spell.CircleDisciplines.role(group,kind);
		return new Cast(caster,level,depth,budget,castNumber,power*factor,
			duration*dev.wildercord.spell.CircleDisciplines.profile(group).duration(),passive,wanted,info,repeated);
	}

	/** The feel of a group of this cast's plan (motion, element, role, scale band, modifiers), worked out once and adjusted by signatures. */
	public dev.wildercord.cast.feel.Feel feel(dev.wildercord.spell.SpellPlan.Group g) {
		return budget.shared.feels.computeIfAbsent(g,
			k -> dev.wildercord.cast.feel.Signatures.adjust(dev.wildercord.cast.feel.Feel.of(k, weight(), budget.shared.charge)));
	}

	/** The theme of a group with its feel attached: its first effect's element look, the signature's accent, and {@link #feel}. */
	public Vfx.Theme theme(dev.wildercord.spell.SpellPlan.Group g) {
		return dev.wildercord.cast.feel.Feels.themed(Vfx.theme(g), feel(g));
	}

	/** A continuation whose power is {@code multiplier} times this one's (the k-th On Hit trigger in a cast, say), sharing every budget. */
	public Cast child(double multiplier) {
		return new Cast(caster, level, depth + 1, budget, castNumber, power * multiplier, duration, passive, wanted, info, repeated);
	}

	public Cast child() {
		return new Cast(caster, level, depth + 1, budget, castNumber, power, duration, passive, wanted, info, repeated);
	}

	/**
	 * One strike of a shape that strikes repeatedly: its own creature and block budget. At the same depth: a strike
	 * is part of its shape, not a link, so it mustn't use up the {@link #MAX_DEPTH} links a cast may chain.
	 */
	public Cast pulse() {
		return new Cast(caster, level, depth, new Budget(budget.shared), castNumber, power, duration, passive, wanted, info, repeated);
	}

	/**
	 * A part of this cast that was paid for on its own: one of a Pulse's runs, or what an Echo repeats.
	 * A child, but with its own count of the repeats after On Hit or On Kill (see {@link #firstRepeat}).
	 */
	public Cast repeat() {
		return new Cast(caster, level, depth + 1, budget, castNumber, power, duration, passive, wanted, info, new java.util.HashSet<>());
	}

	/**
	 * True the first time this paid run reaches {@code link}, an Echo or a Pulse after an On Hit or On
	 * Kill ({@link dev.wildercord.spell.SpellPlan.Link#firstOnly}): the rest of the spell fires at every
	 * creature hit, but the repeat was paid for once, so it goes off once.
	 */
	public boolean firstRepeat(dev.wildercord.spell.SpellPlan.Link link) {
		return repeated.add(link);
	}

	/**
	 * How much mana the spell asks, as its list price (not what a discount made the caster pay): a
	 * Shield stops a spell that weighs no more than the one that raised it.
	 */
	public double weight() {
		Shared shared = budget.shared;
		if (shared.weight < 0) {
			shared.weight = info.root() != null ? dev.wildercord.spell.SpellCompiler.cost(info.root()) : DEFAULT_WEIGHT;
		}
		return shared.weight;
	}

	/** Sets what the whole cast weighs (see {@link #weight()}). */
	public Cast weigh(double weight) {
		budget.shared.weight = weight;
		return this;
	}

	/** Sets the casting gear the caster held (see {@link dev.wildercord.gear.Gear}); read by every part of the cast. */
	public Cast gear(dev.wildercord.gear.GearBonuses gear) {
		budget.shared.gear = gear;
		return this;
	}

	/** The casting gear the caster held (so a cast made from this one, like a chorus, can carry it on). */
	public dev.wildercord.gear.GearBonuses gear() {
		return budget.shared.gear;
	}

	/** Casting gear: the power multiplier on an effect of {@code element}. */
	public double gearPower(String element) {
		return budget.shared.gear.power(element);
	}

	/**
	 * Lets the caster's affinities with the elements add their power to this spell: a spell cast from their
	 * Cord, an imbued spell letting go, a spell turned back by Mirrorfrost. Scrolls, passives and the like go
	 * off at their plain strength, as they always did for leaning. Copies of the cast keep it.
	 */
	public Cast withAffinity() {
		budget.shared.affinity = true;
		return this;
	}

	/** Whether the caster's affinities count on this spell: see {@link #withAffinity}. */
	public boolean affinity() {
		return budget.shared.affinity;
	}

	/**
	 * Notes what the way it was cast added to this spell's power (an overchannel stage, a release on the
	 * beat, a traced glyph), which {@link #power} already holds. Against a player that share counts inside
	 * the bonus cap ({@link SpellDefenceRules#capBonus(double, double, double)}). Copies keep it.
	 */
	public Cast performance(double multiplier) {
		budget.shared.performance = Double.isNaN(multiplier) ? 1.0 : Math.max(1.0, multiplier);
		return this;
	}

	/** What the way it was cast added to this spell's power (1 for none): see {@link #performance(double)}. */
	public double performance() {
		return budget.shared.performance;
	}

	/**
	 * Marks this as a stored spell's release, set off at {@code origin} (the creature struck, the block broken, whoever
	 * stepped on the glyph): an Echo inside it repeats what was stored there, not from the caster.
	 */
	public Cast from(Trigger origin) {
		budget.shared.origin = origin;
		return this;
	}

	/** Where a stored spell's release was set off (see {@link #from}), or null for a spell cast from a Cord. */
	public Trigger origin() {
		return budget.shared.origin;
	}

	/** No Siphon for this cast: for one that was paid for earlier (an imbued release), so it can't earn its mana back again. */
	public Cast noSiphon() {
		budget.shared.paid.siphon = 0;
		return this;
	}

	/** The same for every part of one cast (links, pulses, echoes): so a Shield that stopped it stops all of it. */
	public Object identity() {
		return budget.shared;
	}

	/**
	 * The same for every copy of a spell paid for once (see {@link #again}): for a cap on what one payment may
	 * win back, which a storm's echo or Twin Star mustn't get a second time.
	 */
	public Object payment() {
		return budget.shared.paid;
	}

	/** Takes one segment from the whole cast's allowance; false once it's spent. */
	public boolean takeSegment() {
		return budget.shared.segments-- > 0;
	}

	/** True the first time {@code key} is asked for in this whole cast (links and echoes included). */
	public boolean once(String key) {
		return budget.shared.paid.once.add(key);
	}

	/** False once the caster has left, died or changed dimension: pending parts then fizzle. */
	public boolean alive() {
		return !caster.isRemoved() && caster.isAlive() && caster.level() == level && depth <= MAX_DEPTH && !budget.shared.cancelled
			&& (wanted == null || wanted.getAsBoolean());
	}

	/** Cuts the whole cast short: every pending part of it fizzles. */
	public void cancel() {
		budget.shared.cancelled = true;
	}

	/**
	 * The same spell going off again for the same payment, {@code multiplier} times as strong: a storm's
	 * echo, Twin Star, a Focus of Echoes, a wild surge. It gets its own creature, block and segment
	 * budgets (so a Shield that stopped the first doesn't stop it), but shares the first's Siphon cap
	 * and once-per-cast things (a second Imbue would store the spell twice for one price), and keeps
	 * its weight and casting gear.
	 */
	public Cast again(double multiplier) {
		return new Cast(caster, level, depth, new Budget(budget.shared.copy(true)), castNumber, power * multiplier, duration, passive, wanted, info,
			new java.util.HashSet<>());
	}

	/** The same cast, stronger or weaker: see {@link #again}. */
	public Cast withPower(double multiplier) {
		return again(multiplier);
	}

	/**
	 * The same spell, turned back by a parry: now {@code by}'s, at the same power (casting gear
	 * included) and weight, with a fresh budget (so the Shield that stopped the original doesn't stop it).
	 * It's still the one payment, so it shares the original's Siphon cap and once-per-cast things: a
	 * spell parried back and forth never earns mana back afresh.
	 */
	public Cast reflected(LivingEntity by) {
		weight();
		return new Cast(by, (ServerLevel) by.level(), 0, new Budget(budget.shared.copy(true)), 1, power, duration, false, null, info, new java.util.HashSet<>());
	}

	/** Takes up to {@code wanted} creatures from the budget and returns how many may be touched. */
	public int takeEntities(int wanted) {
		int granted = Math.min(wanted, budget.entities);
		budget.entities -= granted;
		return granted;
	}

	/**
	 * Siphon: each creature this cast hits returns mana, up to a cap per cast. The Cord's
	 * Siphon level is read once, when the first creature is hit.
	 */
	public void siphon(long creatures) {
		Paid shared = budget.shared.paid;
		if (creatures <= 0 || shared.siphon <= 0 || !(caster instanceof ServerPlayer player)) {
			return;
		}
		if (shared.siphonLevel < 0 || shared.siphoner != player) {
			shared.siphonLevel = dev.wildercord.player.Mana.of(player).siphon();
			shared.siphoner = player;
		}
		if (shared.siphonLevel == 0) {
			return;
		}
		int amount = (int) Math.min(shared.siphon, creatures * shared.siphonLevel * dev.wildercord.player.Mana.SIPHON_MANA);
		shared.siphon -= amount;
		dev.wildercord.player.Mana.restore(player, amount);
	}

	public boolean takeBlock() {
		if (budget.blocks <= 0) {
			return false;
		}
		budget.blocks--;
		return true;
	}

	/** Where a segment starts: after a link this is the thing that triggered it. */
	public record Trigger(Vec3 pos, Vec3 dir, Entity entity, BlockPos block, Direction face) {
		public static Trigger self(LivingEntity caster) {
			return new Trigger(caster.getEyePosition(), caster.getLookAngle(), caster, null, null);
		}

		public boolean fromCaster(LivingEntity caster) {
			return entity == caster;
		}
	}

	/**
	 * What a shape hit: creatures (maybe none), the point, and the block if one was hit.
	 * {@code origin} is where push and pull measure from; {@code power} multiplies its effects (1 for most shapes).
	 */
	public record Hit(List<Entity> entities, Vec3 point, Vec3 dir, Vec3 origin, BlockPos block, Direction face, boolean self, double power) {
		public Hit(List<Entity> entities, Vec3 point, Vec3 dir, Vec3 origin, BlockPos block, Direction face, boolean self) {
			this(entities, point, dir, origin, block, face, self, 1.0);
		}

		/** The same hit, its effects {@code multiplier} times as strong (a Touch's laying on of hands, the near half of a Cone). */
		public Hit times(double multiplier) {
			return new Hit(entities, point, dir, origin, block, face, self, power * multiplier);
		}
	}
}
