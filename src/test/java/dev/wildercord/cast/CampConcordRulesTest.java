package dev.wildercord.cast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Resource conservation over realistic donor/need boundaries, not a restatement of implementation branches. */
final class CampConcordRulesTest {
 @Test void donationAlwaysLosesResourceAndKeepsReserve(){for(int donor=0;donor<=300;donor++)for(int target=0;target<=220;target++){var g=CampConcordRules.gift(donor,target,200);if(g.gain()>0){assertTrue(g.debit()>g.gain());assertTrue(donor-g.debit()>=2);assertTrue(target+g.gain()<=200);assertTrue(g.gain()<=16&&g.debit()<=24);}else assertEquals(0,g.debit());}}
 @Test void concreteTransferExamplesAndFractions(){assertEquals(new CampConcordRules.Gift(22,16),CampConcordRules.gift(40,0,200));assertEquals(new CampConcordRules.Gift(10,7),CampConcordRules.gift(12,0,200));assertEquals(new CampConcordRules.Gift(2,1),CampConcordRules.gift(40,199,200));assertEquals(CampConcordRules.Gift.NONE,CampConcordRules.gift(3,0,200));assertEquals(CampConcordRules.Gift.NONE,CampConcordRules.gift(40,199.25F,200));}
 @Test void corruptPoolsCannotGrant(){for(float f:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1}){assertEquals(CampConcordRules.Gift.NONE,CampConcordRules.gift(f,0,200));assertEquals(CampConcordRules.Gift.NONE,CampConcordRules.gift(100,f,200));}}
 @Test void receivingThenReturningCannotMint(){float a=100,b=0;for(int i=0;i<12;i++){float before=a+b;var gift=CampConcordRules.gift(a,b,200);a-=gift.debit();b+=gift.gain();assertTrue(a+b<=before);var back=CampConcordRules.gift(b,a,200);b-=back.debit();a+=back.gain();assertTrue(a+b<=before);}assertTrue(a+b<100);}
 @Test void consentRequiresFreshEdgeAndRestRejectsForeignClock(){assertFalse(CampConcordRules.edge(true,true));assertFalse(CampConcordRules.edge(true,false));assertTrue(CampConcordRules.edge(false,true));assertEquals(900,CampConcordRules.ready(900,500,600));assertEquals(0,CampConcordRules.ready(1200,500,600));}
}
