package dev.wildercord.monster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TemperingRulesTest {
 @Test void aNewPlayerMeetsTheWorldAsItWas(){int t=TemperingRules.threat(0,0);assertEquals(0,t);assertEquals(1,TemperingRules.health(t));assertEquals(1,TemperingRules.damage(t));assertEquals(0,TemperingRules.eliteChance(t));}
 @Test void theStrongestMeetTheHardestWorld(){int t=TemperingRules.threat(20,4);assertEquals(32,t);assertEquals(4,TemperingRules.health(t),1e-9);assertEquals(2.5,TemperingRules.damage(t),1e-9);assertEquals(.20,TemperingRules.eliteChance(t),1e-9);}
 @Test void threatGrowsSteadily(){double last=0;for(int c=0;c<=20;c++){double h=TemperingRules.health(TemperingRules.threat(c,c/5));assertTrue(h>=last);last=h;}assertEquals(TemperingRules.MAX_THREAT,TemperingRules.threat(999,999));assertEquals(0,TemperingRules.threat(-5,-1));}
 @Test void bossesGrowWithTheParty(){assertEquals(1,TemperingRules.bossParty(1));assertEquals(1,TemperingRules.bossParty(0));assertEquals(2,TemperingRules.bossParty(3),1e-9);assertEquals(TemperingRules.bossParty(TemperingRules.PARTY_MAX),TemperingRules.bossParty(40));}
 @Test void bossesEnrageOnceHalfGone(){assertFalse(TemperingRules.enrages(100,100));assertFalse(TemperingRules.enrages(51,100));assertTrue(TemperingRules.enrages(50,100));assertTrue(TemperingRules.enrages(1,100));assertFalse(TemperingRules.enrages(0,100));assertFalse(TemperingRules.enrages(5,0));}
 @Test void splittingLeavesLesserCopies(){assertTrue(TemperingRules.SPLIT_COUNT>=2);assertTrue(TemperingRules.SPLIT_HEALTH<1);assertEquals("wildercord.elite.splitting",TemperingRules.Elite.SPLITTING.tag());}
}
