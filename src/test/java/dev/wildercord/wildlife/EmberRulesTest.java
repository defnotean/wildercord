package dev.wildercord.wildlife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EmberRulesTest {
 @Test void fixedSectorHasThreeDistinctLanes(){assertTrue(EmberRules.lane(0,0,3,0,1,1));assertFalse(EmberRules.lane(0,0,3,0,1,0));assertTrue(EmberRules.lane(1.5,0,2.598,0,1,2));assertTrue(EmberRules.lane(-1.5,0,2.598,0,1,0));assertFalse(EmberRules.lane(0,0,-3,0,1,1));}
 @Test void finiteReachAndVerticalEscape(){assertFalse(EmberRules.lane(0,0,4.01,0,1,1));assertFalse(EmberRules.lane(0,1.41,3,0,1,1));assertFalse(EmberRules.lane(Double.NaN,0,3,0,1,1));assertFalse(EmberRules.lane(0,0,3,0,1,3));}
}
