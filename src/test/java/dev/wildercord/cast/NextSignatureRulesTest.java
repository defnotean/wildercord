package dev.wildercord.cast;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NextSignatureRulesTest {
    @Test void reactedDamageHasOneFinitePostBonusAllowance() {
        var ledger=new NextSignatureRules.Ledger();
        assertEquals(7F,ledger.damage("second_bell",8,7));
        assertEquals(1F,ledger.damage("second_bell",8,1_000));
        assertEquals(0F,ledger.damage("second_bell",8,1_000));
        assertEquals(6F,ledger.damage("red_ledger",6,10));
        assertEquals(0F,ledger.damage("red_ledger",6,Float.NaN));
        var contributions=new NextSignatureRules.Ledger();var first=UUID.randomUUID();var second=UUID.randomUUID();
        assertEquals(2F,contributions.contribution("bell",first,8,4,2,5_000));
        assertEquals(2F,contributions.contribution("bell",first,8,4,2,5_000));
        assertEquals(0F,contributions.contribution("bell",first,8,4,2,5_000));
        assertEquals(2F,contributions.contribution("bell",second,8,4,2,5_000));
        assertEquals(2F,contributions.contribution("bell",second,8,4,2,5_000));
        assertEquals(0F,contributions.contribution("bell",UUID.randomUUID(),8,4,2,5_000));
    }
    @Test void admissionNeverCopiesOrRefills() {
        var ledger=new NextSignatureRules.Ledger();var first=UUID.randomUUID();var second=UUID.randomUUID();
        assertTrue(ledger.once("nullcatch"));assertFalse(ledger.once("nullcatch"));
        assertTrue(ledger.target("ferry",first,2));assertFalse(ledger.target("ferry",first,2));
        assertTrue(ledger.target("ferry",second,2));assertFalse(ledger.target("ferry",UUID.randomUUID(),2));
    }
    @Test void escrowNeedsActualSpareHealthAndShieldCapacity() {
        assertEquals(0F,NextSignatureRules.escrow(4,0));
        assertEquals(1F,NextSignatureRules.escrow(5,0));
        assertEquals(0F,NextSignatureRules.escrow(20,4));
        assertEquals(1F,NextSignatureRules.escrow(20,2));
        assertEquals(0F,NextSignatureRules.escrow(Float.NaN,0));
    }
    @Test void ferrySpendsMissingHealthOnly() {
        assertEquals(0F,NextSignatureRules.ferry(20,20,6));
        assertEquals(1F,NextSignatureRules.ferry(19,20,6));
        assertEquals(2F,NextSignatureRules.ferry(1,20,2));
        assertEquals(0F,NextSignatureRules.ferry(1,20,Float.POSITIVE_INFINITY));
    }
    @Test void movementIsCounterplayAndTeleportDoesNotMultiplyCuts() {
        assertFalse(NextSignatureRules.escapedBell(.6,.6));assertTrue(NextSignatureRules.escapedBell(1.1,0));
        assertFalse(NextSignatureRules.ledgerStep(.3,0));assertTrue(NextSignatureRules.ledgerStep(1,0));
        assertFalse(NextSignatureRules.ledgerStep(40,0));assertFalse(NextSignatureRules.ledgerStep(Double.NaN,0));
        assertFalse(NextSignatureRules.crouchEdge(true,true));assertTrue(NextSignatureRules.crouchEdge(false,true));
    }
    @Test void breadcrumbAndTowAreFinite() {
        assertEquals(0,NextSignatureRules.tow(Double.NaN));assertEquals(0,NextSignatureRules.tow(7));
        assertTrue(NextSignatureRules.tow(5)<=.18);assertEquals(0,NextSignatureRules.gradedCrumb(Double.POSITIVE_INFINITY));
        assertTrue(NextSignatureRules.gradedCrumb(1)>NextSignatureRules.gradedCrumb(8));
    }
    @Test void matchingTargetCannotOverrideForeignThrowerReservation() {
        var player=UUID.randomUUID();var foreign=UUID.randomUUID();
        assertTrue(NextSignatureRules.reservedFor(player,null,null));
        assertTrue(NextSignatureRules.reservedFor(player,player,player));
        assertFalse(NextSignatureRules.reservedFor(player,player,foreign));
        assertFalse(NextSignatureRules.reservedFor(player,foreign,player));
        assertFalse(NextSignatureRules.reservedFor(null,null,null));
    }
    @Test void fourAdmittedScreensStillBuyOnlyOneProjectile() {
        var ledger=new NextSignatureRules.Ledger();float intercepted=0;
        for(int ally=0;ally<4;ally++){
            assertTrue(ledger.target("nullcatch",UUID.randomUUID(),4));
            intercepted+=ledger.resource("nullcatch_capture",1,1);
        }
        assertEquals(1F,intercepted);
        assertEquals(0F,ledger.resource("nullcatch_capture",1,5_000));
        assertFalse(ledger.target("nullcatch",UUID.randomUUID(),4));
    }
    @Test void invalidAdmissionCapsCannotPoisonLaterValidDamage() {
        var ledger=new NextSignatureRules.Ledger();var target=UUID.randomUUID();
        assertEquals(0F,ledger.contribution("bell",target,Double.POSITIVE_INFINITY,4,2,5_000));
        assertEquals(0F,ledger.contribution("bell",target,8,Double.NaN,2,5_000));
        assertEquals(0F,ledger.contribution("bell",target,8,4,-1,5_000));
        assertEquals(8,ledger.left("bell",8));
        assertEquals(2F,ledger.contribution("bell",target,8,4,2,5_000));
    }
    @Test void additivePushUsesDeltaAndNeverDoublesCurrentMotion() {
        double velocity=.16;
        for(int tick=0;tick<100;tick++){
            double desired=Math.min(.32,velocity+.05);
            double impulse=NextSignatureRules.velocityImpulse(velocity,desired);
            assertEquals(desired,velocity+impulse,1e-12);
            double resistant=velocity+impulse*.4;
            assertTrue(resistant<=.32 && resistant>=velocity);
            velocity+=impulse;assertTrue(velocity<=.32);
        }
        assertEquals(0,NextSignatureRules.velocityImpulse(Double.NaN,.2));
        assertEquals(0,NextSignatureRules.velocityImpulse(-Double.MAX_VALUE,Double.MAX_VALUE));
    }
}
