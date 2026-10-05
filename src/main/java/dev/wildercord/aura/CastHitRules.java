package dev.wildercord.aura;

/** Decision after one synchronous resolved hit; this does not deal damage, cancel a spell or bypass immunity. */
public final class CastHitRules {
	private CastHitRules() {}
	public enum Response { NONE, INTERRUPT, SEAL_IDLE }

	/**
	 * Compare the exact pre-impact attachment, not its value or start tick. A damage callback which replaces
	 * a charge (even with equal fields) cannot transfer this hit's interrupt. No normal charge tick occurs
	 * inside a synchronous hit; a callback-created stage record is conservatively treated as a replacement.
	 */
	public static Response response(boolean damaging, Object before, Object after) {
		if (!damaging || before != after) return Response.NONE;
		return before == null ? Response.SEAL_IDLE : Response.INTERRUPT;
	}
}
