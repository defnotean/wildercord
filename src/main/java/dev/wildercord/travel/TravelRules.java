package dev.wildercord.travel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The travel commands' rules as plain Java (no Minecraft types, so they're unit-tested): what a home,
 * warp or waypoint may be called, how many homes fit, when a warmup is broken by moving, cooldowns,
 * teleport requests and when they run out, where a random teleport may aim, and which way the
 * waypoint arrow points. The numbers a server owner sets come from the config; the rest are here.
 */
public final class TravelRules {
	private TravelRules() {}

	/** The name {@code /sethome} and {@code /home} use when none is given. */
	public static final String DEFAULT_HOME = "home";
	/** The longest name a home, warp or waypoint may have. */
	public static final int MAX_NAME = 24;
	/** Waypoints one player may keep. */
	public static final int MAX_WAYPOINTS = 50;
	/** How far a player may drift during a warmup before it's cancelled, in blocks. */
	public static final double MOVE_TOLERANCE = 0.5;
	/** How close a tracked waypoint must be for its beam to show, in blocks (level distance). */
	public static final double BEAM_RANGE = 96.0;
	/** Spots a random teleport looks at before giving up. Most are passed over cheaply (an ocean, say). */
	public static final int RTP_TRIES = 64;
	/** Chunks a random teleport may load (or generate) looking for ground, so one command can't stall the server long. */
	public static final int RTP_CHUNK_LOADS = 12;
	/** Server ticks in a second. */
	public static final int TICKS_PER_SECOND = 20;

	// ------------------------------------------------------------------ names

	/**
	 * A name as it's kept: trimmed and in lower case. Null when it can't be one: empty, longer than
	 * {@link #MAX_NAME}, or with anything but letters, digits, {@code -} and {@code _}.
	 */
	public static String name(String raw) {
		if (raw == null) {
			return null;
		}
		String name = raw.trim().toLowerCase(Locale.ROOT);
		if (name.isEmpty() || name.length() > MAX_NAME) {
			return null;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-' || c == '_';
			if (!ok) {
				return null;
			}
		}
		return name;
	}

	/** What setting a home by this name would do. */
	public enum HomeCheck { NEW, REPLACE, FULL }

	/** Setting home {@code name}: a new one, moving one they have (always allowed), or refused because {@code max} are set. */
	public static HomeCheck setHome(Collection<String> homes, String name, int max) {
		if (homes.contains(name)) {
			return HomeCheck.REPLACE;
		}
		return homes.size() >= max ? HomeCheck.FULL : HomeCheck.NEW;
	}

	/**
	 * The home {@code /home} goes to without a name: the one called {@link #DEFAULT_HOME}, or the only
	 * one there is. Null when there's none, or several and none of them is the default.
	 */
	public static String defaultHome(Collection<String> homes) {
		if (homes.contains(DEFAULT_HOME)) {
			return DEFAULT_HOME;
		}
		return homes.size() == 1 ? homes.iterator().next() : null;
	}

	// ------------------------------------------------------------------ warmups and cooldowns

	/** Whether a player has moved far enough from where their warmup began to break it. */
	public static boolean moved(double dx, double dy, double dz) {
		return dx * dx + dy * dy + dz * dz > MOVE_TOLERANCE * MOVE_TOLERANCE;
	}

	/** Ticks left before {@code readyAt} (0 once it's passed). */
	public static long remaining(long now, long readyAt) {
		return Math.max(0, readyAt - now);
	}

