package dev.wildercord.party;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.wildercord.party.PartyRules.Result.*;
import static org.junit.jupiter.api.Assertions.*;

class PartySessionTest {
	private static final UUID A = new UUID(0, 1);
	private static final UUID B = new UUID(0, 2);
	private static final UUID C = new UUID(0, 3);
	private static final UUID D = new UUID(0, 4);

	@Test
	void unrelatedVisitorChurnIsCollectedAtDisconnectWithoutWaitingForATick() {
		PartySession state = pair();
		for (int i = 0; i < 2_000; i++) {
			UUID visitor = new UUID(1, i);
			state.remember(visitor, "Visitor" + i);
			state.disconnect(visitor);
			assertEquals(visitor.toString(), state.name(visitor));
		}
		Set<UUID> scanned = new HashSet<>();
		state.prune(100, player -> {
			scanned.add(player);
			return false;
		});
		assertEquals(Set.of(A, B), scanned, "departed visitors no longer occupy the name cache");
		assertEquals("Birch", state.name(B));
		assertTrue(state.rules.sameParty(A, B));
	}

	@Test
	void onlinePlayersKeepTheirNamesWithoutGainingPartyMembership() {
		PartySession state = new PartySession();
		state.remember(C, "Cedar");
		state.prune(100, C::equals);
		assertEquals("Cedar", state.name(C));
		assertNull(state.rules.party(C));
		state.prune(120, player -> false);
		assertEquals(C.toString(), state.name(C));
	}

	@Test
	void offlineMembersKeepAuthenticatedNamesAndProtectionUntilTheyActuallyLeave() {
		PartySession state = pair();
		UUID party = state.rules.party(A).id();
		state.disconnect(A);
		state.disconnect(B);
		state.prune(10_000, player -> false);
		assertEquals("Ash", state.name(A));
		assertEquals("Birch", state.name(B));
		assertEquals(A, state.rules.party(B).leader());
		assertEquals(party, state.rules.party(B).id());
		assertTrue(state.rules.blocksHarm(A, B, null));
		assertFalse(state.rules.blocksHarm(A, B, true), "a fighting duel still overrides party protection");
		state.remember(B, "NewBirch");
		assertEquals("NewBirch", state.name(B), "reconnect refreshes the authenticated name for the same UUID");
		assertEquals(party, state.rules.party(B).id());
		assertEquals(List.of(A, B), state.rules.party(B).members());
	}

	@Test
	void offlineKickRetainsTheNameForCommandFeedbackThenCollectsIt() {
		PartySession state = pair();
		state.disconnect(B);
		state.prune(100, A::equals);
		assertEquals("Birch", state.name(B), "offline kick lookup still has the previously authenticated name");
		assertEquals(OK, state.rules.kick(A, B));
		assertEquals("Birch", state.name(B), "the synchronous kick announcement can still name the removed member");
		state.prune(120, A::equals);
		assertEquals(B.toString(), state.name(B));
		assertNull(state.rules.party(B));
		assertEquals("Ash", state.name(A));
	}

	@Test
	void kickingAnOnlineMemberKeepsTheirNameUntilDisconnect() {
		PartySession state = pair();
		assertEquals(OK, state.rules.kick(A, B));
		state.prune(100, player -> true);
		assertEquals("Birch", state.name(B));
		state.disconnect(B);
		assertEquals(B.toString(), state.name(B));
	}

	@Test
	void disbandCollectsOfflineNamesAndCancelledInviteNamesButKeepsOnlineNames() {
		PartySession state = pair();
		state.remember(C, "Cedar");
		assertEquals(OK, state.rules.invite(A, C, 20));
		state.disconnect(B);
		assertEquals(OK, state.rules.disband(A));
		state.prune(40, A::equals);
		assertEquals("Ash", state.name(A));
		assertEquals(B.toString(), state.name(B));
		assertEquals(C.toString(), state.name(C));
		assertEquals(NO_INVITE, state.rules.accept(C, A, 41));
		assertNull(state.rules.party(B));
	}

