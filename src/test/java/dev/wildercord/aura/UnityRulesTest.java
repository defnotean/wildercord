package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnityRulesTest {
	@Test void activationHasExactWindowAndIndependentRest() {
		var s=UnityRules.begin(100);assertFalse(s.active(99));assertTrue(s.active(100));assertTrue(s.active(339));assertFalse(s.active(340));
		assertEquals(2500,s.readyAt());assertFalse(s.stop().active(100));assertEquals(2500,s.stop().readyAt());
	}
	@Test void exchangeIsLossyAcrossAlternatingSpends() {
		double mana=20,total=0;
		for(int i=0;i<100;i++){float aura=UnityRules.convert(mana,UnityRules.MANA_TO_AURA,0,1000,1000);
			mana=UnityRules.convert(aura,UnityRules.AURA_TO_MANA,0,1000,1000);total+=mana;}
		assertTrue(total<3,"20 mana cannot circulate into a free resource loop");
	}
	@Test void eachAllowanceIsIndependentAndFiniteAcrossManyTinyPayments() {
		var s=UnityRules.begin(1);
		for(int i=0;i<10000;i++)s=s.returned(UnityRules.convert(.1,UnityRules.AURA_TO_MANA,s.manaReturned(),UnityRules.MANA_CAP,100),
			UnityRules.convert(.1,UnityRules.MANA_TO_AURA,s.auraReturned(),UnityRules.AURA_CAP,100));
		assertEquals(24,s.manaReturned(),.001);assertEquals(12,s.auraReturned(),.001);
		assertEquals(0,UnityRules.convert(100,.5,s.manaReturned(),24,100));
	}
	@Test void onlyDeliveredResourcesUseAllowance() {
		assertEquals(2,UnityRules.convert(20,.5,0,24,2));assertEquals(0,UnityRules.convert(20,.5,0,24,0));
		assertEquals(1,UnityRules.convert(100,.5,23,24,100));
	}
	@Test void invalidPaymentsAndClockOverflowCannotCreateResources() {
		for(double amount:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY})assertEquals(0,UnityRules.convert(amount,.5,0,24,100));
		assertSame(UnityRules.State.NONE,UnityRules.begin(-1));assertSame(UnityRules.State.NONE,UnityRules.begin(Long.MAX_VALUE));
		assertFalse(new UnityRules.State(1,10000,12000,0,0).active(2));
	}
	@Test void corruptedReturnCountersCannotRenewBudgets() {
		var s=new UnityRules.State(0,240,2400,Float.NaN,Float.POSITIVE_INFINITY);
		assertEquals(24,s.manaReturned());assertEquals(12,s.auraReturned());
	}
}
