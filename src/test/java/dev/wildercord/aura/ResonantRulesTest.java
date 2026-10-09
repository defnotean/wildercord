package dev.wildercord.aura;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class ResonantRulesTest {
	private static final UUID M=UUID.randomUUID(), B=UUID.randomUUID(), T=UUID.randomUUID();
	private static ResonantRules.Hit hit(boolean blade,long time) { return new ResonantRules.Hit(blade?B:M,blade?"wind":"fire",8,time,blade); }
	@Test void scopedCreditKeepsItsMarkerAndExactRetirementCannotEraseAnotherOpening() {
		var ledger = new ResonantRules.Ledger();
		var scoped = new ResonantRules.Hit(M,"frost",5,100,false,true);
		ledger.offer(T,scoped,(a,b)->true);
		assertNull(ledger.offer(T,hit(true,101),(spell,blade)->!spell.scoped()),"Missing original receipt refuses a scoped pair");
		var newer = new ResonantRules.Hit(M,"fire",6,102,false);
		ledger.offer(T,newer,(a,b)->true);
		ledger.retire(T,scoped);
		assertNotNull(ledger.offer(T,hit(true,103),(a,b)->true),"Retiring stale scoped credit preserves the newer ordinary opening");
		var other = new ResonantRules.Ledger();other.offer(T,scoped,(a,b)->true);other.retire(T,scoped);
		assertNull(other.offer(T,hit(true,101),(a,b)->true),"Closed paid credit cannot become a later fresh Cast");
	}

	@Test void eitherOrderWorksOnlyInsideWindow() {
		for(boolean first:new boolean[]{false,true}) {
			var l=new ResonantRules.Ledger();assertNull(l.offer(T,hit(first,100),(a,b)->true));
			var p=l.offer(T,hit(!first,124),(a,b)->true);assertNotNull(p);assertFalse(p.spell().blade());assertTrue(p.blade().blade());
			var expired=new ResonantRules.Ledger();expired.offer(T,hit(first,100),(a,b)->true);assertNull(expired.offer(T,hit(!first,125),(a,b)->true));
		}
	}
	@Test void repeatedSameKindCannotReact() { var l=new ResonantRules.Ledger();for(int i=0;i<200;i++)assertNull(l.offer(T,hit(false,i),(a,b)->true)); }
	@Test void hostilePairCannotStealOrConsumeOpening() {
		var l=new ResonantRules.Ledger();l.offer(T,hit(false,100),(a,b)->true);
		assertNull(l.offer(T,hit(true,101),(a,b)->false));assertNotNull(l.offer(T,hit(true,102),(a,b)->true));
	}
	@Test void targetAndBladeHaveIndependentRestIncludingReconnect() {
		var l=new ResonantRules.Ledger();l.offer(T,hit(false,100),(a,b)->true);assertNotNull(l.offer(T,hit(true,101),(a,b)->true));
		l.forget(B);var other=UUID.randomUUID();l.offer(other,hit(false,120),(a,b)->true);assertNull(l.offer(other,hit(true,121),(a,b)->true));
		assertNull(l.offer(T,hit(false,180),(a,b)->true));assertNull(l.offer(T,hit(false,181),(a,b)->true));assertNotNull(l.offer(T,hit(true,182),(a,b)->true));
	}
	@Test void missedNonFiniteAndUnknownHitsDoNotEraseOpening() {
		var l=new ResonantRules.Ledger();l.offer(T,hit(false,10),(a,b)->true);
		for(float taken:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY})assertNull(l.offer(T,new ResonantRules.Hit(B,"wind",taken,11,true),(a,b)->true));
		assertNull(l.offer(T,new ResonantRules.Hit(B,"unknown",8,11,true),(a,b)->true));assertNotNull(l.offer(T,hit(true,12),(a,b)->true));
	}
	@Test void damageUsesWeakerHitAndStrictCeilings() {
		assertEquals(1,ResonantRules.damage(100,4,false));assertEquals(3,ResonantRules.damage(100,100,false));assertEquals(.75F,ResonantRules.damage(100,100,true));assertEquals(0,ResonantRules.damage(Float.NaN,8,false));assertEquals(0,ResonantRules.damage(-1,8,false));
	}
	@Test void everySchoolAndElementHasIntentionalName() { var names=new HashSet<String>();for(String s:ResonantRules.ELEMENTS)for(String b:ResonantRules.ELEMENTS){var n=ResonantRules.name(s,b);assertFalse(n.isBlank());names.add(n);}assertEquals(20,names.size());assertEquals("",ResonantRules.name("unknown","wind")); }
	@Test void cacheCannotGrowWithLoadedFoes() { var l=new ResonantRules.Ledger();for(int i=0;i<10000;i++)l.offer(new UUID(0,i),hit(false,10),(a,b)->true);assertEquals(ResonantRules.LIMIT,l.size()); }
	@Test void clockRewindCannotUseFutureHit() { var l=new ResonantRules.Ledger();l.offer(T,hit(false,100),(a,b)->true);assertNull(l.offer(T,hit(true,90),(a,b)->true));assertFalse(ResonantRules.within(Long.MAX_VALUE,Long.MIN_VALUE,24)); }
	@Test void everyDynamicReactionVoiceIsActuallyPackaged() throws java.io.IOException {
		try(var in=getClass().getResourceAsStream("/assets/wildercord/kit_sounds.json")) {
			assertNotNull(in);var sounds=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("events");
			for(String e:ResonantRules.ELEMENTS)assertTrue(sounds.has(ResonantRules.sound(e)),e);
		}
	}
}
