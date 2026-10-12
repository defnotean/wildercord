package dev.wildercord.aura.world;
import static dev.wildercord.aura.world.LanternBearerRules.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.cast.MoveDeck;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LanternBearerRulesTest {
	private static long[] rested(){long[] s=new long[32];Arrays.fill(s,99999);return s;}
	private static int at(int move){for(int i=0;i<PICKS.length;i++)if(PICKS[i]==move)return i;throw new IllegalArgumentException(key(move));}
	private static double weight(int move,double d,boolean behind,double h,boolean roused){return weights(d,behind,h,rested(),roused)[at(move)];}
	/** The move it wants most there, rested. */
	private static int best(double d,boolean behind,double h,boolean roused){
		double[] w=weights(d,behind,h,rested(),roused);int b=-1;
		for(int i=0;i<w.length;i++)if(w[i]>0&&(b<0||w[i]>w[b]))b=i;
		return b<0?-1:PICKS[b];
	}

	@Test void onlyAfterDarkAndInAmongTheGravesDoesItWake(){
		assertTrue(wakes(5,0,5,true));
		assertFalse(wakes(5,0,5,false));
		assertFalse(wakes(10,0,10,true));
		assertFalse(wakes(0,8,0,true));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(scripted(DORMANT));assertTrue(scripted(RISE));assertTrue(scripted(BED));
		for(int m:PICKS)assertFalse(scripted(m),key(m));
		assertFalse(scripted(WATCH));assertFalse(scripted(FADE));assertFalse(scripted(RITE));assertFalse(scripted(SNAGGED));
	}
	@Test void theRiseRunsForwardAndSinkingRunsItBack(){
		assertEquals(0,rise(DORMANT,30));
		assertEquals(.5F,rise(RISE,RISE_TICKS/2F),1e-6);
		assertEquals(1,rise(BED,0));
		assertEquals(0,rise(BED,RISE_TICKS));
		assertEquals(1,rise(REAP,0));
	}
	@Test void itTakesTheLanternDownPartWayUpAndHangsItBackPartWayDown(){
		assertFalse(holdsLantern(0));
		assertFalse(holdsLantern(.5F));
		assertTrue(holdsLantern(rise(RISE,HOOK_AT)));
		assertTrue(holdsLantern(1));
		assertTrue(ROAR_AT>HOOK_AT);
	}

	@Test void crowdedInFrontItReapsAndRoundItsBackItSweepsBehindIt(){
		assertEquals(REAP,best(2.5,false,0,false));
		assertEquals(BACKSWEEP,best(3,true,0,false));
		assertEquals(0,weight(BACKSWEEP,3,false,0,false));
		assertEquals(0,weight(REAP,3,true,0,false));
		assertTrue(weight(FLARE,3,true,0,false)>0);
		assertEquals(0,weight(FLARE,8,false,0,false));
	}
	@Test void aWayOffItHooksShovelsAndBreathesAndUpOutOfReachItLoosesWisps(){
		assertEquals(HOOK,best(8,false,0,false));
		for(int m:new int[]{SHOVEL,BREATH,WISPS,GRAVES})assertTrue(weight(m,8,false,0,false)>0,key(m));
		assertEquals(0,weight(REAP,8,false,0,false));
		assertEquals(0,weight(SHOVEL,SHOVEL_NEAR-.5,false,0,false));
		assertEquals(0,weight(BREATH,6,true,0,false));
		assertEquals(WISPS,best(6,false,3,false));
		assertEquals(0,weight(REAP,2,false,3,false));
	}
	@Test void aMoveStillRestingWeighsNothingAndRousedItRestsLess(){
		long[] since=rested();since[REAP]=rest(REAP,false)-1;
		assertEquals(0,weights(2,false,0,since,false)[at(REAP)]);
		since[REAP]=rest(REAP,false);
		assertTrue(weights(2,false,0,since,false)[at(REAP)]>0);
		for(int m:PICKS){
			assertTrue(rest(m,false)>0,key(m));
			if(m==LIGHTS_OUT)assertEquals(rest(m,false),rest(m,true));else assertTrue(rest(m,true)<rest(m,false),key(m));
		}
		assertEquals(-1,choose(new MoveDeck(4),4,false,0,new long[32],false,.5));
	}
	@Test void onlyRousedDoesItPutItsLanternOut(){
		for(double d:new double[]{2,6,12})for(boolean behind:new boolean[]{false,true}){
			assertEquals(0,weight(LIGHTS_OUT,d,behind,0,false));
			assertTrue(weight(LIGHTS_OUT,d,behind,0,true)>0);
		}
	}
	@Test void itNeverMakesTheSameMoveTwiceRunningAndMixesThemAllUp(){
		var deck=new MoveDeck(4);var rng=new Random(7);long[] since=rested();
		Map<Integer,Integer> made=new HashMap<>();int last=-1;
		for(int i=0;i<300;i++){
			double d=new double[]{2.5,6,10}[i%3];boolean behind=i%7==0,roused=i>=150;
			int m=choose(deck,d,behind,0,since,roused,rng.nextDouble());
			assertNotEquals(-1,m);
			assertNotEquals(last,m,"the same move twice running at "+d);
			made.merge(m,1,Integer::sum);deck.played(m);last=m;
		}
		assertTrue(made.size()>=7,"only "+made.keySet());
		for(var e:made.entrySet())assertTrue(e.getValue()<120,key(e.getKey())+" made "+e.getValue()+" times");
	}
	@Test void rousedItRunsOnIntoTheMovesThatFollowAndNeverTheSameAgain(){
		assertEquals(REAP,chain(HOOK,3,.1));assertEquals(-1,chain(HOOK,3,.9));assertEquals(-1,chain(HOOK,8,.1));
		assertEquals(FLARE,chain(REAP,3,.1));assertEquals(REAP,chain(BACKSWEEP,3,.1));
		assertEquals(SHOVEL,chain(GRAVES,8,.1));assertEquals(FLARE,chain(GRAVES,3,.1));
		assertEquals(HOOK,chain(SHOVEL,8,.1));assertEquals(WISPS,chain(FLARE,6,.1));assertEquals(GRAVES,chain(WISPS,6,.1));
		assertEquals(-1,chain(BREATH,5,0));assertEquals(-1,chain(LIGHTS_OUT,3,0));assertEquals(-1,chain(RITE,3,0));
		for(int m:PICKS)for(double d=0;d<=16;d+=.5)for(double r=0;r<1;r+=.05)assertNotEquals(m,chain(m,d,r),key(m));
	}
	@Test void halfBeatenItHoldsItsRiteOnceAndThenFightsQuicker(){
		assertTrue(rouses(170,340,false));
		assertFalse(rouses(170,340,true));
		assertFalse(rouses(171,340,false));
		assertTrue(GAP_ROUSED<GAP);
		assertTrue(tell(RITE,false)>tell(FLARE,false)*2);
		assertTrue(armour(RITE,true,false)<1);
		assertTrue(open(RITE,tell(RITE,false),tell(RITE,false),false));
	}
	@Test void everyMoveGivesItselfAwayInTimeAndChainedALittleQuicker(){
		for(int m:PICKS){
			int t=tell(m,false),c=tell(m,true);
			assertTrue(t>=10&&t<=18,key(m)+" tell "+t);
			assertTrue(c>=10&&c<=t,key(m)+" chained tell "+c);
		}
	}

	@Test void theLowReapSweepsAcrossItsFrontAndCanBeJumped(){
		assertTrue(hits(REAP,3,1,0));
		assertTrue(hits(REAP,2,-3,0));
		assertFalse(hits(REAP,3,1,1));
		assertFalse(hits(REAP,-1,0,0));
		assertFalse(hits(REAP,5,0,0));
		assertEquals(REAP_ARC,reapArc(0,false),1e-9);assertEquals(-REAP_ARC,reapArc(REAP_SWEEP,false),1e-9);
		assertEquals(-REAP_ARC,reapArc(0,true),1e-9);assertEquals(REAP_ARC,reapArc(REAP_SWEEP,true),1e-9);
		for(boolean back:new boolean[]{false,true})for(double b=-REAP_ARC;b<=REAP_ARC;b+=5){
			boolean swept=false;
			for(int k=0;k<REAP_SWEEP;k++)swept|=sweeps(b,reapArc(k,back),reapArc(k+1,back));
			assertTrue(swept,"bearing "+b+(back?" back":""));
		}
	}
	@Test void theHookReachesFarButNarrowAndDragsHardAtFirst(){
		assertTrue(hits(HOOK,9,.5,0));
		assertFalse(hits(HOOK,9,2,0));
		assertFalse(hits(HOOK,11,0,0));
		assertFalse(hits(HOOK,.2,0,0));
		assertTrue(pull(9)>pull(4));
		assertEquals(0,pull(HOOK_PULL_TO));
		assertEquals(0,pull(1));
		assertTrue(pull(100)<=1.6);
		assertTrue(armour(SNAGGED,true,false)>1);
	}
	@Test void theBacksweepCatchesWhoeverIsBehindButNotRoundItsFlank(){
		assertTrue(hits(BACKSWEEP,-3,0,0));
		assertTrue(hits(BACKSWEEP,-2,-2,0));
		assertFalse(hits(BACKSWEEP,0,3,0));
		assertFalse(hits(BACKSWEEP,3,0,0));
		assertFalse(hits(BACKSWEEP,-6,0,0));
		assertTrue(behind(bearing(-3,0)));assertFalse(behind(bearing(0,3)));
	}
	@Test void theBreathIsANarrowConeThatTurnsAndRousedSweepsBackAcross(){
		assertTrue(hits(BREATH,6,0,0));
		assertFalse(hits(BREATH,6,4,0));
		assertFalse(hits(BREATH,-3,0,0));
		assertFalse(hits(BREATH,10,0,0));
		assertEquals(0,breathTurn(0,false),1e-9);
		assertEquals(BREATH_TURN,breathTurn(BREATH_TICKS,false),1e-9);
		assertEquals(BREATH_TURN,breathTurn(BREATH_TICKS_ROUSED/2.0,true),1e-9);
		assertTrue(breathTurn(BREATH_TICKS_ROUSED,true)<0);
		assertTrue(breathTicks(true)>breathTicks(false));
		assertTrue(armour(BREATH,false,false)>armour(BREATH,true,false));
	}
	@Test void theFlareBurstsAllRoundAndItsRingsCanBeJumped(){
		assertTrue(hits(FLARE,-3,2,0));
		assertFalse(hits(FLARE,4,3,0));
		assertFalse(hits(FLARE,1,0,3.5));
		assertTrue(hits(RITE,3,3,0));
		assertFalse(hits(RITE,4,4,0));
		assertTrue(rolls(6,5.5,6.2,0));
		assertFalse(rolls(6,5.5,6.2,1));
		assertFalse(rolls(9,5.5,6.2,0));
		assertFalse(rolls(3,5.5,6.2,0));
		assertTrue(FLARE_WAVE_REACH>FLARE_RADIUS&&RITE_WAVE_REACH>RITE_RADIUS);
		assertFalse(hits(WATCH,1,0,0));
	}
	@Test void aGraveIsMarkedByAClosingRingLongEnoughToStepOut(){
		assertEquals(GRAVE_RING,graveRing(0),1e-9);
		assertEquals(GRAVE_RING_END,graveRing(GRAVE_MARK),1e-9);
		for(int a=1;a<=GRAVE_MARK;a++)assertTrue(graveRing(a)<graveRing(a-1));
		assertTrue(buries(1,0,1));
		assertFalse(buries(1.5,0,1));
		assertFalse(buries(0,3,0));
		assertTrue(GRAVE_MARK*.15>GRAVE_RADIUS);
	}
	@Test void theEarthFallsRoundWhereYouStandButNeverCloseUnderIt(){
		for(double d=0;d<=20;d+=.5)for(int i=0;i<CLODS;i++)for(boolean second:new boolean[]{false,true}){
			double[] s=shovelSpot(i,d,second);double r=Math.hypot(s[0],s[1]);
			assertTrue(r>=SHOVEL_NEAR-1e-9&&r<=SHOVEL_FAR+1e-9,"clod "+i+" at "+d+" lands "+r);
		}
		double[] mid=shovelSpot(CLODS/2,8,false);
		assertEquals(8,mid[0],1e-9);assertEquals(0,mid[1],1e-9);
		var spots=new HashSet<String>();
		for(int i=0;i<CLODS;i++)for(boolean second:new boolean[]{false,true}){double[] s=shovelSpot(i,8,second);spots.add(Math.round(s[0]*10)+","+Math.round(s[1]*10));}
		assertEquals(CLODS*2,spots.size());
		assertTrue(clods(1,0,1));
		assertFalse(clods(1.2,0,1.2));
	}
	@Test void wispsBurstOnlyWhenTheyReachTheirPreyAndGutterOutOfSight(){
		assertTrue(bursts(1));
		assertFalse(bursts(2));
		assertTrue(gutters(WISP_BLIND));
		assertFalse(gutters(WISP_BLIND-1));
		assertTrue(WISP_COUNT_ROUSED>WISP_COUNT);
	}
	@Test void itIsOpenStuckCoughingSnaggedOrSpentAndSmokeTakesNothing(){
		assertEquals(0,armour(FADE,true,false));
		assertTrue(armour(RECOVER,true,false)>1);
		assertTrue(armour(WATCH,true,false)<armour(WATCH,false,false));
		int g=tell(GRAVES,false),b=tell(BREATH,false);
		assertFalse(open(GRAVES,g,g,false));assertTrue(open(GRAVES,g+1,g,false));
		assertFalse(open(BREATH,b+BREATH_TICKS-1,b,false));assertTrue(open(BREATH,b+BREATH_TICKS,b,false));
		assertFalse(open(BREATH,b+BREATH_TICKS,b,true));
		assertFalse(open(REAP,99,tell(REAP,false),true));
		assertTrue(armour(GRAVES,true,true)>1);
		assertTrue(frontal(30));assertTrue(frontal(370));assertFalse(frontal(120));
	}
	@Test void itFadesOnlyWhileWatchingAndNotTooOften(){
		assertTrue(dodges(WATCH,FADE_REST,.1));
		assertFalse(dodges(WATCH,FADE_REST-1,.1));
		assertFalse(dodges(REAP,999,.1));
		assertFalse(dodges(WATCH,999,.9));
	}
	@Test void stungEnoughItDriftsAway(){
		assertTrue(leaps(WATCH,STING_AFTER,LEAP_REST));
		assertTrue(leaps(RECOVER,STING_AFTER,LEAP_REST));
		assertFalse(leaps(WATCH,STING_AFTER-1,LEAP_REST));
		assertFalse(leaps(GRAVES,99,999));
		assertFalse(leaps(RITE,99,999));
	}
	@Test void dawnSendsItDownAndBeingLeftAloneLongEnoughForgets(){
		assertFalse(dawn(DAWN_TICKS-1));
		assertTrue(dawn(DAWN_TICKS));
		assertFalse(forgets(RESET_TICKS));
		assertTrue(forgets(RESET_TICKS+1));
		assertTrue(wounded(170,340));
		assertFalse(wounded(171,340));
	}
	@Test void lightsOutRunsSnuffThenGoneGlimmerSwingEachRound(){
		int s=SNUFF_TELL,R=LIGHTS_ROUND,G=LIGHTS_GONE,M=LIGHTS_GLIMMER;
		assertEquals(SNUFFING,lightsStage(0));
		assertEquals(-1,lightsRound(0));
		assertEquals(GONE,lightsStage(s));
		assertEquals(GLIMMER,lightsStage(s+G));
		assertEquals(SWING,lightsStage(s+G+M));
		assertEquals(GONE,lightsStage(s+R));
		assertEquals(1,lightsRound(s+R));
		assertEquals(LIGHTS_ROUNDS-1,lightsRound(LIGHTS_TICKS-1));
		assertEquals(OVER,lightsStage(LIGHTS_TICKS));
		assertEquals(0,lightsGlimmer(s+R+G),1e-6);
	}
	@Test void outInTheDarkItCannotBeSeenUntilItGlimmers(){
		int s=SNUFF_TELL,G=LIGHTS_GONE;
		assertTrue(lightsSeen(s-1));
		assertTrue(lightsSeen(s));
		assertFalse(lightsSeen(s+LIGHTS_VANISH));
		assertFalse(lightsSeen(s+G-1));
		assertTrue(lightsSeen(s+G));
		assertEquals(0,lightsSolid(s+G-1),1e-6);
		assertTrue(lightsSolid(s+G)>0);
	}
	@Test void itMovesBehindItsPreyUnseenAndReapsOncePerRound(){
		int strikes=0,moves=0;
		for(long el=0;el<LIGHTS_TICKS+10;el++){
			if(lightsStrikes(el)){strikes++;assertEquals(SWING,lightsStage(el));}
			if(lightsAppears(el)){moves++;assertFalse(lightsSeen(el));}
		}
		assertEquals(LIGHTS_ROUNDS,strikes);
		assertEquals(LIGHTS_ROUNDS,moves);
		assertTrue(hits(LIGHTS_OUT,3,0,1));
		assertFalse(hits(LIGHTS_OUT,-3,0,1));
	}
	@Test void theBarrowSteedFadesOnceItsRiderHasBeenOffItAWhile(){
		assertTrue(steedStays(true,0));
		assertTrue(steedStays(true,STEED_EMPTY-1));
		assertFalse(steedStays(true,STEED_EMPTY));
		assertFalse(steedStays(false,0));
		assertTrue(STEED_LINGER>STEED_CHECK*STEED_EMPTY);
	}
	@Test void theBarNamesWhatItIsDoingAndEveryNameIsWritten() throws IOException{
		assertEquals("lights",key(LIGHTS_OUT));
		assertEquals("reap",key(REAP));
		assertEquals("graves",key(GRAVES));
		assertEquals("rite",key(RITE));
		assertEquals("watch",key(RISE));
		JsonObject lang=lang();
		var keys=new HashSet<String>();
		for(int m:PICKS)assertTrue(keys.add(key(m)),"two moves named "+key(m));
		for(int m:new int[]{REAP,HOOK,WISPS,GRAVES,FLARE,SHOVEL,BACKSWEEP,BREATH,LIGHTS_OUT,SNAGGED,RITE,FADE,RECOVER,WATCH})
			assertTrue(lang.has("boss.wildercord.lantern_bearer.move."+key(m)),key(m));
		assertFalse(lang.has("boss.wildercord.lantern_bearer.move.slam"));
		assertTrue(lang.has("guide.wildercord.lantern_bearer"));
	}
	private static JsonObject lang() throws IOException{
		try(InputStream in=LanternBearerRulesTest.class.getResourceAsStream("/assets/wildercord/lang/en_us.json")){
			if(in!=null)return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
		}
		return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
	}
}
