package dev.wildercord.content;

import org.junit.jupiter.api.Test;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class RelayLessonRetirementTest {
    @Test void invalidBodiesAreRemovedBeforeTheirOriginalReadingIsReported() {
        Object live=new Object(), expired=new Object(), cracked=new Object();
        Map<Object,String> readings=new IdentityHashMap<>();
        readings.put(live,"live nonce");readings.put(expired,"expired nonce");readings.put(cracked,"cracked nonce");
        Map<Object,String> reported=new IdentityHashMap<>();
        assertDoesNotThrow(()->RelayLesson.removeInvalid(readings,(body,reading)->body==live,(body,reading)->{
            assertFalse(readings.containsKey(body),"Observers must never see the retired session as active");
            assertNull(reported.put(body,reading),"Each original body is interrupted once");
        }));
        assertEquals(Map.of(expired,"expired nonce",cracked,"cracked nonce"),reported);
        assertEquals("live nonce",readings.get(live));assertEquals(1,readings.size());
        RelayLesson.removeInvalid(readings,(body,reading)->true,(body,reading)->fail("Already-retired session notified again"));
    }

    @Test void distinctButEqualBodiesNeverExchangeSessions() {
        String oldBody=new String("same UUID"), replacement=new String("same UUID");
        Map<String,Long> readings=new IdentityHashMap<>();readings.put(oldBody,41L);readings.put(replacement,42L);
        RelayLesson.removeInvalid(readings,(body,nonce)->body==replacement,(body,nonce)->{
            assertSame(oldBody,body);assertEquals(41L,nonce);assertFalse(readings.containsKey(oldBody));
        });
        assertEquals(1,readings.size());assertEquals(42L,readings.get(replacement));
    }

    @Test void manyRetirementsAndExplicitCloseDisconnectOrClearLeaveNoStaleNotifications() {
        Map<Object,Integer> readings=new IdentityHashMap<>();
        for(int i=0;i<256;i++)readings.put(new Object(),i);
        Object closed=new Object(),disconnected=new Object();readings.put(closed,-1);readings.put(disconnected,-2);
        readings.remove(closed);readings.remove(disconnected);
        Set<Object> reported=java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        RelayLesson.removeInvalid(readings,(body,nonce)->false,(body,nonce)->{
            assertTrue(nonce>=0);assertTrue(reported.add(body));assertFalse(readings.containsKey(body));
        });
        assertTrue(readings.isEmpty());assertEquals(256,reported.size());
        readings.put(new Object(),7);readings.clear();
        RelayLesson.removeInvalid(readings,(body,nonce)->false,(body,nonce)->fail("Cleared server state must remain silent"));
    }
}