	/** Ticks as whole seconds, rounded up, so "1 second" is shown until the very end. */
	public static long seconds(long ticks) {
		return (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
	}

	// ------------------------------------------------------------------ directions and random spots

	/**
	 * Which way to turn to face a point {@code dx}, {@code dz} blocks away while facing {@code yaw}
	 * (Minecraft's: 0 is south, 90 west, rising as you turn right): degrees from -180 to 180, positive
	 * to the right. The waypoint arrow is turned by this.
	 */
	public static double bearing(double dx, double dz, double yaw) {
		double toward = Math.toDegrees(Math.atan2(-dx, dz));
		return wrap(toward - yaw);
	}

	/** An angle folded into -180 (inclusive) to 180 (exclusive). */
	static double wrap(double degrees) {
		double d = degrees % 360.0;
		if (d >= 180.0) {
			d -= 360.0;
		} else if (d < -180.0) {
			d += 360.0;
		}
		return d;
	}

	/**
	 * A random column within {@code radius} of {@code cx}, {@code cz}, spread evenly over the disc,
	 * from two random numbers {@code u} and {@code v} in [0, 1).
	 *
	 * @return {x, z}
	 */
	public static int[] randomPoint(double u, double v, int cx, int cz, int radius) {
		double r = radius * Math.sqrt(u);
		double a = v * Math.PI * 2.0;
		return new int[] {cx + (int) Math.floor(r * Math.cos(a)), cz + (int) Math.floor(r * Math.sin(a))};
	}

	// ------------------------------------------------------------------ teleport requests

	/**
	 * A teleport request: {@code from} asks to go to {@code to} (with {@code here} false, {@code /tpa}),
	 * or asks {@code to} to come to them ({@code here} true, {@code /tpahere}). {@code made} is when, in
	 * server ticks.
	 */
	public record Request(UUID from, UUID to, boolean here, long made) {
		/** Whether it has waited {@code timeout} ticks or more. */
		public boolean expired(long now, long timeout) {
			return now - made >= timeout;
		}

		/** Who is teleported if it's accepted: the asker for {@code /tpa}, the one asked for {@code /tpahere}. */
		public UUID traveller() {
			return here ? to : from;
		}

		/** Who the traveller goes to. */
		public UUID host() {
			return here ? from : to;
		}
	}

	/**
	 * Every teleport request waiting for an answer. A player may have requests out to several people,
	 * but only one to each: a new request to the same person replaces the old one.
	 */
	public static final class Requests {
		private final List<Request> requests = new ArrayList<>();

		/** Adds {@code request}; returns the one it replaced (from the same player to the same player), or null. */
		public Request add(Request request) {
			Request replaced = null;
			for (Iterator<Request> it = requests.iterator(); it.hasNext(); ) {
				Request r = it.next();
				if (r.from().equals(request.from()) && r.to().equals(request.to())) {
					replaced = r;
					it.remove();
				}
			}
			requests.add(request);
			return replaced;
		}

		/** The requests waiting for {@code to} to answer, newest first. */
		public List<Request> waitingFor(UUID to) {
			List<Request> found = new ArrayList<>();
			for (Request r : requests) {
				if (r.to().equals(to)) {
					found.addFirst(r);
				}
			}
			return found;
		}

		/** The requests {@code from} has out, oldest first. */
		public List<Request> sentBy(UUID from) {
			return requests.stream().filter(r -> r.from().equals(from)).toList();
		}

		/**
		 * Takes the request {@code to} is answering: the one from {@code from}, or with {@code from} null
		 * the newest one. Null if there's no such request.
		 */
		public Request take(UUID to, UUID from) {
			Request found = null;
			for (Request r : requests) {
				if (r.to().equals(to) && (from == null || r.from().equals(from))) {
					found = r;
				}
			}
			if (found != null) {
				requests.remove(found);
			}
			return found;
		}

		/** Takes back every request {@code from} has out. */
		public List<Request> cancel(UUID from) {
			return removeIf(r -> r.from().equals(from));
		}

		/** Removes every request that has waited {@code timeout} ticks, and returns them. */
		public List<Request> expire(long now, long timeout) {
			return removeIf(r -> r.expired(now, timeout));
		}

		/** Removes every request to or from {@code player} (they left, or stopped taking requests). */
		public List<Request> forget(UUID player) {
			return removeIf(r -> r.from().equals(player) || r.to().equals(player));
		}

		/** Removes every request waiting for {@code to}. */
		public List<Request> refuseAll(UUID to) {
			return removeIf(r -> r.to().equals(to));
		}

		public boolean isEmpty() {
			return requests.isEmpty();
		}

		public void clear() {
			requests.clear();
		}

		private List<Request> removeIf(java.util.function.Predicate<Request> test) {
			List<Request> removed = new ArrayList<>();
			for (Iterator<Request> it = requests.iterator(); it.hasNext(); ) {
				Request r = it.next();
				if (test.test(r)) {
					removed.add(r);
					it.remove();
				}
			}
			return removed;
		}
	}
}
