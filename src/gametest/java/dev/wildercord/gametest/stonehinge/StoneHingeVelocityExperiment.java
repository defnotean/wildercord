package dev.wildercord.gametest.stonehinge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Explicit GameTest experiment, never an equipped form or damage listener. A complete genuine attack is captured
 * before one optional horizontal deflection. It does not pay, arm, move a body, send a packet, or change sync flags.
 */
public final class StoneHingeVelocityExperiment {
	private StoneHingeVelocityExperiment() {}
	public enum Decision { APPLIED, AIRBORNE, RECEIPT_REFUSED, STATE_CHANGED, DUPLICATE }
	public record State(Vec3 position, Vec3 motion, double fall, boolean grounded, float health, float absorption,
		boolean needsSync, boolean syncVelocity) {
		public static State of(ServerPlayer player) {
			return new State(player.position(), player.getDeltaMovement(), player.fallDistance, player.onGround(),
				player.getHealth(), player.getAbsorptionAmount(), player.needsSync, player.syncVelocity);
		}
	}
	public static final class Strike {
		private final ServerPlayer player;
		private final State before, nativeOutcome;
		private final StoneHingeImpulseProbe.Trial receipt;
		private boolean attempted;
		private Strike(ServerPlayer player, State before, StoneHingeImpulseProbe.Trial receipt) {
			this.player = player; this.before = before; this.receipt = receipt; nativeOutcome = State.of(player);
		}
		public State before() { return before; }
		public State nativeOutcome() { return nativeOutcome; }
		public StoneHingeImpulseProbe.Trial receipt() { return receipt; }
		/** Sign selects a quarter-turn of the measured impulse, not a displacement or new speed. */
		public Decision turn(int sign) {
			if (sign != -1 && sign != 1) throw new IllegalArgumentException("One left or right direction required");
			if (!player.level().getServer().isSameThread()) throw new IllegalStateException("Server-thread experiment only");
			if (attempted) return Decision.DUPLICATE;
			attempted = true;
			if (!before.grounded || !nativeOutcome.grounded) return Decision.AIRBORNE;
			if (!nativeOutcome.equals(State.of(player))) return Decision.STATE_CHANGED;
			if (!receipt.eligibleReceipt()) return Decision.RECEIPT_REFUSED;
			var impulse = receipt.impulses().getFirst();
			double retainedX = impulse.before().x * .5, retainedZ = impulse.before().z * .5;
			double nativeX = impulse.after().x - retainedX, nativeZ = impulse.after().z - retainedZ;
			Vec3 turned = new Vec3(retainedX + sign * nativeZ, player.getDeltaMovement().y, retainedZ - sign * nativeX);
			if (!Double.isFinite(turned.x) || !Double.isFinite(turned.y) || !Double.isFinite(turned.z)) return Decision.STATE_CHANGED;
			// The native tracker retains responsibility for its original dispatch, including the native current Y.
			player.setDeltaMovement(turned);
			return Decision.APPLIED;
		}
	}
	/** The caller supplies the complete real attack, including any caller-side enchanted knockback after hurtServer. */
	public static Strike capture(ServerPlayer player, Runnable nativeAttack) {
		if (!player.level().getServer().isSameThread()) throw new IllegalStateException("Server-thread experiment only");
		State before = State.of(player);
		return new Strike(player, before, StoneHingeImpulseProbe.capture(player, nativeAttack));
	}
}
