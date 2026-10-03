package dev.wildercord.cast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FieldFusionRulesTest {
    @Test void mercyCannotHealFromNothingOrExceedMissingHealth() {
        assertEquals(0,FieldFusionRules.mercyHealing(0,false,20));
        assertEquals(2,FieldFusionRules.mercyHealing(0,true,20));
        assertEquals(4,FieldFusionRules.mercyHealing(100,true,20));
        assertEquals(1,FieldFusionRules.mercyHealing(100,true,1));
        assertEquals(0,FieldFusionRules.mercyHealing(-4,false,20));
        assertEquals(0,FieldFusionRules.mercyHealing(2,true,Float.NaN));
    }
    @Test void clockrootHasAHardHorizontalThreshold() {
        assertFalse(FieldFusionRules.escaped(2,0));
        assertTrue(FieldFusionRules.escaped(2.01,0));
        assertTrue(FieldFusionRules.escaped(1.5,1.5));
        assertFalse(FieldFusionRules.escaped(Double.NaN,0));
    }
    @Test void skylatchCorrectionIsFiniteAndBounded() {
        assertEquals(0,FieldFusionRules.hoverVelocity(100,100));
        assertEquals(.10,FieldFusionRules.hoverVelocity(0,100));
        assertEquals(-.10,FieldFusionRules.hoverVelocity(200,100));
        assertEquals(0,FieldFusionRules.hoverVelocity(Double.NaN,100));
    }
    @Test void sieveOnlyProcessesWholeOutputsWithinPaidCapacity() {
        assertEquals(0,FieldFusionRules.sieveUnits(64,2,1,16));
        assertEquals(2,FieldFusionRules.sieveUnits(64,2,5,16));
        assertEquals(16,FieldFusionRules.sieveUnits(64,1,64,64));
        assertEquals(3,FieldFusionRules.sieveUnits(64,1,64,3));
        assertEquals(0,FieldFusionRules.sieveUnits(64,0,64,16));
    }
}
