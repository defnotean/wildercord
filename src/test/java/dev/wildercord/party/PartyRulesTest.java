package dev.wildercord.party;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static dev.wildercord.party.PartyRules.Result.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real membership state transitions, including replay, authority, lifecycle and bounded state. */
class PartyRulesTest {
	private static final UUID A = new UUID(0, 1);
	private static final UUID B = new UUID(0, 2);
	private static final UUID C = new UUID(0, 3);
	private static final UUID D = new UUID(0, 4);

	@Test
	void anInvitationNeverSilentlyJoinsItsRecipient() {
		PartyRules rules = new PartyRules();
		assertEquals(OK, rules.invite(A, B, 0));
		assertEquals(List.of(A), rules.party(A).members());
		assertNull(rules.party(B));
		assertFalse(rules.sameParty(A, B));
		assertEquals(NO_INVITE, rules.accept(C, A, 1));
		assertEquals(NO_INVITE, rules.accept(B, C, 1));
		assertEquals(OK, rules.accept(B, A, 1));
		assertTrue(rules.sameParty(A, B));
		assertEquals(List.of(A, B), rules.party(A).members());
		assertEquals(ALREADY_GROUPED, rules.accept(B, A, 2));
	}

	@Test
	void selfAndNonLeaderInvitesCannotChangeMembership() {
		PartyRules rules = pair();
		assertEquals(SELF, rules.invite(C, C, 30));
		assertNull(rules.party(C));
		assertEquals(NOT_LEADER, rules.invite(B, C, 30));
		assertNull(rules.party(C));
		assertEquals(ALREADY_GROUPED, rules.invite(C, B, 30));
		assertNull(rules.party(C), "a refused invitation cannot manufacture a party");
	}

	@Test
	void invitationsExpireAtTheBoundaryAndCannotReviveAfterClockRollback() {
		PartyRules rules = new PartyRules();
		rules.invite(A, B, 100);
		assertEquals(OK, rules.accept(B, A, 100 + PartyRules.INVITE_TICKS - 1));
		rules.invite(C, D, 100);
		assertEquals(NO_INVITE, rules.accept(D, C, 100 + PartyRules.INVITE_TICKS));
		rules.invite(C, D, 1500);
		assertEquals(NO_INVITE, rules.accept(D, C, 1499));
	}

	@Test
	void decliningConsumesOnlyThatInvitation() {
		PartyRules rules = new PartyRules();
		rules.invite(A, C, 0);
		rules.invite(B, C, 0);
		assertEquals(OK, rules.decline(C, A, 1));
		assertEquals(NO_INVITE, rules.accept(C, A, 1));
		assertEquals(OK, rules.accept(C, B, 1));
	}

	@Test
	void acceptingOnePartyInvalidatesOtherInvitationsEvenAfterLeaving() {
		PartyRules rules = new PartyRules();
		rules.invite(A, C, 0);
		rules.invite(B, C, 0);
		assertEquals(OK, rules.accept(C, A, 1));
		assertEquals(ALREADY_GROUPED, rules.accept(C, B, 2));
		rules.leave(C);
		assertEquals(NO_INVITE, rules.accept(C, B, 3));
		assertNull(rules.party(C));
	}

	@Test
	void disbandedInvitationCannotJoinARecreatedPartyWithTheSameLeader() {
		PartyRules rules = new PartyRules();
		rules.invite(A, B, 0);
		UUID old = rules.party(A).id();
		assertEquals(OK, rules.disband(A));
		rules.invite(A, C, 30);
		assertNotEquals(old, rules.party(A).id());
		assertEquals(NO_INVITE, rules.accept(B, A, 31));
		assertEquals(OK, rules.accept(C, A, 31));
	}

	@Test
	void leaderLeaveTransfersToEarliestMemberAndCancelsTheirOldInvitations() {
		PartyRules rules = pair();
		rules.invite(A, C, 30);
		rules.accept(C, A, 31);
		rules.invite(A, D, 60);
		UUID id = rules.party(A).id();
		assertEquals(OK, rules.leave(A));
		assertNull(rules.party(A));
		assertEquals(B, rules.party(B).leader());
		assertEquals(id, rules.party(B).id());
		assertEquals(List.of(B, C), rules.party(B).members());
		assertEquals(NO_INVITE, rules.accept(D, A, 61));
		assertEquals(OK, rules.invite(B, D, 61));
		assertEquals(OK, rules.accept(D, B, 62));
	}

