package dev.wildercord.loadout;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.PassiveCaster;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.gear.SpellSlots;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Loadouts: saved Cord setups a player can save, name, load and delete, from the Cord screen, the
 * quick-switch key or {@code /loadout}. Everything here runs on the server; the client only asks.
 *
 * <p>Saving copies the server's own spellbook, never anything the client sends, and loading only
 * rearranges rune ids in it: the runes learned don't change, so every rule that keeps a spell honest
 * ({@link SpellCaster#activeSockets}: learned, loaded, held by the Cord, inside its sockets) still
 * decides what fires. Loading puts each spell that changed on its cooldown (see
 * {@link LoadoutRules#readyAfterLoad}), and is refused while charging, in a duel or sealed in ice.</p>
 */
public final class Loadouts {
	private Loadouts() {}

	/** Each player's loadouts. Saved, kept through death, and synced to that player for the Cord screen. */
	public static final AttachmentType<LoadoutData> DATA = AttachmentRegistry.create(
		Wildercord.id("loadouts"),
		builder -> builder
			.initializer(() -> LoadoutData.EMPTY)
			.persistent(LoadoutData.CODEC)
			.syncWith(LoadoutData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** What a request came to: whether it was done, and what to tell the player either way. */
	public record Result(boolean ok, Component message) {
		static Result done(String key, Object... args) {
			return new Result(true, text(key, args));
		}

		static Result refused(String key, Object... args) {
			return new Result(false, text(key, args));
		}
	}

	private static final String KEY = "message.wildercord.loadout.";

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> LoadoutCommands.register(dispatcher));
	}

	public static LoadoutData data(Player player) {
		return player.getAttachedOrElse(DATA, LoadoutData.EMPTY);
	}

	private static void set(ServerPlayer player, LoadoutData data) {
		player.setAttached(DATA, data);
	}

	static MutableComponent text(String key, Object... args) {
		return Component.translatable(KEY + key, args);
	}

	/** A loadout's name, picked out. */
	static MutableComponent name(String name) {
		return Component.literal(name).withColor(0xE8C46A);
	}

	// ------------------------------------------------------------------ saving

	/**
	 * Saves the Cord as it is now under {@code raw}. With {@code replace}, a loadout already called that
	 * is saved over (the command); without it, the name must be new (the Cord screen, which saves over a
	 * loadout with its own button and a confirm).
	 */
	public static Result save(ServerPlayer player, String raw, boolean replace) {
		String name = LoadoutRules.name(raw);
		if (name == null) {
			return Result.refused("bad_name", LoadoutRules.MAX_NAME);
		}
		LoadoutData data = data(player);
		Loadout loadout = Loadout.of(name, Spellbooks.get(player));
		switch (LoadoutRules.save(data.names(), name, LoadoutRules.MAX)) {
			case REPLACE -> {
				int index = LoadoutRules.find(data.names(), name);
				if (!replace) {
					return Result.refused("name_taken", name(data.get(index).name()));
				}
				set(player, data.replace(index, loadout.named(data.get(index).name()), true));
				return Result.done("replaced", name(data.get(index).name()));
			}
			case FULL -> {
				return Result.refused("full", LoadoutRules.MAX);
			}
			default -> {
				set(player, data.with(loadout));
				return Result.done("saved", name(name), data.size() + 1, LoadoutRules.MAX);
			}
		}
	}

	/** Saves the Cord as it is now over loadout {@code index}, keeping its name. */
	public static Result saveOver(ServerPlayer player, int index) {
		LoadoutData data = data(player);
		Loadout old = data.get(index);
		if (old == null) {
			return Result.refused("gone");
		}
		set(player, data.replace(index, Loadout.of(old.name(), Spellbooks.get(player)), true));
		return Result.done("replaced", name(old.name()));
	}

	public static Result rename(ServerPlayer player, int index, String raw) {
		LoadoutData data = data(player);
		Loadout old = data.get(index);
		if (old == null) {
			return Result.refused("gone");
		}
		String name = LoadoutRules.name(raw);
		if (name == null) {
			return Result.refused("bad_name", LoadoutRules.MAX_NAME);
		}
		int other = LoadoutRules.find(data.names(), name);
		if (other >= 0 && other != index) {
			return Result.refused("name_taken", name(data.get(other).name()));
		}
		set(player, data.replace(index, old.named(name), false));
		return Result.done("renamed", name(old.name()), name(name));
	}

	public static Result delete(ServerPlayer player, int index) {
		LoadoutData data = data(player);
		Loadout old = data.get(index);
		if (old == null) {
			return Result.refused("gone");
		}
		set(player, data.without(index));
		return Result.done("deleted", name(old.name()));
	}

	/** The loadout called {@code raw} (ignoring case), or -1. */
	public static int find(Player player, String raw) {
		return LoadoutRules.find(data(player).names(), LoadoutRules.name(raw));
	}

	// ------------------------------------------------------------------ loading

	/** Why a loadout can't load right now, or null if it can. */
	public static Component blocked(ServerPlayer player) {
		LoadoutRules.Block block = LoadoutRules.block(Spellbooks.tier(player) != null, player.isAlive() && !player.isSpectator(),
			player.hasAttached(WildercordAttachments.CHARGE), dev.wildercord.duel.Duels.inDuel(player), SpellCaster.sealed(player));
		return switch (block) {
			case NONE -> null;
			case NO_CORD -> Component.translatable("message.wildercord.no_cord");
			case DEAD -> text("dead");
			case CHARGING -> text("charging");
			case DUEL -> text("duel");
			case SEALED -> text("sealed");
		};
	}

	/**
	 * Loads loadout {@code index} onto the Cord: every spell row, its name, the passives, their switches
	 * and the selected spell. Runes the player doesn't know, too strong for the Cord worn, or in sockets
	 * and rows it doesn't have are kept but quiet, as after changing to a smaller Cord. Each spell whose
	 * firing runes change starts its cooldown, unless it's cooling down already.
	 */
	public static Result load(ServerPlayer player, int index) {
		return load(player, index, false);
	}

	/** The quick-switch key: loads the loadout after the last one loaded, and names it. */
	public static Result next(ServerPlayer player) {
		LoadoutData data = data(player);
		int index = LoadoutRules.next(data.current(), data.size());
		if (index < 0) {
			return Result.refused("none", Component.keybind("key.wildercord.open_cord"));
		}
		return load(player, index, true);
	}

	private static Result load(ServerPlayer player, int index, boolean switched) {
		LoadoutData data = data(player);
		Loadout loadout = data.get(index);
		if (loadout == null) {
			return Result.refused("gone");
		}
		Component why = blocked(player);
		if (why != null) {
			return new Result(false, why);
		}
		CordTier tier = Spellbooks.tier(player);
		Spellbook before = Spellbooks.get(player);
		Spellbook after = loadout.applyTo(before);
		long now = player.level().getGameTime();
		long[] readyAt = new long[SpellSlots.ALL];
		for (int slot = 0; slot < readyAt.length; slot++) {
			readyAt[slot] = Spellbooks.readyAt(player, slot);
		}
		long[] ready = LoadoutRules.readyAfterLoad(firing(before, tier), firing(after, tier), readyAt, now, slot -> cooldown(player, after, slot, tier));
		Spellbooks.set(player, after);
		for (int slot = 0; slot < ready.length; slot++) {
			if (ready[slot] != readyAt[slot]) {
				Spellbooks.setReadyAt(player, slot, ready[slot]);
			}
		}
		set(player, data.withCurrent(index));
		int quiet = quiet(after, tier);
		MutableComponent name = name(loadout.name());
		if (switched) {
			return quiet == 0 ? Result.done("switched", name, index + 1, data.size())
				: Result.done("switched_quiet", name, index + 1, data.size(), quiet);
		}
		return quiet == 0 ? Result.done("loaded", name) : Result.done("loaded_quiet", name, quiet);
	}

	/** The runes that fire in each spell slot of {@code book} on a Cord of {@code tier}. */
	private static List<List<String>> firing(Spellbook book, CordTier tier) {
		List<List<String>> rows = new ArrayList<>();
		for (int slot = 0; slot < SpellSlots.ALL; slot++) {
			rows.add(SpellCaster.activeRunes(book, slot, tier).stream().map(RuneDef::id).toList());
		}
		return rows;
	}

	/** What spell {@code slot} of {@code book} would cool down for once cast (0 when it has nothing to cast), as a cast works it out. */
	private static int cooldown(ServerPlayer player, Spellbook book, int slot, CordTier tier) {
		List<RuneDef> runes = SpellCaster.activeRunes(book, slot, tier);
		if (runes.isEmpty()) {
			return 0;
		}
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		return compiled.isEmpty() ? 0
			: Heart.cooldownTicks(player, compiled, Heart.secretCooldown(player, runes) * dev.wildercord.cast.Mastery.cooldownFactor(player, runes));
	}

	/** Runes threaded in {@code book} that won't fire on this Cord: spells first, then passives. */
	static int quiet(Spellbook book, CordTier tier) {
		int quiet = 0;
		for (int slot = 0; slot < SpellSlots.ALL; slot++) {
			List<String> ids = book.spells().get(slot);
			quiet += ids.size() - SpellCaster.activeSockets(ids, book, slot, tier).size();
		}
		for (List<String> passive : book.passives()) {
			quiet += passive.size() - PassiveCaster.activeRunes(passive, book, tier).size();
		}
		return quiet;
	}

	// ------------------------------------------------------------------ requests from the Cord screen and the key

	/** What the client may ask for (see {@code WildercordNetworking.LoadoutRequest}). */
	public static final int SAVE_NEW = 0;
	public static final int SAVE_OVER = 1;
	public static final int LOAD = 2;
	public static final int RENAME = 3;
	public static final int DELETE = 4;
	public static final int NEXT = 5;

	/** Handles one request from the client; the caller has already rate-limited it. */
	public static Result request(ServerPlayer player, int kind, int index, String name) {
		return switch (kind) {
			case SAVE_NEW -> save(player, name, false);
			case SAVE_OVER -> saveOver(player, index);
			case LOAD -> load(player, index);
			case RENAME -> rename(player, index, name);
			case DELETE -> delete(player, index);
			case NEXT -> next(player);
			default -> null;
		};
	}
}
