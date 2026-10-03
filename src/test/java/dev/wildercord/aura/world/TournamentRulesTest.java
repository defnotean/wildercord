package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class TournamentRulesTest {
 @Test void completeCleanRunIsRequired(){assertTrue(TournamentRules.award(2,true,false,true));assertFalse(TournamentRules.award(1,true,false,true));assertFalse(TournamentRules.award(2,false,false,true));assertFalse(TournamentRules.award(2,true,true,true));assertFalse(TournamentRules.award(2,true,false,false));}
 @Test void threeDistinctSchoolsPerSiteAndGathering(){for(long site:new long[]{Long.MIN_VALUE,-99,0,213,Long.MAX_VALUE})for(long event:new long[]{0,72000,144000}){int a=TournamentRules.method(site,event,0),b=TournamentRules.method(site,event,1),c=TournamentRules.method(site,event,2);assertNotEquals(a,b);assertNotEquals(a,c);assertNotEquals(b,c);}}
 @Test void gatheringHasExclusiveDeadline(){assertFalse(TournamentRules.open(0,-1,-1));assertFalse(TournamentRules.open(99,100,200));assertTrue(TournamentRules.open(100,100,200));assertTrue(TournamentRules.open(199,100,200));assertFalse(TournamentRules.open(200,100,200));}
}
