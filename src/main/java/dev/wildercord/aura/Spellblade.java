package dev.wildercord.aura;

import dev.wildercord.cast.BladeCasting;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.config.Config;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The spellblade (Edge and up): cast a Cord spell while sneaking with an aura weapon in hand, and instead of leaving it flows
 * into the blade. The next Aura Slash within a few seconds carries it: the slash takes the place of the spell's shape and its
 * effects land on what the slash cuts (the first few foes, each a little weaker), through the cast engine as any spell's do
 * ({@link BladeCasting}), so a Shield, the spell defences, the PvP cap and mastery all have their say. Both prices are paid: the
 * spell's mana and cooldown when it was cast, the slash's aura when it's loosed.
 *
 * <p>Unused, it slips off the blade when its time is up and leaves as it was cast. Sneaking is the choice: a spell cast
 * standing, sword or not, goes out as usual. Spells that only act on their caster (Self) and secret spells never ride a blade.</p>
 */
public final class Spellblade {
	private Spellblade() {}

	/** A spell riding a player's blade: the cast (paid for), what it is, until when, its colour, and how it leaves if unused. */
	public record Held(Cast cast, SpellPlan.Segment root, long until, int color, Consumer<Cast> release) {}

	private static final Map<UUID, Held> HELD = new HashMap<>();

	static void forget(UUID id) {
		HELD.remove(id);
	}

	static void clear() {
		HELD.clear();
	}

	/**
	 * Asked as a spell is cast, before anything leaves: whether it will ride the blade. Edge or higher, aura working, an aura
	 * weapon in hand, sneaking, not a secret, and something in it that reaches out.
	 */
	public static boolean wants(ServerPlayer player, SpellPlan.Segment root, boolean secret) {
		return !secret && Aura.enabled(player) && Aura.stage(player) >= AuraRules.EDGE && Aura.holdsWeapon(player) && player.isShiftKeyDown()
			&& BladeCasting.rides(root);
	}

	/**
	 * The spell, its formation done, flows into the blade instead of leaving. A spell already riding it slips off first and
	 * leaves as cast. Returns false (and the spell leaves as usual) if the player can no longer hold it.
	 */
	public static boolean draw(ServerPlayer player, Cast cast, SpellPlan.Segment root, Consumer<Cast> release) {
		if (!player.isAlive() || !Aura.holdsWeapon(player) || Aura.stage(player) < AuraRules.EDGE || !Aura.enabled(player)) {
			return false;
		}
		long now = player.level().getGameTime();
		Held old = HELD.remove(player.getUUID());
		if (old != null && old.cast().alive()) {
			old.release().accept(old.cast());
		}
		int color = BladeCasting.color(root);
		long until = now + Config.get().aura().heights().spellbladeTicks();
		HELD.put(player.getUUID(), new Held(cast, root, until, color, release));
		AuraPresence.look(player, AuraPresence.look(player).blade(color, until));
		Aura.sound(player, "aura_spellblade", 0.9F, 1.0F);
		AuraVfx.spellDrawn(player, color, Aura.color(player));
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.spellblade").withColor(0xFF000000 | color));
		return true;
	}

	/** Whether a spell rides {@code player}'s blade now (the server's view). */
	public static boolean holding(Player player) {
		Held held = HELD.get(player.getUUID());
		return held != null && player.level().getGameTime() <= held.until();
	}

	/** The spell riding the blade, for a slash being loosed now (it's taken off the blade), or null. */
	static Held take(ServerPlayer player) {
		Held held = HELD.remove(player.getUUID());
		AuraPresence.look(player, AuraPresence.look(player).blade(0, -1));
		if (held == null || player.level().getGameTime() > held.until() || !held.cast().alive()) {
			return null;
		}
		Grimoire.unlock(player, "aura:spellblade");
		return held;
	}

	/** Every tick: a spell unused past its time slips off the blade and leaves as cast; a blade put away lets it go at once. */
	static void tick(MinecraftServer server) {
		if (HELD.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Held>> it = HELD.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Held> entry = it.next();
			Held held = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !held.cast().alive()) {
				it.remove();
				continue;
			}
			long now = player.level().getGameTime();
			if (now > held.until() || !Aura.holdsWeapon(player)) {
				it.remove();
				AuraPresence.look(player, AuraPresence.look(player).blade(0, -1));
				AuraVfx.spellSlips(player, held.color());
				held.release().accept(held.cast());
			} else if ((now + player.getId()) % 4 == 0) {
				AuraVfx.spellRiding(player, held.color());
			}
		}
	}
}
