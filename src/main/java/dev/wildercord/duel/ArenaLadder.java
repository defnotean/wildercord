package dev.wildercord.duel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The arena's ladder, with the overworld's saved data: every caster who has fought a ranked bout, their rating, wins and
 * losses this season. Kept here rather than on the players so the ladder can name those who are away.
 */
public final class ArenaLadder extends SavedData {
	/** One caster's standing: their name as last seen, rating, this season's wins and losses, and the season it was last brought up to. */
	public record Standing(String name, int rating, int wins, int losses, int season) {
		static final Codec<Standing> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("name", "").forGetter(Standing::name),
			Codec.INT.optionalFieldOf("rating", ArenaRules.START).forGetter(Standing::rating),
			Codec.INT.optionalFieldOf("wins", 0).forGetter(Standing::wins),
			Codec.INT.optionalFieldOf("losses", 0).forGetter(Standing::losses),
			Codec.INT.optionalFieldOf("season", 0).forGetter(Standing::season)
		).apply(i, Standing::new));

		/** This standing as it stands in {@code season}: an older one is carried over, its wins and losses cleared. */
		public Standing in(int season) {
			if (season <= this.season) return this;
			return new Standing(name, ArenaRules.carried(rating, season - this.season), 0, 0, season);
		}
	}

	static final Codec<ArenaLadder> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Standing.CODEC).optionalFieldOf("standings", Map.of()).forGetter(l -> l.standings)
	).apply(i, ArenaLadder::new));

	static final SavedDataType<ArenaLadder> TYPE = new SavedDataType<>(Wildercord.id("arena_ladder"), ArenaLadder::new, CODEC, null);

	private final Map<UUID, Standing> standings;

	public ArenaLadder() {
		this(Map.of());
	}

	private ArenaLadder(Map<UUID, Standing> standings) {
		this.standings = new HashMap<>(standings);
	}

	public static ArenaLadder of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** A caster's standing in {@code season} (a fresh one if they've never fought). */
	public Standing standing(UUID id, String name, int season) {
		Standing standing = standings.get(id);
		return standing == null ? new Standing(name, ArenaRules.START, 0, 0, season) : standing.in(season);
	}

	public void put(UUID id, Standing standing) {
		standings.put(id, standing);
		setDirty();
	}

	/** The best this season, best first: those who've fought this season, by rating. */
	public List<Standing> top(int season, int count) {
		return standings.values().stream().map(s -> s.in(season)).filter(s -> s.wins() + s.losses() > 0)
			.sorted(Comparator.comparingInt(Standing::rating).reversed().thenComparing(Standing::name)).limit(count).toList();
	}

	/** Forgets a caster (the game tests clean up after themselves). */
	public void forget(UUID id) {
		if (standings.remove(id) != null) setDirty();
	}
}
