package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CastHitRulesTest {
	@Test void sameLiveTokenRequiresResolvedDamage() {
		Object charge = new Object();
		assertEquals(CastHitRules.Response.INTERRUPT, CastHitRules.response(true, charge, charge));
		assertEquals(CastHitRules.Response.NONE, CastHitRules.response(false, charge, charge));
	}
	@Test void idleSealRequiresIdleBeforeAndAfter() {
		assertEquals(CastHitRules.Response.SEAL_IDLE, CastHitRules.response(true, null, null));
		assertEquals(CastHitRules.Response.NONE, CastHitRules.response(false, null, null));
		Object charge = new Object();
		assertEquals(CastHitRules.Response.NONE, CastHitRules.response(true, null, charge));
		assertEquals(CastHitRules.Response.NONE, CastHitRules.response(true, charge, null));
	}
	@Test void equalReplacementCannotInheritTheHit() {
		record Token(long start, int spell) {}
		Token before = new Token(100, 0), replacement = new Token(100, 0);
		assertEquals(before, replacement);
		assertNotSame(before, replacement);
		assertEquals(CastHitRules.Response.NONE, CastHitRules.response(true, before, replacement));
		assertEquals(CastHitRules.Response.NONE, CastHitRules.response(false, before, replacement));
	}
}
