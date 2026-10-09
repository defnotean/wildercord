package dev.wildercord.party;

import dev.wildercord.aura.StoneHingeRules;
import dev.wildercord.spell.CircleVows;
import dev.wildercord.spell.LessonPackRules;
import dev.wildercord.spell.MasterStudyRules;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Party rules as they touch forms and lessons: a party shields its members from each other's harmful lessons and
 * forms, never teaches, entitles or pays for anyone, and never removes an ally's chance to refuse a Lifeline.
 */
class PartyProgressionRulesTest {
	private static final UUID LEADER = new UUID(0, 1), MEMBER = new UUID(0, 2), STRANGER = new UUID(0, 3);

	private static PartyRules party() {
		PartyRules rules = new PartyRules();
		assertEquals(PartyRules.Result.OK, rules.invite(LEADER, MEMBER, 0));
		assertEquals(PartyRules.Result.OK, rules.accept(MEMBER, LEADER, 1));
		return rules;
	}

	@Test
	void membersCannotHarmEachOtherWithLessonsOrFormsUnlessTheyDuel() {
		PartyRules rules = party();
		// Tollgate tolls, Cinder Lunge sweeps and arts all ask the same veto (Parties.blocksHarm / blocksCurrentHarm).
		assertTrue(rules.blocksHarm(LEADER, MEMBER, null));
		assertTrue(rules.blocksHarm(MEMBER, LEADER, null));
		assertFalse(rules.blocksHarm(LEADER, MEMBER, true), "An accepted, fighting duel is the only exception");
		assertFalse(rules.blocksHarm(LEADER, STRANGER, null), "Strangers keep the ordinary PvP and team rules");
		assertFalse(rules.blocksHarm(LEADER, LEADER, null), "Your own costs and recoils are never refunded");
	}

	@Test
	void leavingOrBeingKickedEndsProtectionAtOnce() {
		PartyRules rules = party();
		assertEquals(PartyRules.Result.OK, rules.leave(MEMBER));
		assertFalse(rules.blocksHarm(LEADER, MEMBER, null));
		rules = party();
		assertEquals(PartyRules.Result.OK, rules.kick(LEADER, MEMBER));
		assertFalse(rules.sameParty(LEADER, MEMBER));
		rules = party();
		rules.disconnect(MEMBER);
		assertTrue(rules.blocksHarm(LEADER, MEMBER, null), "A disconnected member keeps protection for their lingering effects");
	}

	@Test
	void entitlementIsPersonalAndNeverBorrowedFromAPartner() {
		// Every lesson asks only for the learner's own circles and own feat; there is no party argument to share.
		for (LessonPackRules.Lesson lesson : LessonPackRules.ALL) {
			assertFalse(lesson.eligible(lesson.circle, false), lesson.name + " without the learner's own feat");
			assertFalse(lesson.eligible(lesson.circle - 1, true), lesson.name + " below the learner's own circle");
			assertTrue(lesson.eligible(lesson.circle, true));
			assertFalse(lesson.mayStudy(lesson.circle, true, false), "Reading needs the learner's own copy");
		}
		assertFalse(MasterStudyRules.eligibleReweave(MasterStudyRules.REWEAVE_CIRCLE, false));
		assertFalse(MasterStudyRules.eligibleExcise(MasterStudyRules.EXCISE_CIRCLE, false));
		assertFalse(StoneHingeRules.eligible(5, false), "Stone Hinge needs this player's own Stone Master clear");
		assertFalse(StoneHingeRules.eligible(4, true));
		// Circle Vows are saved per player; a partner's circles cannot open one.
		assertEquals(CircleVows.Refusal.NOT_REACHED, CircleVows.take(0, 8, 9));
	}

	@Test
	void aPartyAllyAlwaysGetsTheLifelineRefusalWindow() {
		long threaded = 100;
		for (long now = threaded; now < threaded + LessonPackRules.CONSENT_TICKS; now++) {
			assertTrue(LessonPackRules.consentOpen(threaded, now));
			assertFalse(LessonPackRules.mayReel(threaded, now, false), "No reel inside the refusal window, party or not");
		}
		assertTrue(LessonPackRules.mayReel(threaded, threaded + LessonPackRules.CONSENT_TICKS, false));
		assertFalse(LessonPackRules.mayReel(threaded, threaded + LessonPackRules.CONSENT_TICKS, true), "A crouching ally holds fast");
		assertFalse(LessonPackRules.mayReel(threaded, threaded - 1, false), "A rolled-back clock never reels");
	}
}
