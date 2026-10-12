package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SerpentSpineTest {
	private static final double[] LANDING={0,0,5.5};

	private static void chain(double[][] joints){
		assertEquals(SerpentSpine.JOINTS,joints.length);
		for(int i=1;i<joints.length;i++){
			double d=SerpentSpine.distance(joints[i-1],joints[i]);
			// Joints sit SEG apart along the curve, so the straight line across the reared neck's tight S is a little shorter.
			assertTrue(d<=SerpentSpine.SEG+1e-6 && d>SerpentSpine.SEG*.75,"joint "+i+" is "+d+" from the last");
		}
	}

	private static void spaced(double[][] joints,int count){
		assertEquals(count,joints.length);
		for(int i=1;i<joints.length;i++){
			double d=SerpentSpine.distance(joints[i-1],joints[i]);
			assertTrue(d<=SerpentSpine.SEG+1e-6 && d>SerpentSpine.SEG*.6,"joint "+i+" is "+d+" from the last");
		}
	}
	private static final int[] MOVES={SerpentRules.WATCH,SerpentRules.STRIKE,SerpentRules.LUNGE,SerpentRules.CONSTRICT,SerpentRules.SQUEEZE,
		SerpentRules.SIDESTEP,SerpentRules.RECOVER,SerpentRules.DIVE,SerpentRules.SUBMERGED,SerpentRules.BURST,SerpentRules.TAIL_SLAM};

	@Test void itsBodyIsFarThickerThanAPlayerAndItsCoilClearsTheTrunk(){
		assertTrue(SerpentSpine.GIRTH*2>1.5);
		assertTrue(SerpentSpine.COIL_TOP_R-SerpentSpine.GIRTH*1.15>=1.9);
	}
	@Test void everyPoseKeepsItsNeckItsOwnLengthAndItsHeadOffTheGround(){
		double rest=SerpentSpine.length(SerpentSpine.dense(SerpentSpine.FIGHT_NECK));
		for(int move:MOVES)for(float el=0;el<=30;el+=1){
			var pose=SerpentSpine.pose(move,el,6);
			assertEquals(4,pose.neck().length);
			assertArrayEquals(SerpentSpine.BASE,pose.neck()[3],1e-9);
			double l=SerpentSpine.length(SerpentSpine.dense(pose.neck()));
			assertTrue(l>rest-1.2 && l<rest+1.2,SerpentRules.key(move)+" stretches its neck to "+l);
			assertTrue(pose.neck()[0][1]>=.6,SerpentRules.key(move)+" puts its head in the ground");
			var neck=SerpentSpine.neck(pose.neck());
			assertEquals(SerpentSpine.neckJoints(),neck.length);
			assertArrayEquals(pose.neck()[0],neck[0],1e-9);
			assertArrayEquals(SerpentSpine.BASE,neck[neck.length-1],1e-6,SerpentRules.key(move)+" stops short of its base or runs past it");
			for(int i=1;i<neck.length;i++){double d=SerpentSpine.distance(neck[i-1],neck[i]);assertTrue(d>.55 && d<.95,SerpentRules.key(move)+" spaces its neck unevenly: "+d);}
		}
	}
	@Test void aBiteThrowsItsHeadForwardFromWhereItWasDrawn(){
		var drawn=SerpentSpine.pose(SerpentRules.STRIKE,0,4);
		var bite=SerpentSpine.pose(SerpentRules.STRIKE,4,4);
		assertTrue(bite.neck()[0][2]-drawn.neck()[0][2]>=3.5);
		assertTrue(bite.pitch()>drawn.pitch() && drawn.jaw()>bite.jaw());
		assertTrue(SerpentRules.hits(SerpentRules.STRIKE,bite.neck()[0][2],0,bite.neck()[0][1]));
	}
	@Test void theSqueezeWindsRoundItsPrey(){
		double[][] wrap=SerpentSpine.wrap();
		spaced(wrap,SerpentSpine.WRAP+1);
		assertArrayEquals(SerpentSpine.BASE,wrap[0],1e-9);
		int round=0;
		for(int i=1;i<wrap.length;i++)if(Math.abs(Math.hypot(wrap[i][0],wrap[i][2]-SerpentSpine.GRIP)-1.45)<.3)round++;
		assertTrue(round>=wrap.length/2,"only "+round+" joints wind round");
	}
	@Test void theSlamLiftsTheTailHighThenBringsItDownInFront(){
		int count=SerpentSpine.JOINTS-SerpentSpine.neckJoints()+1;
		for(float el=0;el<=SerpentRules.TAIL_TELL;el+=.5F){
			double[][] body=SerpentSpine.slam(el,SerpentRules.TAIL_TELL);
			spaced(body,count);
			for(double[] j:body)assertTrue(j[1]>-.01,"the slam's body goes into the ground at "+el);
		}
		double top=0;
		for(double[] j:SerpentSpine.slam(SerpentRules.TAIL_TELL*.65F,SerpentRules.TAIL_TELL))top=Math.max(top,j[1]);
		assertTrue(top>=5,"the lifted tail only reaches "+top);
		boolean struck=false;
		for(double[] j:SerpentSpine.slam(SerpentRules.TAIL_TELL,SerpentRules.TAIL_TELL))
			struck|=j[1]<1 && Math.abs(j[0])<2 && Math.abs(j[2]-SerpentRules.SLAM_AHEAD)<SerpentRules.SHOCK_RADIUS;
		assertTrue(struck,"the tail never comes down where the shock lands");
	}
	@Test void theBodyIsThirtyBlocksAndSwellsBehindTheNeck(){
		assertEquals(30,SerpentSpine.LENGTH,1e-9);
		assertTrue(SerpentSpine.radius(SerpentSpine.JOINTS/3)>SerpentSpine.radius(0));
		assertTrue(SerpentSpine.radius(SerpentSpine.JOINTS-1)<SerpentSpine.radius(0)/3);
	}
	@Test void theCoilIsOneUnbrokenChainWoundRoundTheTrunkAndOffTheGround(){
		double[][] coiled=SerpentSpine.coiled();
		chain(coiled);
		for(int i=0;i<coiled.length;i++){
			assertTrue(coiled[i][1]>=0,"joint "+i+" is under the ground");
			assertTrue(Math.hypot(coiled[i][0],coiled[i][2])>=SerpentSpine.COIL_TOP_R-.3,"joint "+i+" is inside the trunk");
		}
		assertArrayEquals(SerpentSpine.SLEEP_NECK[0],coiled[0],1e-6);
	}
	@Test void theUncoilStaysAChainAndNeverJumps(){
		double[][] last=SerpentSpine.uncoil(0,LANDING);
		assertArrayEquals(SerpentSpine.coiled()[0],last[0],1e-6);
		for(int step=1;step<=200;step++){
			double[][] now=SerpentSpine.uncoil(step/200F,LANDING);
			chain(now);
			for(int i=0;i<now.length;i++)assertTrue(SerpentSpine.distance(last[i],now[i])<.6,"joint "+i+" jumps at step "+step);
			last=now;
		}
	}
	@Test void itLandsRearedAtItsLandingFacingAwayFromTheTrunk(){
		double[][] landed=SerpentSpine.uncoil(1,LANDING);
		double[][] neck=SerpentSpine.fightNeck();
		for(int i=0;i<neck.length;i++)assertArrayEquals(new double[]{LANDING[0]+neck[i][0],LANDING[1]+neck[i][1],LANDING[2]+neck[i][2]},landed[i],1e-6);
		assertTrue(landed[0][2]>LANDING[2]);
	}
	@Test void framesTurnAndTurnBack(){
		for(float yaw:new float[]{0,37,-120,180}){
			double[] p={1.5,2,-3},w=SerpentSpine.toWorld(p,yaw),back=SerpentSpine.fromWorld(w,yaw);
			assertArrayEquals(p,back,1e-9);
			assertEquals(Math.hypot(p[0],p[2]),Math.hypot(w[0],w[2]),1e-9);
		}
		double[] ahead=SerpentSpine.toWorld(new double[]{0,0,1},90);
		assertEquals(-1,ahead[0],1e-9);
		assertEquals(0,ahead[2],1e-9);
	}
}
