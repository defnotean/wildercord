package dev.wildercord.party;

import net.minecraft.server.level.ServerPlayer;

/** Package-local session access for the bounded Hailfall/Skyfall native acceptance fixture. */
public final class HailSkyPartySupport {
	private HailSkyPartySupport() {}
	public static void join(ServerPlayer owner, ServerPlayer guest) {
		var rules = Parties.session(owner.level().getServer()).rules;
		long now = owner.level().getGameTime();
		if (rules.invite(owner.getUUID(), guest.getUUID(), now) != PartyRules.Result.OK
			|| rules.accept(guest.getUUID(), owner.getUUID(), now) != PartyRules.Result.OK) {
			throw new AssertionError("The native late-recipient party invitation and consent must succeed");
		}
	}
	public static void leave(ServerPlayer player) { Parties.session(player.level().getServer()).rules.leave(player.getUUID()); }
}
