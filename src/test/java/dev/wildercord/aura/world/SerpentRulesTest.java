package dev.wildercord.aura.world;
import com.google.gson.JsonParser;
import dev.wildercord.cast.MoveDeck;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
class SerpentRulesTest {
	private static long[] rested(){long[] a=new long[32];java.util.Arrays.fill(a,9999);return a;}
	private static double weight(double[] w,int move){
		for(int i=0;i<SerpentRules.PICKS.length;i++) if(SerpentRules.PICKS[i]==move) return w[i];
		throw new AssertionError("not a pick: "+move);
	}
	@Test void onlyAPlayerOnTheIslandNearItsCoilsWakesIt(){
		assertTrue(SerpentRules.wakes(5,0,5));
		assertTrue(SerpentRules.wakes(0,4,7));
		assertFalse(SerpentRules.wakes(6,0,6));
		assertFalse(SerpentRules.wakes(0,8,2));
		assertFalse(SerpentRules.wakes(0,-7,2));
	}
	@Test void itIsPutBackOnlyWhenItLeavesTheCavern(){
		assertFalse(SerpentRules.leashed(15,0,15));
		assertTrue(SerpentRules.leashed(17,0,17));
		assertTrue(SerpentRules.leashed(0,-8,0));
		assertTrue(SerpentRules.leashed(0,12,0));
	}
	@Test void theScriptedStatesHideTheBarAndTheFightDoesNot(){
		assertTrue(SerpentRules.scripted(SerpentRules.DORMANT));
		assertTrue(SerpentRules.scripted(SerpentRules.UNCOIL));
		assertTrue(SerpentRules.scripted(SerpentRules.RECOIL));
		assertFalse(SerpentRules.scripted(SerpentRules.WATCH));
	}
	@Test void theUncoilRunsForwardAndTheRecoilRunsItBack(){
		assertEquals(0,SerpentRules.uncoil(SerpentRules.DORMANT,30));
		assertEquals(.5F,SerpentRules.uncoil(SerpentRules.UNCOIL,SerpentRules.UNCOIL_TICKS/2F),1e-6);
		assertEquals(1,SerpentRules.uncoil(SerpentRules.UNCOIL,999));
		assertEquals(1,SerpentRules.uncoil(SerpentRules.RECOIL,0));
		assertEquals(0,SerpentRules.uncoil(SerpentRules.RECOIL,SerpentRules.UNCOIL_TICKS));
		assertEquals(1,SerpentRules.uncoil(SerpentRules.WATCH,0));
	}
	@Test void itWakesBeforeItCouldReachYouAndFightsInsideItsLeash(){
		assertTrue(SerpentRules.WAKE_RADIUS>=SerpentSpine.COIL_FOOT_R+3);
		assertTrue(SerpentRules.LAND_FAR<SerpentRules.ARENA && SerpentRules.ARENA<SerpentRules.LEASH);
		assertTrue(SerpentRules.BAR_SHOW<SerpentRules.BAR_DROP);
		assertTrue(SerpentRules.RETURN_TICKS<SerpentRules.RESET_TICKS);
		assertTrue(SerpentRules.inLair(10,3,-12));assertTrue(SerpentRules.inLair(0,-11,0));
		assertFalse(SerpentRules.inLair(15,0,15));assertFalse(SerpentRules.inLair(0,12,0));
		assertFalse(SerpentRules.forgets(1));assertFalse(SerpentRules.forgets(SerpentRules.RESET_TICKS));assertTrue(SerpentRules.forgets(SerpentRules.RESET_TICKS+1));
	}
	@Test void itPicksItsBlowsFromWhereItsPreyStands(){
		long[] since=rested();
		double[] front=SerpentRules.weights(3,false,since,false);
		assertTrue(weight(front,SerpentRules.STRIKE)>0 && weight(front,SerpentRules.CONSTRICT)>0 && weight(front,SerpentRules.HOOD_SPRAY)>0);
		assertEquals(0,weight(front,SerpentRules.LUNGE));
		double[] back=SerpentRules.weights(4,true,since,false);
		assertEquals(0,weight(back,SerpentRules.STRIKE));
		assertEquals(0,weight(back,SerpentRules.HOOD_SPRAY));
		assertEquals(0,weight(back,SerpentRules.CONSTRICT));
		for(int m:SerpentRules.PICKS) assertTrue(weight(back,SerpentRules.TAIL_SWEEP)>=weight(back,m),"the tail is what it does to anyone at its back");
		double[] far=SerpentRules.weights(12,false,since,false);
		assertTrue(weight(far,SerpentRules.ROOT_LINE)>0 && weight(far,SerpentRules.TAIL_SLAM)>0);
		assertEquals(0,weight(far,SerpentRules.STRIKE));
		assertEquals(0,weight(far,SerpentRules.TAIL_SWEEP));
		for(double w:SerpentRules.weights(16,false,since,false)) assertEquals(0,w);
		assertEquals(-1,SerpentRules.choose(new MoveDeck(5),16,false,since,false,.5));
	}
	@Test void aMoveStillRestingIsNotPickedAndRestsAreShorterRoused(){
		long[] since=rested();
		since[SerpentRules.TAIL_SWEEP]=SerpentRules.SWEEP_REST-1;
		assertEquals(0,weight(SerpentRules.weights(4,true,since,false),SerpentRules.TAIL_SWEEP));
		assertTrue(weight(SerpentRules.weights(4,true,since,true),SerpentRules.TAIL_SWEEP)>0);
		for(int m:SerpentRules.PICKS) assertTrue(SerpentRules.rest(m,true)<=SerpentRules.rest(m,false));
		assertTrue(SerpentRules.rest(SerpentRules.RING_COIL,true)<SerpentRules.rest(SerpentRules.RING_COIL,false));
		assertEquals(0,SerpentRules.rest(SerpentRules.STRIKE,false));
	}
	@Test void itNeverMakesTheSameMoveTwiceRunningAndMixesThemAllUp(){
		var deck=new MoveDeck(5);
		var rng=new Random(11);
		long[] since=rested();
		Map<Integer,Integer> made=new HashMap<>();
		int last=-1;
		for(int i=0;i<300;i++){
			double d=new double[]{3,6,12}[i%3];
			boolean behind=i%4==0 && d<12;
			int m=SerpentRules.choose(deck,d,behind,since,i>150,rng.nextDouble());
			assertNotEquals(-1,m);
			assertNotEquals(last,m,"the same move twice running at "+d);
			made.merge(m,1,Integer::sum);
			deck.played(m);
			last=m;
		}
		assertTrue(made.size()>=6,"only "+made.keySet());
		for(int n:made.values()) assertTrue(n<150,"one move made "+n+" times: "+made);
	}
	@Test void rousedItsBlowsRunOnIntoOneAnother(){
		long[] since=rested();
		assertEquals(-1,SerpentRules.chain(SerpentRules.STRIKE,true,3,false,since,SerpentRules.CHAIN_CHANCE));
		assertEquals(SerpentRules.TAIL_SWEEP,SerpentRules.chain(SerpentRules.STRIKE,true,3,false,since,.1));
		assertEquals(SerpentRules.LUNGE,SerpentRules.chain(SerpentRules.STRIKE,false,7,false,since,.1));
		assertEquals(SerpentRules.STRIKE,SerpentRules.chain(SerpentRules.TAIL_SWEEP,true,4,false,since,.1));
		assertEquals(SerpentRules.STRIKE,SerpentRules.chain(SerpentRules.LUNGE,true,3,false,since,.1));
		assertEquals(-1,SerpentRules.chain(SerpentRules.LUNGE,false,3,false,since,.1));
		assertEquals(SerpentRules.ROOT_LINE,SerpentRules.chain(SerpentRules.TAIL_SLAM,false,10,false,since,.1));
		assertEquals(SerpentRules.TAIL_SWEEP,SerpentRules.chain(SerpentRules.HOOD_SPRAY,false,4,true,since,.1));
		assertEquals(-1,SerpentRules.chain(SerpentRules.RING_COIL,true,3,false,since,.1));
		since[SerpentRules.TAIL_SWEEP]=0;
		assertEquals(-1,SerpentRules.chain(SerpentRules.STRIKE,true,3,false,since,.1));
	}
	@Test void theTailGoesAllTheWayRoundAtAnkleHeight(){
		assertEquals(SerpentRules.SWEEP_START,SerpentRules.tail(1,0),1e-9);
		assertEquals(SerpentRules.SWEEP_START-360,SerpentRules.tail(1,SerpentRules.SWEEP_TICKS),1e-9);
		assertEquals(-SerpentRules.tail(1,3),SerpentRules.tail(-1,3),1e-9);
		assertTrue(SerpentRules.behind(SerpentRules.SWEEP_START),"it starts from behind");
		for(int side=-1;side<=1;side+=2) for(int a=-180;a<180;a+=5){
			boolean swept=false;
			for(int t=1;t<=SerpentRules.SWEEP_TICKS;t++) swept|=SerpentRules.sweeps(side,t,a,4,0);
			assertTrue(swept,"the tail misses "+a+" degrees round");
		}
		for(int t=1;t<=SerpentRules.SWEEP_TICKS;t++) for(int a=-180;a<180;a+=15){
			assertFalse(SerpentRules.sweeps(1,t,a,4,1.1),"a jump clears it");
			assertFalse(SerpentRules.sweeps(1,t,a,SerpentRules.SWEEP_REACH+1,0),"it reaches no further than its tail");
		}
		assertFalse(SerpentRules.sweeps(1,1,0,4,0),"at first it is still behind");
	}
	@Test void theRootsRunOutAlongTheLineTooTallToJump(){
		assertEquals(SerpentRules.ROOT_FROM,SerpentRules.roots(0),1e-9);
		assertEquals(SerpentRules.ROOT_REACH,SerpentRules.roots(999),1e-9);
		assertTrue(SerpentRules.roots(4)<SerpentRules.roots(5));
		assertTrue(SerpentRules.rooted(5,0,0,4.5,5.5));
		assertTrue(SerpentRules.rooted(5,0,1.3,4.5,5.5),"a jump does not clear the roots");
		assertFalse(SerpentRules.rooted(5,SerpentRules.ROOT_HALF+.5,0,4.5,5.5),"a step off the line does");
		assertFalse(SerpentRules.rooted(9,0,0,4.5,5.5),"not yet reached");
		assertFalse(SerpentRules.rooted(2,0,0,4.5,5.5),"already passed");
		assertTrue(SerpentRules.ROOT_STUCK>SerpentRules.ROOT_TELL,"its head stuck fast is an opening");
	}
	@Test void theSpraySweepsAcrossItsFrontAndNeverItsBack(){
		assertEquals(SerpentRules.SPRAY_HALF,SerpentRules.jet(1,0,false),1e-9);
		assertEquals(-SerpentRules.SPRAY_HALF,SerpentRules.jet(1,SerpentRules.SPRAY_TICKS,false),1e-9);
		assertEquals(-SerpentRules.SPRAY_HALF_ROUSED,SerpentRules.jet(-1,0,true),1e-9);
		assertTrue(SerpentRules.SPRAY_HALF_ROUSED>SerpentRules.SPRAY_HALF);
		assertTrue(SerpentRules.sprays(0,5,4,0));
		assertFalse(SerpentRules.sprays(0,40,4,0));
		assertFalse(SerpentRules.sprays(0,0,SerpentRules.SPRAY_REACH+1,0));
		for(int t=0;t<=SerpentRules.SPRAY_TICKS;t++) for(int a=125;a<=235;a+=5)
			assertFalse(SerpentRules.sprays(SerpentRules.jet(1,t,true),a,4,0),"behind it is out of the spray");
		assertTrue(SerpentRules.SPRAY_HITS*SerpentRules.SPRAY_DAMAGE<SerpentRules.STRIKE_DAMAGE);
	}
	@Test void theRingDrawsShutOnAnyoneInsideIt(){
		assertEquals(SerpentRules.RING_R,SerpentRules.ring(SerpentRules.RING_R,0),1e-9);
		assertEquals(0,SerpentRules.ring(SerpentRules.RING_R,SerpentRules.RING_CLOSE),1e-9);
		for(double d=0;d<=SerpentRules.RING_R;d+=.25){
			boolean caught=false;
			for(int t=1;t<=SerpentRules.RING_CLOSE;t++) caught|=SerpentRules.closes(d,SerpentRules.ring(SerpentRules.RING_R,t-1),SerpentRules.ring(SerpentRules.RING_R,t),0);
			assertTrue(caught,"inside at "+d+" it misses");
		}
		for(int t=1;t<=SerpentRules.RING_CLOSE;t++)
			assertFalse(SerpentRules.closes(SerpentRules.RING_R+1.5,SerpentRules.ring(SerpentRules.RING_R,t-1),SerpentRules.ring(SerpentRules.RING_R,t),0),"outside the ring is safe");
		assertTrue(SerpentRules.RING_SET+SerpentRules.RING_LAP>=20,"time to get out");
		assertTrue(SerpentRules.RING_R<SerpentRules.RING-SerpentRules.RING_R,"the ring fits on the island");
	}
	@Test void theRootsItRousesWithCanBeJumped(){
		assertTrue(SerpentRules.crosses(5,4.5,5.2,0));
		assertFalse(SerpentRules.crosses(5,4.5,5.2,1.2));
		assertFalse(SerpentRules.crosses(8,4.5,5.2,0));
		assertTrue(SerpentRules.ROUSE_HEIGHT<1.25 && SerpentRules.ROUSE_REACH<=SerpentRules.RING+1);
		assertTrue(SerpentRules.ROUSE_TELL>SerpentRules.DIVE_TICKS/2 && SerpentRules.ROUSE_TELL<SerpentRules.DIVE_TICKS-4);
	}
	@Test void eachMoveHasItsOwnNameAndWindUp(){
		int[] moves={SerpentRules.WATCH,SerpentRules.STRIKE,SerpentRules.LUNGE,SerpentRules.CONSTRICT,SerpentRules.SQUEEZE,SerpentRules.SIDESTEP,
			SerpentRules.RECOVER,SerpentRules.DIVE,SerpentRules.SUBMERGED,SerpentRules.BURST,SerpentRules.TAIL_SLAM,SerpentRules.TAIL_SWEEP,
			SerpentRules.ROOT_LINE,SerpentRules.HOOD_SPRAY,SerpentRules.RING_COIL,SerpentRules.SPRAWLED,SerpentRules.BURIED};
		var keys=new HashSet<String>();
		for(int m:moves) assertTrue(keys.add(SerpentRules.key(m)),"two moves called "+SerpentRules.key(m));
		for(int t:new int[]{SerpentRules.STRIKE_TELL,SerpentRules.LUNGE_TELL,SerpentRules.TAIL_TELL,SerpentRules.CONSTRICT_TELL,
				SerpentRules.SWEEP_TELL,SerpentRules.ROOT_TELL,SerpentRules.SPRAY_TELL})
			assertTrue(t>=10 && t<=18,"a wind-up of "+t);
		assertTrue(SerpentRules.SPRAWL_TICKS>SerpentRules.LUNGE_TELL,"a missed lunge is an opening");
	}
	@Test void everyMoveItShowsHasANameOnTheBar() throws Exception{
		var lang=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
		for(int m=SerpentRules.WATCH;m<=SerpentRules.BURIED;m++){
			if(SerpentRules.scripted(m)) continue;
			assertTrue(lang.has("boss.wildercord.rootcoil_serpent.move."+SerpentRules.key(m)),SerpentRules.key(m));
		}
	}
	@Test void aBiteReachesAheadAndNotBehindOrWide(){
		assertTrue(SerpentRules.hits(SerpentRules.STRIKE,4,0,1));
		assertTrue(SerpentRules.hits(SerpentRules.STRIKE,5,0,3));
		assertFalse(SerpentRules.hits(SerpentRules.STRIKE,6,0,1));
		assertFalse(SerpentRules.hits(SerpentRules.STRIKE,3,2,1));
		assertFalse(SerpentRules.hits(SerpentRules.STRIKE,-2,0,1));
		assertTrue(SerpentRules.hits(SerpentRules.LUNGE,1,-1,0));
		assertFalse(SerpentRules.hits(SerpentRules.WATCH,1,0,0));
	}
	@Test void itsScalesTurnBlowsFromTheFrontAndItsBellyDoesNot(){
		assertTrue(SerpentRules.frontal(0) && SerpentRules.frontal(-50) && SerpentRules.frontal(410));
		assertFalse(SerpentRules.frontal(90) || SerpentRules.frontal(180) || SerpentRules.frontal(-120));
		assertTrue(SerpentRules.scales(SerpentRules.WATCH,true)<SerpentRules.scales(SerpentRules.WATCH,false));
		assertTrue(SerpentRules.scales(SerpentRules.RECOVER,true)>1);
		assertTrue(SerpentRules.scales(SerpentRules.SUBMERGED,false)<.5);
		assertTrue(SerpentRules.scales(SerpentRules.SPRAWLED,true)>SerpentRules.scales(SerpentRules.RECOVER,true));
		assertTrue(SerpentRules.scales(SerpentRules.BURIED,true)>SerpentRules.scales(SerpentRules.RECOVER,true));
		assertTrue(SerpentRules.scales(SerpentRules.HOOD_SPRAY,true)>1);
		assertTrue(SerpentRules.scales(SerpentRules.DIVE,true)<.5);
	}
	@Test void itSlipsOnlyFrontalBlowsWhileWatchingAndNotTwiceInARow(){
		assertTrue(SerpentRules.slips(SerpentRules.WATCH,true,SerpentRules.SIDESTEP_REST));
		assertFalse(SerpentRules.slips(SerpentRules.WATCH,true,SerpentRules.SIDESTEP_REST-1));
		assertFalse(SerpentRules.slips(SerpentRules.WATCH,false,999));
		assertFalse(SerpentRules.slips(SerpentRules.RECOVER,true,999));
	}
	@Test void itDivesOnceAtHalfHealth(){
		assertFalse(SerpentRules.dives(200,360,false));
		assertTrue(SerpentRules.dives(180,360,false));
		assertFalse(SerpentRules.dives(100,360,true));
	}
	@Test void itsBlowsSitBelowAMastersAndItsOpeningsOutlastItsTells(){
		assertTrue(SerpentRules.STRIKE_DAMAGE<MastersRules.Move.SWEEP.damage);
		assertTrue(SerpentRules.STRIKE_CHAIN_TELL>=SerpentRules.STRIKE_LOCK+3 && SerpentRules.STRIKE_CHAIN_TELL<SerpentRules.STRIKE_TELL);
		assertTrue(SerpentRules.STRIKE_RECOVER>=SerpentRules.STRIKE_TELL && SerpentRules.LUNGE_RECOVER>=SerpentRules.LUNGE_TELL);
		assertTrue(SerpentRules.RING-SerpentRules.LUNGE_REACH>0 && SerpentRules.RING<SerpentRules.ARENA);
		assertEquals("squeeze",SerpentRules.key(SerpentRules.SQUEEZE));
	}
	@Test void aGuardiansBiteGoesPartlyThroughDefences(){
		assertEquals(20,GuardianBite.pierced(20,20,.45),1e-6);
		assertEquals(10+10*.45,GuardianBite.pierced(20,10,.45),1e-4);
		assertEquals(10,GuardianBite.pierced(20,10,0),1e-6);
		assertEquals(10,GuardianBite.pierce(20,10),1e-6);
	}
	@Test void theSlamsClodsFlyChestHighAcrossItsReach(){
		for(double d=SerpentRules.SLAM_AHEAD;d<=SerpentRules.SLAM_REACH-SerpentRules.SLAM_AHEAD;d+=.5){
			double h=SerpentRules.DEBRIS_FROM+SerpentRules.rise(d);
			assertTrue(h>0.3 && h<1.8,"a clod is "+h+" up after "+d+" blocks");
		}
		assertTrue((SerpentRules.SLAM_REACH)/SerpentRules.DEBRIS_SPEED<SerpentRules.DEBRIS_LIFE);
		assertEquals("tail_slam",SerpentRules.key(SerpentRules.TAIL_SLAM));
	}
	@Test void theSlamGetsThroughFullNetheriteProtectionFour(){
		// Netherite (20 armour, 12 toughness) and Protection IV on all four pieces, the way vanilla works them out.
		double dmg=SerpentRules.DEBRIS_DAMAGE;
		double armour=Math.min(20,Math.max(20*.2,20-dmg/(2+12/4.0)))/25;
		double after=dmg*(1-armour)*(1-Math.min(20,16)/25.0);
		double through=GuardianBite.pierced((float)dmg,(float)after,SerpentRules.SLAM_PIERCE);
		assertTrue(through>=dmg*.6,"only "+through+" of "+dmg+" gets through");
		assertTrue(SerpentRules.SLAM_PIERCE>SerpentRules.PIERCE);
	}
	@Test void theCoilrootQuickensTheBreathingStanceAndNothingElse(){
		assertTrue(SerpentRules.COILROOT_BREATH>1);
		assertEquals(2*SerpentRules.COILROOT_BREATH,SerpentRules.coilroot("stance",2),1e-9);
		assertEquals(2*SerpentRules.COILROOT_BREATH,SerpentRules.coilroot("beat",2),1e-9);
		assertEquals(2,SerpentRules.coilroot("hit",2),1e-9);
		assertEquals(SerpentRules.COILROOT_TRICKLE,SerpentRules.coilroot("coilroot",SerpentRules.COILROOT_TRICKLE),1e-9);
		assertTrue(SerpentRules.COILROOT_TRICKLE>0&&SerpentRules.COILROOT_GUARD>=SerpentRules.WAKE_RADIUS);
	}
}
