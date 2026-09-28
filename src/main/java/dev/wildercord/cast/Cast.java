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

	/** Limits for the whole cast, pulses included. */
	private static final class Shared {
		int siphon = dev.wildercord.player.Mana.SIPHON_CAP_PER_CAST;
		int siphonLevel = -1;
		final java.util.Set<String> once = new java.util.HashSet<>();
		/** Set when the whole cast is cut short, e.g. a Domain shattered in a clash. */
		boolean cancelled;
		int segments = MAX_SEGMENTS;
		/** The mana the spell asks, as a Shield weighs it; worked out from the plan when nobody set it. */
		double weight = -1;
		/** The casting gear in the caster's hands when it was cast (staffs and foci). */
		dev.wildercord.gear.GearBonuses gear = dev.wildercord.gear.GearBonuses.NONE;
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

	public Cast(LivingEntity caster) {
		this(caster, 1, dev.wildercord.player.Heart.Bonuses.NONE, false, null, Info.NONE);
	}

	public Cast(LivingEntity caster, int castNumber, dev.wildercord.player.Heart.Bonuses bonuses, boolean passive, java.util.function.BooleanSupplier wanted,
			Info info) {
		this(caster, (ServerLevel) caster.level(), 0, new Budget(new Shared()), castNumber, bonuses.power(), bonuses.duration(), passive, wanted, info);
	}

	private Cast(LivingEntity caster, ServerLevel level, int depth, Budget budget, int castNumber, double power, double duration, boolean passive,
			java.util.function.BooleanSupplier wanted, Info info) {
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
	}

	public Cast child() {
		return new Cast(caster, level, depth + 1, budget, castNumber, power, duration, passive, wanted, info);
	}

	/** One strike of a shape that strikes repeatedly: its own creature and block budget. */
	public Cast pulse() {
		return new Cast(caster, level, depth + 1, new Budget(budget.shared), castNumber, power, duration, passive, wanted, info);
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

	/** Casting gear: the power multiplier on an effect of {@code element}. */
	public double gearPower(String element) {
		return budget.shared.gear.power(element);
	}

	/** No Siphon for this cast: for one that was paid for earlier (an imbued release), so it can't earn its mana back again. */
	public Cast noSiphon() {
		budget.shared.siphon = 0;
		return this;
	}

	/** The same for every part of one cast (links, pulses, echoes): so a Shield that stopped it stops all of it. */
	public Object identity() {
		return budget.shared;
	}

	/** Takes one segment from the whole cast's allowance; false once it's spent. */
	public boolean takeSegment() {
		return budget.shared.segments-- > 0;
	}

	/** True the first time {@code key} is asked for in this whole cast (links and echoes included). */
	public boolean once(String key) {
		return budget.shared.once.add(key);
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

	/** The same cast, stronger: for the second copy of a Twin Star cast. */
	public Cast withPower(double multiplier) {
		return new Cast(caster, level, depth, new Budget(new Shared()), castNumber, power * multiplier, duration, passive, wanted, info);
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
		Shared shared = budget.shared;
		if (creatures <= 0 || shared.siphon <= 0 || !(caster instanceof ServerPlayer player)) {
			return;
		}
		if (shared.siphonLevel < 0) {
			shared.siphonLevel = dev.wildercord.player.Mana.of(player).siphon();
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
	 * {@code origin} is where push and pull measure from.
	 */
	public record Hit(List<Entity> entities, Vec3 point, Vec3 dir, Vec3 origin, BlockPos block, Direction face, boolean self) {}
}
