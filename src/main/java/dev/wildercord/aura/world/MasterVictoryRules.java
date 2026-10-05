package dev.wildercord.aura.world;

import dev.wildercord.aura.TechniqueRules;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Bounded first-clear progress for the three prototype schools; repeated wins never mint consumable rewards. */
public final class MasterVictoryRules {
	private MasterVictoryRules() {}

	public static final int ALL = 0b111;

	/** Only three known clear bits are stored, so corrupt or future bits cannot grant an existing school's reward. */
	public record Progress(int schools) {
		public static final Progress NONE = new Progress(0);

		public Progress { schools &= ALL; }

		public boolean cleared(int school) {
			int flag = bit(school);
			return flag != 0 && (schools & flag) != 0;
		}

		public Progress withClear(int school) {
			return new Progress(schools | bit(school));
		}
	}

	private static int bit(int school) {
		return school >= MastersRules.EMBER && school <= MastersRules.STONE ? 1 << school : 0;
	}

	/** Existing horizontal technique parts; learning one never advances an Aura stage or unlocks a foundational art. */
	public static String reward(int school) {
		return switch (school) {
			case MastersRules.EMBER -> TechniqueRules.ECHO;
			case MastersRules.GALE -> TechniqueRules.AFTERIMAGE;
			case MastersRules.STONE -> TechniqueRules.SUNDER;
			default -> "";
		};
	}

	public static boolean legitimate(boolean started, boolean activeAi, boolean bypassKill, boolean enrolledFinisher) {
		return started && activeAi && !bypassKill && enrolledFinisher;
	}

	/** Living, present opt-ins share a clear, including support players. Being nearby or merely in the same party is insufficient. */
	public static Set<UUID> credit(Set<UUID> enrolled, Set<UUID> present) {
		Set<UUID> result = new LinkedHashSet<>();
		for (UUID id : enrolled) {
			if (result.size() >= MastersRules.MAX_PARTICIPANTS) break;
			if (present.contains(id)) result.add(id);
		}
		return Set.copyOf(result);
	}
}
