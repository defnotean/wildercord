package dev.wildercord.cast;

import dev.wildercord.aura.arts.ReleasedArtOwner;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A synchronous user-action admission cannot be re-entered by its validation or payment callbacks. */
public final class ActionAdmission implements AutoCloseable {
	private static final Map<UUID, ActionAdmission> ACTIVE = new HashMap<>();
	private final ServerPlayer player;
	private final ReleasedArtOwner owner;
	private ActionAdmission(ServerPlayer player) { this.player = player; this.owner = ReleasedArtOwner.capture(player); }
	public static boolean busy(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
	public static ActionAdmission begin(ServerPlayer player) {
		if (busy(player)) return null;
		ActionAdmission admission = new ActionAdmission(player);
		if (!admission.owner.valid()) return null;
		ACTIVE.put(player.getUUID(), admission);
		return admission;
	}
	/** A lifecycle callback retires this receipt even if the same body returns to the same world immediately. */
	public boolean valid() { return ACTIVE.get(player.getUUID()) == this && owner.valid(); }
	@Override public void close() { ACTIVE.remove(player.getUUID(), this); }
}