	@Test
	void nonLeadersCannotKickOrDisbandAndLeaderCannotKickSelf() {
		PartyRules rules = pair();
		assertEquals(NOT_LEADER, rules.kick(B, A));
		assertEquals(NOT_LEADER, rules.disband(B));
		assertEquals(SELF, rules.kick(A, A));
		assertEquals(NOT_MEMBER, rules.kick(A, C));
		assertEquals(OK, rules.kick(A, B));
		assertFalse(rules.sameParty(A, B));
		assertNull(rules.party(B));
		assertEquals(NO_INVITE, rules.accept(B, A, 10));
	}

	@Test
	void disbandRemovesEveryMemberAndInvitation() {
		PartyRules rules = pair();
		rules.invite(A, C, 30);
		assertEquals(OK, rules.disband(A));
		assertNull(rules.party(A));
		assertNull(rules.party(B));
		assertEquals(NO_INVITE, rules.accept(C, A, 31));
		assertEquals(NO_PARTY, rules.disband(A));
	}

	@Test
	void disconnectKeepsMembershipButCancelsBothDirectionsOfInvitation() {
		PartyRules rules = pair();
		rules.invite(A, C, 30);
		rules.disconnect(A);
		assertTrue(rules.sameParty(A, B));
		assertEquals(A, rules.party(B).leader());
		assertEquals(NO_INVITE, rules.accept(C, A, 31));
		rules.invite(A, C, 60);
		rules.disconnect(C);
		assertEquals(NO_INVITE, rules.accept(C, A, 61));
	}

	@Test
	void leaveLastMemberAndServerStopClearTheSession() {
		PartyRules rules = new PartyRules();
		rules.invite(A, B, 0);
		rules.leave(A);
		assertNull(rules.party(A));
		assertEquals(NO_INVITE, rules.accept(B, A, 1));
		rules.invite(A, B, 30);
		rules.accept(B, A, 31);
		rules.clear();
		assertNull(rules.party(A));
		assertNull(rules.party(B));
		assertFalse(rules.sameParty(A, B));
		assertEquals(NO_INVITE, rules.accept(B, A, 32));
	}

	@Test
	void twoServersDoNotShareMembership() {
		PartyRules first = pair();
		PartyRules second = new PartyRules();
		assertTrue(first.sameParty(A, B));
		assertFalse(second.sameParty(A, B));
		assertNull(second.party(A));
	}

	@Test
	void invitationCapacityAndAcceptanceCapacityAreBothBounded() {
		PartyRules rules = new PartyRules();
		List<UUID> invitees = new ArrayList<>();
		for (int i = 0; i < PartyRules.MAX_MEMBERS; i++) {
			UUID member = new UUID(1, i);
			invitees.add(member);
			assertEquals(OK, rules.invite(A, member, i * 20));
		}
		for (int i = 0; i < PartyRules.MAX_MEMBERS - 1; i++) {
			assertEquals(OK, rules.accept(invitees.get(i), A, 200));
		}
		assertEquals(FULL, rules.accept(invitees.getLast(), A, 200));
		assertNull(rules.party(invitees.getLast()));
		assertEquals(FULL, rules.invite(A, B, 220));
		assertEquals(PartyRules.MAX_MEMBERS, rules.party(A).members().size());
	}

	@Test
	void outgoingInvitationsAndIncomingInvitationsCannotGrowWithoutBound() {
		PartyRules sent = new PartyRules();
		for (int i = 0; i < PartyRules.MAX_PENDING_INVITES; i++) assertEquals(OK, sent.invite(A, new UUID(1, i), i * 20));
		assertEquals(INVITE_LIMIT, sent.invite(A, B, 200));
		assertEquals(OK, sent.invite(A, B, PartyRules.INVITE_TICKS + 200));
		PartyRules received = new PartyRules();
		for (int i = 0; i < PartyRules.MAX_PENDING_INVITES; i++) assertEquals(OK, received.invite(new UUID(1, i), B, 0));
		assertEquals(INVITE_LIMIT, received.invite(A, B, 0));
		assertNull(received.party(A));
	}

