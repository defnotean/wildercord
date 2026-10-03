package dev.wildercord.wildlife;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class HabitatSweepTest {
	@Test void sweepVisitsEveryColumnOnceInNearestOrder() {
		for(int radius:new int[]{4,6}) {
			var sweep=new HabitatSweep(radius);sweep.anchor(0,100,0);var seen=new HashSet<HabitatSweep.Offset>();int last=-1;
			for(HabitatSweep.Offset o;(o=sweep.next())!=null;) {assertTrue(seen.add(o));assertTrue(Math.abs(o.x())<=radius && Math.abs(o.z())<=radius);int distance=o.x()*o.x()+o.z()*o.z();assertTrue(distance>=last);last=distance;}
			assertEquals((radius*2+1)*(radius*2+1),seen.size());assertNull(sweep.next());
		}
	}
	@Test void smallWanderingDoesNotStarveLaterColumns() {
		var sweep=new HabitatSweep(4);sweep.anchor(0,100,0);sweep.next();sweep.anchor(1,100,1);
		assertEquals(0,sweep.x());assertEquals(0,sweep.z());assertNotEquals(new HabitatSweep.Offset(0,0),sweep.next());
		sweep.anchor(8,100,8);assertEquals(8,sweep.x());assertEquals(new HabitatSweep.Offset(0,0),sweep.next());
		sweep.anchor(8,103,8);assertEquals(103,sweep.y());assertEquals(new HabitatSweep.Offset(0,0),sweep.next());
	}
}
