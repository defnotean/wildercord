package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The world's record of masters and disciples, with the overworld's saved data: every bond that stands (a disciple has one master, so they're
 * kept by disciple), what each master is owed while they're away (a share of a road a disciple walked), when each last had a lesson, and the
 * disciples each master has seen through to their own stage (honoured on the Lineage tab). Kept here rather than on the players because
 * either side may be away when the other ends the bond, breaks through or graduates.
 */
public final class LineageRegistry extends SavedData {
	/**
	 * One bond: the master and the disciple (their names as last seen, and their stages as last seen for the page while one is away), when it
	 * was made and when the last lesson was (game time; {@link Long#MIN_VALUE} for none), and experience owed to the master.
	 */
	public record Bond(UUID master, String masterName, UUID disciple, String discipleName, long since, long lessonAt, double owed, int masterStage,
			int discipleStage, int masterColor, int discipleColor) {
		static final Codec<Bond> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("master").forGetter(Bond::master),
			Codec.STRING.optionalFieldOf("master_name", "").forGetter(Bond::masterName),
			UUIDUtil.CODEC.fieldOf("disciple").forGetter(Bond::disciple),
			Codec.STRING.optionalFieldOf("disciple_name", "").forGetter(Bond::discipleName),
			Codec.LONG.optionalFieldOf("since", 0L).forGetter(Bond::since),
			Codec.LONG.optionalFieldOf("lesson_at", Long.MIN_VALUE).forGetter(Bond::lessonAt),
			Codec.DOUBLE.optionalFieldOf("owed", 0.0).forGetter(Bond::owed),
			Codec.INT.optionalFieldOf("master_stage", 0).forGetter(Bond::masterStage),
			Codec.INT.optionalFieldOf("disciple_stage", 0).forGetter(Bond::discipleStage),
			Codec.INT.optionalFieldOf("master_color", 0).forGetter(Bond::masterColor),
			Codec.INT.optionalFieldOf("disciple_color", 0).forGetter(Bond::discipleColor)
		).apply(i, Bond::new));

		Bond lessoned(long now) {
			return new Bond(master, masterName, disciple, discipleName, since, now, owed, masterStage, discipleStage, masterColor, discipleColor);
		}

		Bond owing(double more) {
			return new Bond(master, masterName, disciple, discipleName, since, lessonAt, Math.max(0, owed + more), masterStage, discipleStage, masterColor,
				discipleColor);
		}

		Bond seenMaster(String name, int stage, int color) {
			return new Bond(master, name, disciple, discipleName, since, lessonAt, owed, stage, discipleStage, color, discipleColor);
		}

		Bond seenDisciple(String name, int stage, int color) {
			return new Bond(master, masterName, disciple, name, since, lessonAt, owed, masterStage, stage, masterColor, color);
		}
	}

	/** A disciple a master saw through to their own stage: who, when it was made and when they graduated (game time). */
	public record Honoured(UUID disciple, String name, long since, long graduated, int color) {
		static final Codec<Honoured> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("disciple").forGetter(Honoured::disciple),
			Codec.STRING.optionalFieldOf("name", "").forGetter(Honoured::name),
			Codec.LONG.optionalFieldOf("since", 0L).forGetter(Honoured::since),
			Codec.LONG.optionalFieldOf("graduated", 0L).forGetter(Honoured::graduated),
			Codec.INT.optionalFieldOf("color", 0).forGetter(Honoured::color)
		).apply(i, Honoured::new));
	}

	/** How many graduated disciples each master's record keeps (the latest). */
	public static final int HONOURED_KEPT = 8;

	static final Codec<LineageRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
		Bond.CODEC.listOf().optionalFieldOf("bonds", List.of()).forGetter(r -> List.copyOf(r.bonds.values())),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Honoured.CODEC.listOf()).optionalFieldOf("honoured", Map.of()).forGetter(r -> r.honoured),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.DOUBLE).optionalFieldOf("owed_after", Map.of()).forGetter(r -> r.owedAfter)
	).apply(i, LineageRegistry::new));
	static final SavedDataType<LineageRegistry> TYPE = new SavedDataType<>(Wildercord.id("lineage"), LineageRegistry::new, CODEC, null);

	/** Every standing bond, by disciple. */
	private final Map<UUID, Bond> bonds = new HashMap<>();
	private final Map<UUID, List<Honoured>> honoured = new HashMap<>();
	/** Experience owed to a master whose bond has since ended (a share earned just before it did), paid when they're next here. */
	private final Map<UUID, Double> owedAfter = new HashMap<>();

	public LineageRegistry() {
	}

	private LineageRegistry(List<Bond> bonds, Map<UUID, List<Honoured>> honoured, Map<UUID, Double> owedAfter) {
		for (Bond b : bonds) {
			this.bonds.put(b.disciple(), b);
		}
		honoured.forEach((master, list) -> this.honoured.put(master, new ArrayList<>(list)));
		this.owedAfter.putAll(owedAfter);
	}

	public static LineageRegistry of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** {@code disciple}'s bond with their master, if they have one. */
	public Optional<Bond> masterOf(UUID disciple) {
		return Optional.ofNullable(bonds.get(disciple));
	}

	/** Every disciple's bond with {@code master}, oldest first. */
	public List<Bond> disciplesOf(UUID master) {
		List<Bond> out = new ArrayList<>();
		for (Bond b : bonds.values()) {
			if (b.master().equals(master)) {
				out.add(b);
			}
		}
		out.sort(java.util.Comparator.comparingLong(Bond::since));
		return out;
	}

	/** Whether {@code disciple} is {@code master}'s disciple. */
	public boolean bonded(UUID master, UUID disciple) {
		Bond b = bonds.get(disciple);
		return b != null && b.master().equals(master);
	}

	/** The disciples {@code master} has seen through to their own stage, the latest last. */
	public List<Honoured> honoured(UUID master) {
		return List.copyOf(honoured.getOrDefault(master, List.of()));
	}

	void add(Bond bond) {
		bonds.put(bond.disciple(), bond);
		setDirty();
	}

	void put(Bond bond) {
		if (bonds.containsKey(bond.disciple())) {
			bonds.put(bond.disciple(), bond);
			setDirty();
		}
	}

	/** Ends {@code disciple}'s bond (what was owed to the master is kept for them). Returns it, if there was one. */
	Optional<Bond> end(UUID disciple) {
		Bond b = bonds.remove(disciple);
		if (b != null) {
			if (b.owed() > 0) {
				owedAfter.merge(b.master(), b.owed(), Double::sum);
			}
			setDirty();
		}
		return Optional.ofNullable(b);
	}

	void honour(UUID master, Honoured h) {
		List<Honoured> list = honoured.computeIfAbsent(master, k -> new ArrayList<>());
		list.add(h);
		while (list.size() > HONOURED_KEPT) {
			list.removeFirst();
		}
		setDirty();
	}

	/** Takes everything owed to {@code master} (on every bond and after), to pay it now. */
	double collect(UUID master) {
		double total = owedAfter.getOrDefault(master, 0.0);
		owedAfter.remove(master);
		for (Bond b : List.copyOf(bonds.values())) {
			if (b.master().equals(master) && b.owed() > 0) {
				total += b.owed();
				bonds.put(b.disciple(), b.owing(-b.owed()));
			}
		}
		if (total > 0) {
			setDirty();
		}
		return total;
	}

	/** Every standing bond (the game tests, and an operator's list). */
	public List<Bond> all() {
		return List.copyOf(bonds.values());
	}
}
