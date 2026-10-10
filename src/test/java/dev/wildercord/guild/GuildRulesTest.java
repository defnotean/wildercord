package dev.wildercord.guild;

import dev.wildercord.guild.GuildRules.Kind;
import dev.wildercord.guild.GuildRules.Result;
import dev.wildercord.guild.GuildRules.Roster;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildRulesTest {
	private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3);

	@Test
	void namesAreTidyAndUniqueWhateverTheirCase() {
		assertTrue(GuildRules.validName("The Iron Wake"));
		assertTrue(GuildRules.validName("Moon's-Eye 7"));
		assertFalse(GuildRules.validName("ab"));
		assertFalse(GuildRules.validName(" Lead"));
		assertFalse(GuildRules.validName("Two  Spaces"));
		assertFalse(GuildRules.validName("<b>bold</b>"));
		assertFalse(GuildRules.validName("x".repeat(GuildRules.NAME_MAX + 1)));
		Roster r = new Roster();
		assertEquals(Result.OK, r.found(A, Kind.GUILD, "Iron Wake"));
		assertEquals(Result.NAME_TAKEN, r.found(B, Kind.COVEN, "iron wake"));
	}

	@Test
	void oneOfEachKindAtMost() {
		Roster r = new Roster();
		assertEquals(Result.OK, r.found(A, Kind.GUILD, "Iron Wake"));
		assertEquals(Result.ALREADY_IN, r.found(A, Kind.GUILD, "Second Wake"));
		assertEquals(Result.OK, r.found(A, Kind.COVEN, "Ember Circle"));
		assertEquals("Iron Wake", r.of(A, Kind.GUILD).name());
		assertEquals("Ember Circle", r.of(A, Kind.COVEN).name());
	}

	@Test
	void onlyTheLeaderInvitesAndAGuildFillsUp() {
		Roster r = new Roster();
		r.found(A, Kind.GUILD, "Iron Wake");
		assertEquals(Result.SELF, r.canInvite(A, A, Kind.GUILD));
		assertEquals(Result.NOT_IN, r.canInvite(B, C, Kind.GUILD));
		assertEquals(Result.OK, r.canInvite(A, B, Kind.GUILD));
		assertEquals(Result.OK, r.join(B, "Iron Wake"));
		assertEquals(Result.NOT_LEADER, r.canInvite(B, C, Kind.GUILD));
		for (int i = 10; r.of(A, Kind.GUILD).members().size() < GuildRules.MAX_MEMBERS; i++) r.join(new UUID(0, i), "Iron Wake");
		assertEquals(Result.FULL, r.canInvite(A, C, Kind.GUILD));
		assertEquals(Result.FULL, r.join(C, "Iron Wake"));
	}

	@Test
	void aLeaderLeavingHandsItOnAndTheLastOneOutEndsIt() {
		Roster r = new Roster();
		r.found(A, Kind.COVEN, "Ember Circle");
		r.join(B, "Ember Circle");
		r.join(C, "Ember Circle");
		assertEquals(Result.OK, r.leave(A, Kind.COVEN));
		assertEquals(B, r.of(C, Kind.COVEN).leader());
		assertEquals(Result.NOT_LEADER, r.kick(C, B, Kind.COVEN));
		assertEquals(Result.OK, r.kick(B, C, Kind.COVEN));
		assertEquals(List.of(B), r.of(B, Kind.COVEN).members());
		r.leave(B, Kind.COVEN);
		assertNull(r.named("Ember Circle"));
	}

	@Test
	void disbandingIsTheLeadersAlone() {
		Roster r = new Roster();
		r.found(A, Kind.GUILD, "Iron Wake");
		r.join(B, "Iron Wake");
		assertEquals(Result.NOT_LEADER, r.disband(B, Kind.GUILD));
		assertEquals(Result.OK, r.disband(A, Kind.GUILD));
		assertNull(r.of(B, Kind.GUILD));
	}

	@Test
	void trainingTogetherIsAModestCappedBonus() {
		assertEquals(1.0, GuildRules.bonus(0), 1e-9);
		assertEquals(1.05, GuildRules.bonus(1), 1e-9);
		assertEquals(1.15, GuildRules.bonus(3), 1e-9);
		assertEquals(1.15, GuildRules.bonus(11), 1e-9);
		assertEquals(1.0, GuildRules.bonus(-2), 1e-9);
	}
}
