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
