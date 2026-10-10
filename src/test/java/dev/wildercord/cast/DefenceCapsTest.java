package dev.wildercord.cast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DefenceCapsTest {
 @Test void guardsStopAtResistanceTwo(){assertEquals(1,DefenceCaps.resistance(3,200));assertEquals(1,DefenceCaps.resistance(4,60));assertEquals(1,DefenceCaps.resistance(1,60));assertEquals(0,DefenceCaps.resistance(0,600));}
 @Test void aDodgeKeepsItsLevel(){assertEquals(4,DefenceCaps.resistance(4,DefenceCaps.DODGE_TICKS));assertEquals(1,DefenceCaps.resistance(4,DefenceCaps.DODGE_TICKS+1));}
}
