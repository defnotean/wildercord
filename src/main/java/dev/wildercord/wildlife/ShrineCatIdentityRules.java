package dev.wildercord.wildlife;


import java.util.UUID;

/**
 * Pure identity and deduplication rules for the four Shrine Cats ({@code Yuki}, {@code Odie}, {@code Freya}, {@code Kuro}).
 *
 * <p>Each player owns at most one active companion per {@link ShrineCatRules.Variant}. Sitting, loafing,
 * unloaded-chunk, or cross-dimension cats remain active and never authorize spawning a duplicate body.</p>
 */
public final class ShrineCatIdentityRules {
	public enum LoadResolution {
		REGISTER_NEW,
		KEEP_CANONICAL,
		ADOPT_TRANSFER,
		DISCARD_DUPLICATE
	}

	private ShrineCatIdentityRules() {}

	/** Whether {@code owner} may tame a new cat of {@code variant} given any journaled identity. */
	public static boolean canTameVariant(UUID canonicalEntityId, boolean retired) {
		return canonicalEntityId == null || retired;
	}

	/** Decides how a loaded tamed {@code ShrineCat} body reconciles against the durable journal record. */
	public static LoadResolution resolveLoadedBody(UUID bodyId, UUID canonicalEntityId,
			boolean retired, boolean transferring) {
		if (bodyId == null) {
			return LoadResolution.DISCARD_DUPLICATE;
		}
		if (transferring) {
			return LoadResolution.ADOPT_TRANSFER;
		}
		if (canonicalEntityId == null || retired) {
			return LoadResolution.REGISTER_NEW;
		}
		if (bodyId.equals(canonicalEntityId)) {
			return LoadResolution.KEEP_CANONICAL;
		}
		return LoadResolution.DISCARD_DUPLICATE;
	}

	/** Only the currently active canonical body may retire its journal entry when killed or discarded. */
	public static boolean mayRetire(UUID bodyId, UUID canonicalEntityId, boolean retired) {
		return !retired && bodyId != null && canonicalEntityId != null && canonicalEntityId.equals(bodyId);
	}

	/** Ringing the Suzu Cat-Bell Charm may respawn a variant only after its canonical body genuinely died. */
	public static boolean shouldRespawnOnBell(UUID canonicalEntityId, boolean retired) {
		return canonicalEntityId != null && retired;
	}
}
