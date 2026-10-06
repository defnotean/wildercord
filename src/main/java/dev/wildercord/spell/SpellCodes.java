package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spell codes: a whole spell as one short, readable word that can be pasted into chat, a
 * forum post or a Discord message, e.g. {@code wc:bolt.frost.split}. Built-in runes are written
 * by their path; add-on runes as {@code namespace~path}. Pasting a code into chat shows
 * everyone the spell's name and readout; the Cord screen can load one into a spell.
 */
public final class SpellCodes {
	private SpellCodes() {}

	public static final String PREFIX = "wc:";
	/** A code inside other text. */
	public static final Pattern PATTERN = Pattern.compile("\\bwc:([a-z0-9_~:/.-]+)");
	private static final int MAX_CODE_LENGTH = 32 * (Knots.MAX_ID_LENGTH + 1);

	public static String encode(List<String> runeIds) {
		StringBuilder out = new StringBuilder(PREFIX);
		for (int i = 0; i < runeIds.size(); i++) {
			if (i > 0) {
				out.append('.');
			}
			String id = runeIds.get(i);
			out.append(id.startsWith("wildercord:") ? id.substring("wildercord:".length()) : id.replace(':', '~'));
		}
		return out.toString();
	}

	/** The rune ids in a code (with or without its {@code wc:} prefix); unknown runes are kept as ids. */
	public static List<String> decode(String code) {
		if (code.length() > MAX_CODE_LENGTH) return List.of(RelayRules.ID);
		String body = code.trim();
		if (body.startsWith(PREFIX)) body = body.substring(PREFIX.length());
		List<String> ids = new ArrayList<>();
		if (body.isEmpty()) return ids;
		for (String part : body.split("\\.")) {
			if (!part.isEmpty()) ids.add(part.contains("~") ? part.replace('~', ':') : "wildercord:" + part);
		}
		return ReweaveRules.boundedIds(ids, 12);
	}

	/** The first code in a piece of text, or null. */
	public static String find(String text) {
		Matcher m = PATTERN.matcher(text);
		return m.find() ? m.group() : null;
	}
}