	@Test
	void cooldownPreventsInvitationSpamWithoutConsumingExistingInvitation() {
		PartyRules rules = new PartyRules();
		assertEquals(OK, rules.invite(A, B, 0));
		assertEquals(WAIT, rules.invite(A, C, PartyRules.INVITE_COOLDOWN_TICKS - 1));
		assertEquals(OK, rules.accept(B, A, 1));
		assertEquals(OK, rules.invite(A, C, PartyRules.INVITE_COOLDOWN_TICKS));
	}

	@Test
	void snapshotsCannotMutateAuthoritativeMembership() {
		PartyRules rules = pair();
		PartyRules.Party before = rules.party(A);
		assertThrows(UnsupportedOperationException.class, () -> before.members().add(C));
		rules.leave(B);
		assertEquals(List.of(A, B), before.members(), "snapshots remain stable");
		assertEquals(List.of(A), rules.party(A).members());
	}

	@Test
	void protectionExemptsSelfAndExplicitFightingDuelsButNothingElse() {
		assertTrue(PartyRules.blocksHarm(false, true, null));
		assertTrue(PartyRules.blocksHarm(false, true, false), "countdown or duel exclusion remains safe");
		assertFalse(PartyRules.blocksHarm(false, true, true), "explicit fighting opponents override party immunity");
		assertFalse(PartyRules.blocksHarm(true, true, null), "self costs are not refunded by party membership");
		assertFalse(PartyRules.blocksHarm(false, false, null), "party does not change strangers' existing PvP/team rules");
	}

	@Test
	void disconnectedProjectileOwnerKeepsUuidProtectionWithoutAPlayerEntity() {
		PartyRules rules = pair();
		rules.disconnect(A);
		assertTrue(rules.blocksHarm(A, B, null), "a saved projectile owner UUID is still a party member");
		assertFalse(rules.blocksHarm(A, A, null), "the shooter's own identity retains the self exception");
		assertFalse(rules.blocksHarm(C, B, null), "an unrelated offline shooter gains no immunity");
		assertFalse(rules.blocksHarm(null, B, null), "ownerless environmental damage is not attributed");
		assertFalse(rules.blocksHarm(A, B, true), "an explicit active duel still takes precedence");
		assertTrue(rules.blocksHarm(A, B, false), "countdown protection remains intact");
		rules.leave(A);
		assertFalse(rules.blocksHarm(A, B, null), "an old arrow does not retain membership after leaving");
	}

	@Test
	void arbitraryCommandSequencesKeepMembershipUniqueAndLeaderInsideParty() {
		PartyRules rules = new PartyRules();
		List<UUID> players = java.util.stream.IntStream.range(0, 24).mapToObj(i -> new UUID(3, i)).toList();
		Random random = new Random(9182);
		for (int step = 0; step < 10_000; step++) {
			UUID actor = players.get(random.nextInt(players.size())), other = players.get(random.nextInt(players.size()));
			long now = step * 20L;
			switch (random.nextInt(8)) {
				case 0 -> rules.invite(actor, other, now);
				case 1 -> rules.accept(actor, other, now);
				case 2 -> rules.decline(actor, other, now);
				case 3 -> rules.leave(actor);
				case 4 -> rules.kick(actor, other);
				case 5 -> rules.disband(actor);
				case 6 -> rules.disconnect(actor);
				case 7 -> rules.prune(now);
			}
			for (UUID member : players) {
				PartyRules.Party party = rules.party(member);
				if (party == null) continue;
				assertTrue(party.members().contains(member));
				assertTrue(party.members().contains(party.leader()));
				assertTrue(party.members().size() <= PartyRules.MAX_MEMBERS);
				assertEquals(party.members().size(), party.members().stream().distinct().count());
				for (UUID peer : party.members()) assertEquals(party, rules.party(peer));
			}
		}
	}

	private static PartyRules pair() {
		PartyRules rules = new PartyRules();
		assertEquals(OK, rules.invite(A, B, 0));
		assertEquals(OK, rules.accept(B, A, 1));
		return rules;
	}
}
