package dev.wildercord.wildlife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TidewardPlaybackTest {
 @Test void frozenServerClockStillHasExactlyTenFiniteLocalBursts(){var play=new TidewardPlayback(50,100);int bursts=0;long stalledServerAge=0;for(long local=50;local<250;local++){assertEquals(0,stalledServerAge);if(play.burst(local))bursts++;}assertEquals(10,bursts);assertFalse(play.alive(150));}
 @Test void repeatedSameLocalAgeCannotEmitTwice(){var play=new TidewardPlayback(0,100);assertTrue(play.burst(0));for(int n=0;n<128;n++)assertFalse(play.burst(0));assertFalse(play.burst(9));assertTrue(play.burst(10));assertFalse(play.burst(10));}
 @Test void unknownPastOrExpiredPlaybackDoesNotCatchUp(){var play=new TidewardPlayback(10,100);assertFalse(play.burst(9));assertTrue(play.burst(30));assertFalse(play.burst(110));assertFalse(play.burst(200));}
 @Test void finiteBoundsAreValidated(){assertThrows(IllegalArgumentException.class,()->new TidewardPlayback(-1,100));assertThrows(IllegalArgumentException.class,()->new TidewardPlayback(0,101));assertThrows(IllegalArgumentException.class,()->new TidewardPlayback(0,0));}
 @Test void delayedAdmissionKeepsOriginalExpiryAndExactlyTenBursts(){var play=new TidewardPlayback(0,100);assertTrue(play.firstBurst(3));assertFalse(play.firstBurst(3));int bursts=1;for(long local=3;local<140;local++)if(play.burst(local))bursts++;assertEquals(10,bursts);assertFalse(play.alive(100));assertFalse(play.firstBurst(100));}
}
