package dev.wildercord.guild;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Guilds and covens (0.13): standing groups that, unlike a party, outlast a server restart. A guild gathers swordsmen and a
 * coven gathers mages; a player can be in one of each. Members training near each other learn a little faster: a guild's
 * aura experience and a coven's condensed mana both count 5% more for each fellow member close by, up to three of them. Pure
 * rules, so the ledger, the commands and the tests agree.
 */
public final class GuildRules {
	private GuildRules() {}

	public enum Kind {
		GUILD, COVEN;

		public String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final int MAX_MEMBERS = 12;
	public static final int NAME_MIN = 3;
	public static final int NAME_MAX = 24;
	/** Emeralds it takes to found one. */
	public static final int FOUND_COST = 32;
	/** How close a fellow member has to be to count. */
	public static final double NEAR = 32;
	public static final double PER_MEMBER = 0.05;
	public static final int MAX_COUNTED = 3;
	/** Ticks an invitation stays open. */
	public static final long INVITE_TICKS = 20 * 60;

	/** What a member's training counts for with {@code nearby} fellow members close by. */
	public static double bonus(int nearby) {
		return 1 + PER_MEMBER * Math.max(0, Math.min(MAX_COUNTED, nearby));
	}

	/** Letters, digits, spaces, apostrophes and hyphens; starting with a letter or digit; no doubled spaces. */
	public static boolean validName(String name) {
		if (name == null) return false;
		String trimmed = name.trim();
		return trimmed.equals(name) && name.length() >= NAME_MIN && name.length() <= NAME_MAX && !name.contains("  ")
			&& name.matches("[A-Za-z0-9][A-Za-z0-9 '\\-]*");
	}

	/** Names are unique whatever their case. */
	public static String key(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

	/** One guild or coven: its name, its kind, its leader and its members by seniority (the leader among them). */
	public record Guild(String name, Kind kind, UUID leader, List<UUID> members) {
		public Guild {
			members = List.copyOf(members);
		}

		public boolean has(UUID player) {
			return members.contains(player);
		}
	}

	public enum Result { OK, BAD_NAME, NAME_TAKEN, ALREADY_IN, NOT_IN, NOT_LEADER, FULL, NO_SUCH, NOT_MEMBER, SELF }

	/** Every guild and coven there is. */
	public static final class Roster {
		private final Map<String, Guild> byKey = new LinkedHashMap<>();

		public Roster() {}

		public Roster(Collection<Guild> guilds) {
			for (Guild g : guilds) byKey.put(key(g.name()), g);
		}

		public List<Guild> guilds() {
			return List.copyOf(byKey.values());
		}

		public Guild named(String name) {
			return name == null ? null : byKey.get(key(name));
		}

		/** The {@code kind} {@code player} is in, or null. */
		public Guild of(UUID player, Kind kind) {
			for (Guild g : byKey.values()) if (g.kind() == kind && g.has(player)) return g;
			return null;
		}

		public Result found(UUID leader, Kind kind, String name) {
			if (!validName(name)) return Result.BAD_NAME;
			if (byKey.containsKey(key(name))) return Result.NAME_TAKEN;
			if (of(leader, kind) != null) return Result.ALREADY_IN;
			byKey.put(key(name), new Guild(name, kind, leader, List.of(leader)));
			return Result.OK;
		}

		/** Whether {@code from} may invite {@code to} into their {@code kind}: only its leader invites. */
		public Result canInvite(UUID from, UUID to, Kind kind) {
			if (from.equals(to)) return Result.SELF;
			Guild g = of(from, kind);
			if (g == null) return Result.NOT_IN;
			if (!g.leader().equals(from)) return Result.NOT_LEADER;
			if (of(to, kind) != null) return Result.ALREADY_IN;
			if (g.members().size() >= MAX_MEMBERS) return Result.FULL;
			return Result.OK;
		}

		/** {@code player} joins the one named {@code name} (their invitation already checked). */
		public Result join(UUID player, String name) {
			Guild g = named(name);
			if (g == null) return Result.NO_SUCH;
			if (of(player, g.kind()) != null) return Result.ALREADY_IN;
			if (g.members().size() >= MAX_MEMBERS) return Result.FULL;
			List<UUID> members = new ArrayList<>(g.members());
			members.add(player);
			byKey.put(key(g.name()), new Guild(g.name(), g.kind(), g.leader(), members));
			return Result.OK;
		}

		/** {@code player} leaves their {@code kind}. A leader leaving hands it to the longest-standing member; the last one out ends it. */
		public Result leave(UUID player, Kind kind) {
			Guild g = of(player, kind);
			if (g == null) return Result.NOT_IN;
			List<UUID> members = new ArrayList<>(g.members());
			members.remove(player);
			if (members.isEmpty()) {
				byKey.remove(key(g.name()));
			} else {
				UUID leader = g.leader().equals(player) ? members.getFirst() : g.leader();
				byKey.put(key(g.name()), new Guild(g.name(), g.kind(), leader, members));
			}
			return Result.OK;
		}

		public Result kick(UUID leader, UUID member, Kind kind) {
			if (leader.equals(member)) return Result.SELF;
			Guild g = of(leader, kind);
			if (g == null) return Result.NOT_IN;
			if (!g.leader().equals(leader)) return Result.NOT_LEADER;
			if (!g.has(member)) return Result.NOT_MEMBER;
			return leave(member, kind);
		}

		public Result disband(UUID leader, Kind kind) {
			Guild g = of(leader, kind);
			if (g == null) return Result.NOT_IN;
			if (!g.leader().equals(leader)) return Result.NOT_LEADER;
			byKey.remove(key(g.name()));
			return Result.OK;
		}
	}
}
