package dev.wildercord.party;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** GameTest-only party setup hook without widening production package visibility. */
public final class PartyTestHooks {
	private PartyTestHooks() {}

	public static void join(MinecraftServer server, UUID leader, UUID member, long now) {
		PartyRules rules = Parties.session(server).rules;
		PartyRules.Result invited = rules.invite(leader, member, now);
		if (invited != PartyRules.Result.OK) {
			throw new AssertionError("Party invite failed: " + invited);
		}
		PartyRules.Result accepted = rules.accept(member, leader, now);
		if (accepted != PartyRules.Result.OK) {
			throw new AssertionError("Party accept failed: " + accepted);
		}
	}

	public static void clear(MinecraftServer server) {
		Parties.session(server).rules.clear();
	}
}
