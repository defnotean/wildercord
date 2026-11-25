package dev.wildercord.pet;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** A complete validated configuration value; malformed reloads never partially change owner authority. */
record CinnamonConfig(List<OwnerSpec> owners, boolean bell) {
	record OwnerSpec(String raw, UUID id) {
		static OwnerSpec of(String raw) {
			String trimmed = raw.trim();
			if (trimmed.length() > 64) {
				throw new IllegalArgumentException("Cinnamon owner is too long");
			}
			UUID id;
			try { id = UUID.fromString(trimmed); } catch (IllegalArgumentException ignored) { id = null; }
			return new OwnerSpec(trimmed, id);
		}

		boolean matches(UUID playerId, String playerName, boolean isSingleplayerOwner) {
			if (raw.isBlank()) return false;
			if (raw.equalsIgnoreCase("@singleplayer")) return isSingleplayerOwner;
			return id != null ? id.equals(playerId) : raw.equalsIgnoreCase(playerName);
		}
	}

	static final CinnamonConfig EMPTY = new CinnamonConfig(List.of(), true);

	String owner() {
		return owners.isEmpty() ? "" : owners.getFirst().raw();
	}

	UUID ownerId() {
		return owners.isEmpty() ? null : owners.getFirst().id();
	}

	boolean matches(UUID playerId, String playerName, boolean isSingleplayerOwner) {
		for (OwnerSpec spec : owners) {
			if (spec.matches(playerId, playerName, isSingleplayerOwner)) return true;
		}
		return false;
	}

	static CinnamonConfig parse(String text) {
		var object = JsonParser.parseString(text).getAsJsonObject();
		boolean hasOwner = object.has("owner");
		boolean hasOwners = object.has("owners");
		if (!hasOwner && !hasOwners) {
			throw new IllegalArgumentException("Cinnamon config requires owner or owners");
		}
		List<OwnerSpec> specs = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		if (hasOwner) {
			var rawOwner = object.get("owner");
			if (rawOwner == null || !rawOwner.isJsonPrimitive() || !rawOwner.getAsJsonPrimitive().isString())
				throw new IllegalArgumentException("Cinnamon owner must be a username or UUID string");
			OwnerSpec spec = OwnerSpec.of(rawOwner.getAsString());
			if (!spec.raw().isBlank() && seen.add(spec.raw().toLowerCase(Locale.ROOT))) {
				specs.add(spec);
			}
		}
		if (hasOwners) {
			var rawOwners = object.get("owners");
			if (rawOwners == null || !rawOwners.isJsonArray())
				throw new IllegalArgumentException("Cinnamon owners must be an array of usernames or UUID strings");
			for (var el : rawOwners.getAsJsonArray()) {
				if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString())
					throw new IllegalArgumentException("Each Cinnamon owners entry must be a username or UUID string");
				OwnerSpec spec = OwnerSpec.of(el.getAsString());
				if (!spec.raw().isBlank() && seen.add(spec.raw().toLowerCase(Locale.ROOT))) {
					specs.add(spec);
				}
			}
		}
		if (specs.size() > CinnamonJournal.MAX_OWNERS) {
			throw new IllegalArgumentException("Cinnamon owners exceed maximum of " + CinnamonJournal.MAX_OWNERS);
		}
		boolean bell = true;
		if (object.has("bell")) {
			var rawBell = object.get("bell");
			if (!rawBell.isJsonPrimitive() || !rawBell.getAsJsonPrimitive().isBoolean())
				throw new IllegalArgumentException("Cinnamon bell must be true or false");
			bell = rawBell.getAsBoolean();
		}
		return new CinnamonConfig(List.copyOf(specs), bell);
	}
}
