package dev.wildercord.wildlife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public final class MossveilRulesTest{
 @Test void finiteTrimNeverCuresOrExceedsQuarter(){for(int duration:new int[]{0,1,7,8,20,239,240,600,Integer.MAX_VALUE}){int n=MossveilRules.trim(duration);assertTrue(n>=0&&n<=60&&n<=duration/4);if(duration>0)assertTrue(duration-n>0);}assertEquals(0,MossveilRules.trim(-1));assertEquals(60,MossveilRules.trim(600));}
 @Test void ClaimBoundaryDoesNotRenewItself(){assertFalse(MossveilRules.expired(5999,6000));assertTrue(MossveilRules.expired(6000,6000));assertTrue(MossveilRules.expired(7000,6000));}
 @Test void SlowCreepUsesOriginalAnchor(){assertTrue(MossveilRules.planted(.005*.005));assertFalse(MossveilRules.planted(.2*.2));assertFalse(MossveilRules.planted(Double.NaN));assertFalse(MossveilRules.planted(Double.POSITIVE_INFINITY));}
}
