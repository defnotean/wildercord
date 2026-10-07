package dev.wildercord.pet;

import com.google.gson.JsonParser;
import java.util.UUID;

/** A complete validated configuration value; malformed reloads never partially change owner authority. */
record CinnamonConfig(String owner, UUID ownerId, boolean bell) {
	static final CinnamonConfig EMPTY = new CinnamonConfig("", null, true);
	static CinnamonConfig parse(String text) {
		var object = JsonParser.parseString(text).getAsJsonObject();
		var rawOwner = object.get("owner");
		if (rawOwner == null || !rawOwner.isJsonPrimitive() || !rawOwner.getAsJsonPrimitive().isString())
			throw new IllegalArgumentException("Cinnamon owner must be a username or UUID string");
		String owner = rawOwner.getAsString().trim();
		if (owner.length() > 64) throw new IllegalArgumentException("Cinnamon owner is too long");
		boolean bell = true;
		if (object.has("bell")) {
			var rawBell = object.get("bell");
			if (!rawBell.isJsonPrimitive() || !rawBell.getAsJsonPrimitive().isBoolean())
				throw new IllegalArgumentException("Cinnamon bell must be true or false");
			bell = rawBell.getAsBoolean();
		}
		UUID ownerId;
		try { ownerId = UUID.fromString(owner); } catch (IllegalArgumentException ignored) { ownerId = null; }
		return new CinnamonConfig(owner, ownerId, bell);
	}
}
