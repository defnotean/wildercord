package dev.wildercord.travel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Homes: {@code /sethome [name]}, {@code /home [name]}, {@code /delhome <name>} and {@code /homes}.
 * Each player keeps up to the server's {@code travel.max_homes} of them in their {@link TravelData};
 * setting one by a name they already use moves it.
 */
public final class Homes {
	private Homes() {}

	/** {@code /sethome}: sets (or moves) home {@code raw} where {@code player} stands. */
	public static int set(ServerPlayer player, String raw) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		if (name == null) {
			Travel.fail(player, "bad_name", TravelRules.MAX_NAME);
			return 0;
		}
		TravelData data = Travel.data(player);
		int max = Travel.config().maxHomes();
		TravelRules.HomeCheck check = TravelRules.setHome(data.homes().keySet(), name, max);
		if (check == TravelRules.HomeCheck.FULL) {
			Travel.fail(player, max == 0 ? "homes_off" : "homes_full", max);
			return 0;
		}
		Travel.update(player, d -> d.withHome(name, Spot.of(player)));
		boolean usual = name.equals(TravelRules.DEFAULT_HOME);
		String key = (check == TravelRules.HomeCheck.REPLACE ? "home_moved" : "home_set") + (usual ? "_default" : "");
		Travel.good(player, key, Travel.name(name));
		TravelFx.mark(player.level(), player.position());
		return 1;
	}

	/**
	 * {@code /home}: to home {@code raw}, or with {@code raw} null the usual one (the one called
	 * "home", or the only one); with several and none of them "home", the list to pick from.
	 */
	public static int go(ServerPlayer player, String raw, boolean operator) {
		if (!Travel.available(player)) {
			return 0;
		}
		TravelData data = Travel.data(player);
		if (data.homes().isEmpty()) {
			Travel.fail(player, "homes_none");
			return 0;
		}
		String name = raw == null ? TravelRules.defaultHome(data.homes().keySet()) : TravelRules.name(raw);
		if (name == null && raw == null) {
			return list(player);
		}
		Spot spot = name == null ? null : data.homes().get(name);
		if (spot == null) {
			Travel.fail(player, "home_missing", Travel.name(raw));
			return 0;
		}
		return Teleports.begin(player, Teleports.Kind.HOME, where(name), Teleports.to(spot, "home_unsafe", where(name)), operator);
	}

	/** {@code /delhome}: forgets home {@code raw}. */
	public static int delete(ServerPlayer player, String raw) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		if (name == null || !Travel.data(player).homes().containsKey(name)) {
			Travel.fail(player, "home_missing", Travel.name(raw));
			return 0;
		}
		Travel.update(player, d -> d.withoutHome(name));
		Travel.good(player, "home_deleted", Travel.name(name));
		return 1;
	}

	/** {@code /homes}: every home, each a button that goes there. */
	public static int list(ServerPlayer player) {
		if (!Travel.available(player)) {
			return 0;
		}
		Map<String, Spot> homes = Travel.data(player).homes();
		if (homes.isEmpty()) {
			Travel.info(player, "homes_none");
			return 0;
		}
		List<MutableComponent> buttons = new ArrayList<>();
		homes.forEach((name, spot) -> buttons.add(Travel.button(Travel.name(name).withStyle(ChatFormatting.UNDERLINE), "/home " + name,
			Travel.text("go_hover", where(name), spot.describe()))));
		player.sendSystemMessage(Travel.text("homes_list", homes.size(), Travel.config().maxHomes()).withStyle(ChatFormatting.GRAY)
			.append(Travel.row(buttons)));
		return homes.size();
	}

	/** "your home" for the usual one, "your home base" for another. */
	static Component where(String name) {
		return name.equals(TravelRules.DEFAULT_HOME) ? Travel.text("where.home_default") : Travel.text("where.home", Travel.name(name));
	}
}