	@Test
	void aPendingInvitationRetainsNamesUntilItsExactExpiryWithoutJoiningTheRecipient() {
		PartySession state = new PartySession();
		state.remember(A, "Ash");
		state.remember(B, "Birch");
		assertEquals(OK, state.rules.invite(A, B, 100));
		state.prune(100 + PartyRules.INVITE_TICKS - 1, player -> false);
		assertEquals("Ash", state.name(A));
		assertEquals("Birch", state.name(B));
		assertNull(state.rules.party(B));
		state.prune(100 + PartyRules.INVITE_TICKS, player -> false);
		assertEquals(B.toString(), state.name(B));
		assertEquals("Ash", state.name(A), "the inviter's existing one-person party still owns its identity");
		assertEquals(NO_INVITE, state.rules.accept(B, A, 100 + PartyRules.INVITE_TICKS));
	}

	@Test
	void clockRollbackAndDecliningTheLastInvitationReleaseUnusedNames() {
		PartySession state = new PartySession();
		state.remember(C, "Cedar");
		state.rules.invite(A, C, 100);
		state.prune(99, player -> false);
		assertEquals(C.toString(), state.name(C));
		state.remember(C, "Cedar");
		state.rules.invite(A, C, 200);
		state.rules.invite(B, C, 200);
		assertEquals(OK, state.rules.decline(C, A, 201));
		state.prune(201, player -> false);
		assertEquals("Cedar", state.name(C), "one remaining invitation still references the recipient");
		assertEquals(OK, state.rules.decline(C, B, 202));
		state.prune(202, player -> false);
		assertEquals(C.toString(), state.name(C));
	}

	@Test
	void disconnectCancelsInvitationsBeforeDecidingWhetherToKeepNames() {
		PartySession state = pair();
		state.remember(C, "Cedar");
		state.rules.invite(A, C, 20);
		state.disconnect(C);
		assertEquals(C.toString(), state.name(C));
		assertEquals(NO_INVITE, state.rules.accept(C, A, 21));
		state.remember(C, "Cedar");
		state.rules.invite(A, C, 40);
		state.disconnect(A);
		state.prune(41, player -> false);
		assertEquals(C.toString(), state.name(C));
		assertEquals("Ash", state.name(A));
		assertEquals(NO_INVITE, state.rules.accept(C, A, 42));
	}

	@Test
	void leadershipChangesKeepTheOfflineSuccessorAndReleaseTheDepartedLeader() {
		PartySession state = pair();
		state.remember(D, "Dogwood");
		state.rules.invite(A, D, 20);
		state.disconnect(B);
		assertEquals(OK, state.rules.leave(A));
		state.prune(40, player -> false);
		assertEquals(B, state.rules.party(B).leader());
		assertEquals("Birch", state.name(B));
		assertEquals(A.toString(), state.name(A));
		assertEquals(D.toString(), state.name(D));
		assertEquals(NO_INVITE, state.rules.accept(D, A, 41));
	}

	@Test
	void namesAndMembershipAreSessionOnlyAndUnknownNamesCannotCreateMembership() {
		PartySession first = pair();
		PartySession next = new PartySession();
		assertEquals(B.toString(), next.name(B));
		assertNull(next.rules.party(B));
		next.remember(C, "Birch");
		assertEquals("Birch", next.name(C));
		assertNull(next.rules.party(C), "a name matching an old member cannot grant that member's authority");
		assertEquals("Birch", first.name(B));
		assertTrue(first.rules.sameParty(A, B));
	}

	private static PartySession pair() {
		PartySession state = new PartySession();
		state.remember(A, "Ash");
		state.remember(B, "Birch");
		assertEquals(OK, state.rules.invite(A, B, 0));
		assertEquals(OK, state.rules.accept(B, A, 1));
		return state;
	}
}
