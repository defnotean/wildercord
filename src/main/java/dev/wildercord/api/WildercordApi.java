package dev.wildercord.api;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.AddonRunes;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneCategories;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Wildercord's public API for add-ons. An add-on gets it in {@link WildercordAddon#onWildercordInit}
 * (or any time from {@link #get()}), registers runes, categories and reactions, listens to
 * {@link WildercordEvents}, and reads players' magic.
 *
 * <p>Within 1.x this package only grows: nothing public here is removed or renamed. See
 * {@code docs/API.md} for a walk-through and a complete example add-on.</p>
 *
 * @since 1.0
 */
public final class WildercordApi {
	/** The API's version: the minor number grows when something is added. */
	public static final String VERSION = "1.0";
	/** The {@code fabric.mod.json} entrypoint key add-ons list their {@link WildercordAddon} under. */
	public static final String ENTRYPOINT = "wildercord";

	private static final WildercordApi INSTANCE = new WildercordApi();

	private WildercordApi() {}

	public static WildercordApi get() {
		return INSTANCE;
	}

	// ------------------------------------------------------------------ runes

	/**
	 * A new effect rune: what happens to whatever its shape hits.
	 *
	 * @param id      namespaced, e.g. {@code example:frostbite}
	 * @param element fire, frost, storm, wind, earth, life, void, arcane, time, blood, or "" for none
	 * @param kind    who it may touch: HELPFUL, HARMFUL, WORLD or MOVEMENT
	 */
	public RuneBuilder effect(String id, String name, String element, EffectKind kind) {
		return new RuneBuilder(RuneFamily.EFFECT, id, name).element(element, kind);
	}

	/** A new shape rune: where the spell goes. Give it {@link RuneBuilder#onDeliver}. */
	public RuneBuilder shape(String id, String name) {
		return new RuneBuilder(RuneFamily.SHAPE, id, name);
	}

	/** A new modifier rune: numbers on the closest rune to its left with the trait it {@link RuneBuilder#needs needs}. */
	public RuneBuilder modifier(String id, String name) {
		return new RuneBuilder(RuneFamily.MODIFIER, id, name);
	}

	/** A new link rune: when the rest fires. Give it {@link RuneBuilder#onLink}. */
	public RuneBuilder link(String id, String name) {
		return new RuneBuilder(RuneFamily.LINK, id, name);
	}

	/** Any rune, built-in or from an add-on, by its id. */
	public Optional<RuneDef> rune(String id) {
		return Runes.get(id);
	}

	/** Every rune, in Codex order. */
	public Collection<RuneDef> runes() {
		return Runes.all();
	}

	/**
	 * Adds a Codex category to a family, after the built-in ones. Its label is the lang key
	 * {@code category.wildercord.<family>.<category>} (family in lower case), from your own lang file.
	 */
	public void registerCategory(RuneFamily family, String category) {
		RuneCategories.add(family, category);
	}

	/** Every category a family's Codex shows, in order. */
	public List<String> categories(RuneFamily family) {
		return RuneCategories.of(family);
	}

	// ------------------------------------------------------------------ element reactions

	/**
	 * Registers an element reaction: spell damage of {@code element} asks it for a multiplier. Registering
	 * again with the same {@code id} replaces it.
	 */
	public void registerReaction(String id, String element, ElementReaction reaction) {
		AddonRunes.reaction(id, element, reaction);
	}

	/** Leaves a mark on a creature for {@code ticks} (a key of your own, e.g. {@code example:soaked}). */
	public void mark(Entity target, String key, int ticks) {
		AddonRunes.mark(target, key, ticks);
	}

	public boolean hasMark(Entity target, String key) {
		return AddonRunes.hasMark(target, key);
	}

	public void clearMark(Entity target, String key) {
		AddonRunes.clearMark(target, key);
	}

	// ------------------------------------------------------------------ reading a player's magic

	/** Current mana (0 without a Cord). Safe on both sides; the client only knows its own player's. */
	public float mana(Player player) {
		return Spellbooks.mana(player);
	}

	/** Max mana from every source (0 without a Cord). */
	public int maxMana(Player player) {
		return Mana.max(player);
	}

	/** The ids of every rune the player has learned. */
	public List<String> knownRunes(Player player) {
		return Spellbooks.get(player).learned();
	}

	/** Whether the player has learned a rune. */
	public boolean knows(Player player, String runeId) {
		return Spellbooks.knows(player, runeId);
	}

	/**
	 * The player's spells as rune ids, one list per slot (four for the Cord, the fifth the Tome of the
	 * Fifth Page's), including runes that are threaded but silent.
	 */
	public List<List<String>> spells(Player player) {
		return Spellbooks.get(player).spells();
	}

	/** The spell slot the player has selected. */
	public int selectedSpell(Player player) {
		return Spellbooks.get(player).selected();
	}

	/** Whether the player is wearing a Cord (they can't cast without one). */
	public boolean wearsCord(Player player) {
		return Spellbooks.tier(player) != null;
	}

	// ------------------------------------------------------------------ loading add-ons

	private static boolean loaded;

	/** Calls every add-on's {@link WildercordAddon}, once. An add-on that throws is logged and skipped. */
	public static synchronized void loadAddons() {
		if (loaded) {
			return;
		}
		loaded = true;
		AddonRunes.init();
		for (EntrypointContainer<WildercordAddon> container : FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, WildercordAddon.class)) {
			String mod = container.getProvider().getMetadata().getId();
			try {
				container.getEntrypoint().onWildercordInit(INSTANCE);
				Wildercord.LOGGER.info("Loaded Wildercord add-on {}", mod);
			} catch (RuntimeException | LinkageError e) {
				Wildercord.LOGGER.error("Wildercord add-on {} failed to load", mod, e);
			}
		}
	}
}
