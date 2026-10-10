package dev.wildercord.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The stats page (0.13): one look at how far a player has come on both roads and in the world, from {@code /stats}. A
 * {@link Snapshot} is read off the player by {@link Stats} and turned into rows here, so what is shown and how numbers are
 * written can be unit tested. A road not started (no circle, no breathing method) is left off rather than shown as zero.
 */
public final class StatsRules {
	private StatsRules() {}

	public static final int TICKS_PER_MINUTE = 20 * 60;

	/**
	 * Everything the page shows.
	 *
	 * @param playTicks   ticks played in this world (the vanilla statistic)
	 * @param circles     Heart Circles formed, 0 to 20
	 * @param ascension   ascensions past the 20th
	 * @param auraStage   the Aura stage's index, -1 without a breathing method
	 * @param method      the breathing method's id, or empty
	 * @param duelsWon    duelists beaten, by method
	 * @param mastersWon  Sword Master trials won
	 * @param feats       feats earned
	 * @param met         field-guide creatures met
	 * @param guide       creatures in the field guide
	 * @param slain       field-guide creatures slain, all kinds together
	 * @param mastered    kinds mastered in the codex
	 * @param deaths      deaths in this world
	 */
	public record Snapshot(long playTicks, int circles, int ascension, int auraStage, String method, int duelsWon, int mastersWon,
		int feats, int met, int guide, int slain, int mastered, int deaths) {}

	/**
	 * One row: its language key's last part ({@code stats.wildercord.<key>}) and the values it's filled with.
	 */
	public record Row(String key, List<Object> args) {
		public Row(String key, Object... args) {
			this(key, List.of(args));
		}
	}

	/** {@code 12h 05m}, or {@code 42m} under an hour. */
	public static String playTime(long ticks) {
		long minutes = Math.max(0, ticks) / TICKS_PER_MINUTE;
		return minutes < 60 ? minutes + "m" : String.format(Locale.ROOT, "%dh %02dm", minutes / 60, minutes % 60);
	}

	/** {@code part} of {@code whole} as a whole percentage, 0 when there's nothing to have. */
	public static int percent(int part, int whole) {
		return whole <= 0 ? 0 : (int) Math.min(100, Math.round(100.0 * Math.max(0, part) / whole));
	}

	public static List<Row> rows(Snapshot s) {
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("play_time", playTime(s.playTicks())));
		if (s.circles() > 0) {
			rows.add(s.ascension() > 0 ? new Row("circles_ascended", s.circles(), s.ascension()) : new Row("circles", s.circles()));
		}
		if (s.auraStage() >= 0 && !s.method().isEmpty()) {
			rows.add(new Row("aura", s.method(), s.auraStage()));
		}
		if (s.duelsWon() > 0 || s.mastersWon() > 0) {
			rows.add(new Row("duels", s.duelsWon(), s.mastersWon()));
		}
		rows.add(new Row("field_guide", s.met(), s.guide(), percent(s.met(), s.guide())));
		if (s.slain() > 0) {
			rows.add(new Row("codex", s.slain(), s.mastered()));
		}
		rows.add(new Row("feats", s.feats()));
		rows.add(new Row("deaths", s.deaths()));
		return rows;
	}
}
