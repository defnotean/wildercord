package dev.wildercord.api;

import dev.wildercord.cast.AddonRunes;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneCategories;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.RuneNumbers;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Trait;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds and registers one add-on rune. Start from {@link WildercordApi#effect}, {@link WildercordApi#shape},
 * {@link WildercordApi#modifier} or {@link WildercordApi#link}, set what you need, then {@link #register()}:
 *
 * <pre>{@code
 * api.effect("example:frostbite", "Frostbite", "frost", EffectKind.HARMFUL)
 *     .tier(2).cost(7).traits(Trait.POWER, Trait.DURATION).category("damage")
 *     .description("4 frost damage, and slows for 5 seconds.")
 *     .onApply(ctx -> ctx.harmed().forEach(t -> ctx.hurt(t, ctx.magic(), 4 * ctx.power())))
 *     .register();
 * }</pre>
 *
 * <p>The same rules as built-in runes apply (see docs/ADDING_RUNES.md): effects always get
 * {@link Trait#FRUGAL}, shapes always get {@link Trait#COOLDOWN}, a modifier attaches to the closest
 * rune on its left with the trait it {@link #needs needs}, and a tier decides which Cord can hold it.</p>
 *
 * @since 1.0
 */
public final class RuneBuilder {
	private final RuneFamily family;
	private final String id;
	private final String name;
	private int tier = 1;
	private double cost;
	private double multiplier = 1.0;
	private String element = "";
	private EffectKind kind = EffectKind.NONE;
	private final Set<String> traits = new LinkedHashSet<>();
	private String needs = "";
	private String description = "";
	private String category;
	private EffectBehaviour effect;
	private ShapeBehaviour shape;
	private LinkBehaviour link;
	private RuneNumbers.Numbers numbers;

	RuneBuilder(RuneFamily family, String id, String name) {
		this.family = family;
		this.id = id;
		this.name = name;
	}

	/** 1 to 4: which Cord can hold it (Twine I, Copper II, Amethyst III, Echo IV). */
	public RuneBuilder tier(int tier) {
		this.tier = tier;
		return this;
	}

	/** Mana, for shapes, effects and links (modifiers cost nothing themselves: see {@link #multiplier}). */
	public RuneBuilder cost(double cost) {
		this.cost = cost;
		return this;
	}

	/** Shapes: multiplier on the cost of the effects they carry. Modifiers: on the cost of what they attach to. */
	public RuneBuilder multiplier(double multiplier) {
		this.multiplier = multiplier;
		return this;
	}

	/** What modifiers may change on it (see {@link Trait}). */
	public RuneBuilder traits(String... traits) {
		this.traits.addAll(Set.of(traits));
		return this;
	}

	/** Modifiers: the trait a rune must have for this to attach to it. */
	public RuneBuilder needs(String trait) {
		this.needs = trait;
		return this;
	}

	public RuneBuilder description(String description) {
		this.description = description;
		return this;
	}

	/**
	 * The Codex category it's listed under (see {@link RuneCategories}); a new one is added to its
	 * family. Defaults: effects "damage", shapes "direct", modifiers "power", links "trigger".
	 */
	public RuneBuilder category(String category) {
		this.category = category;
		return this;
	}

	/** Effects: what it does. */
	public RuneBuilder onApply(EffectBehaviour behaviour) {
		this.effect = behaviour;
		return this;
	}

	/** Shapes: where it goes. */
	public RuneBuilder onDeliver(ShapeBehaviour behaviour) {
		this.shape = behaviour;
		return this;
	}

	/** Links: when the rest fires. */
	public RuneBuilder onLink(LinkBehaviour behaviour) {
		this.link = behaviour;
		return this;
	}

	/**
	 * Modifiers: multipliers on the power and duration of the effect it attaches to, and the radius of
	 * the shape or effect it attaches to. The readout and the cast both use them.
	 */
	public RuneBuilder numbers(double power, double duration, double radius) {
		this.numbers = new RuneNumbers.Numbers(power, duration, radius);
		return this;
	}

	RuneBuilder element(String element, EffectKind kind) {
		this.element = element;
		this.kind = kind;
		return this;
	}

	/** The rune this builder describes, checked but not registered. */
	public RuneDef build() {
		int colon = id.indexOf(':');
		if (colon <= 0 || colon == id.length() - 1 || id.startsWith("wildercord:")) {
			throw new IllegalArgumentException("A rune id needs the add-on's own namespace, like \"example:frostbite\": " + id);
		}
		if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
			throw new IllegalArgumentException("A rune id may only use a-z, 0-9, _ . - and /: " + id);
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Rune " + id + " needs a name");
		}
		if (tier < 1 || tier > 4) {
			throw new IllegalArgumentException("Rune " + id + ": tier must be 1 to 4, not " + tier);
		}
		if (cost < 0 || multiplier <= 0 || Double.isNaN(cost) || Double.isNaN(multiplier)) {
			throw new IllegalArgumentException("Rune " + id + ": cost can't be negative and the multiplier must be positive");
		}
		Set<String> all = new LinkedHashSet<>(traits);
		switch (family) {
			case EFFECT -> {
				if (kind == EffectKind.NONE) {
					throw new IllegalArgumentException("Effect " + id + " needs a kind (HELPFUL, HARMFUL, WORLD or MOVEMENT)");
				}
				all.add(Trait.FRUGAL);
			}
			case SHAPE -> all.add(Trait.COOLDOWN);
			case MODIFIER -> {
				if (needs.isEmpty()) {
					throw new IllegalArgumentException("Modifier " + id + " needs a trait to attach to (see Trait)");
				}
				if (!all.isEmpty()) {
					throw new IllegalArgumentException("Modifier " + id + " can't have traits of its own");
				}
			}
			case LINK -> { }
			case KNOT -> throw new IllegalArgumentException("Rune " + id + ": Knots are tied at the Fusion Altar, not registered");
		}
		String cat = category != null ? category : switch (family) {
			case EFFECT -> "damage";
			case SHAPE -> "direct";
			case MODIFIER -> "power";
			case LINK, KNOT -> "trigger";
		};
		return new RuneDef(id, name, family, tier, family == RuneFamily.MODIFIER ? 0 : cost, family == RuneFamily.EFFECT || family == RuneFamily.LINK ? 1.0 : multiplier,
			family == RuneFamily.EFFECT ? element : "", family == RuneFamily.EFFECT ? kind : EffectKind.NONE, all, family == RuneFamily.MODIFIER ? needs : "",
			description, cat);
	}

	/** Checks and registers the rune (and its behaviour). Call during {@link WildercordAddon#onWildercordInit}. */
	public RuneDef register() {
		RuneDef def = build();
		RuneCategories.add(family, def.category());
		Runes.registerAddon(def);
		if (numbers != null) {
			RuneNumbers.register(def.id(), numbers);
		}
		if (effect != null) {
			AddonRunes.effect(def.id(), effect);
		}
		if (shape != null) {
			AddonRunes.shape(def.id(), shape);
		}
		if (link != null) {
			AddonRunes.link(def.id(), link);
		}
		return def;
	}
}
