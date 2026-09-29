package dev.wildercord.travel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A player's travel state, kept in the {@code wildercord:travel} attachment (saved, and kept through
 * death): their homes and waypoints by name, where {@code /back} goes, whether they've switched
 * teleport requests off, and the waypoint they're tracking ("" for none). Immutable: every change
 * returns a new one, which is what makes the attachment save it.
 *
 * @param homes       homes by name, in name order
 * @param waypoints   waypoints by name, in name order
 * @param back        where the last teleport left from, or where they last died
 * @param requestsOff whether {@code /tptoggle} has turned teleport requests away
 * @param tracked     the tracked waypoint's name, or ""
 */
public record TravelData(Map<String, Spot> homes, Map<String, Spot> waypoints, Optional<Spot> back, boolean requestsOff, String tracked) {
	public static final TravelData EMPTY = new TravelData(Map.of(), Map.of(), Optional.empty(), false, "");

	// Every field is optional, so data saved by an older version (or with a field added later) still reads.
	public static final Codec<TravelData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Spot.CODEC).optionalFieldOf("homes", Map.of()).forGetter(TravelData::homes),
		Codec.unboundedMap(Codec.STRING, Spot.CODEC).optionalFieldOf("waypoints", Map.of()).forGetter(TravelData::waypoints),
		Spot.CODEC.optionalFieldOf("back").forGetter(TravelData::back),
		Codec.BOOL.optionalFieldOf("requests_off", false).forGetter(TravelData::requestsOff),
		Codec.STRING.optionalFieldOf("tracked", "").forGetter(TravelData::tracked)
	).apply(i, TravelData::new));

	public TravelData {
		homes = Collections.unmodifiableMap(new TreeMap<>(homes));
		waypoints = Collections.unmodifiableMap(new TreeMap<>(waypoints));
		tracked = tracked == null ? "" : tracked;
	}

	public TravelData withHome(String name, Spot spot) {
		return new TravelData(put(homes, name, spot), waypoints, back, requestsOff, tracked);
	}

	public TravelData withoutHome(String name) {
		return new TravelData(remove(homes, name), waypoints, back, requestsOff, tracked);
	}

	public TravelData withWaypoint(String name, Spot spot) {
		return new TravelData(homes, put(waypoints, name, spot), back, requestsOff, tracked);
	}

	/** Removes a waypoint, and stops tracking it if it was tracked. */
	public TravelData withoutWaypoint(String name) {
		return new TravelData(homes, remove(waypoints, name), back, requestsOff, tracked.equals(name) ? "" : tracked);
	}

	public TravelData withBack(Spot spot) {
		return new TravelData(homes, waypoints, Optional.of(spot), requestsOff, tracked);
	}

	public TravelData withRequestsOff(boolean off) {
		return new TravelData(homes, waypoints, back, off, tracked);
	}

	/** Tracks the waypoint {@code name} ("" for none). */
	public TravelData withTracked(String name) {
		return new TravelData(homes, waypoints, back, requestsOff, name);
	}

	/** The tracked waypoint, if there is one. */
	public Optional<Spot> trackedSpot() {
		return tracked.isEmpty() ? Optional.empty() : Optional.ofNullable(waypoints.get(tracked));
	}

	private static Map<String, Spot> put(Map<String, Spot> map, String name, Spot spot) {
		Map<String, Spot> copy = new TreeMap<>(map);
		copy.put(name, spot);
		return copy;
	}

	private static Map<String, Spot> remove(Map<String, Spot> map, String name) {
		Map<String, Spot> copy = new TreeMap<>(map);
		copy.remove(name);
		return copy;
	}
}
