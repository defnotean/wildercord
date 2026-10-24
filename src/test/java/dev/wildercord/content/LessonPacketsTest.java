package dev.wildercord.content;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LessonPacketsTest {
    @Test void aBurstIsAllowedThenTheRestOfTheTickIsRefused() {
        UUID reader = UUID.randomUUID();
        for (int i = 0; i < LessonPackets.BURST; i++) assertTrue(LessonPackets.allow(reader, 100), "Packet " + i + " fits the burst");
        assertFalse(LessonPackets.allow(reader, 100), "The packet after the burst is held back");
    }

    @Test void theAllowanceRefillsWithTime() {
        UUID reader = UUID.randomUUID();
        for (int i = 0; i < LessonPackets.BURST; i++) LessonPackets.allow(reader, 200);
        assertFalse(LessonPackets.allow(reader, 200 + LessonPackets.TICKS_PER_PACKET - 1), "Not refilled before its time");
        assertTrue(LessonPackets.allow(reader, 200 + LessonPackets.TICKS_PER_PACKET), "One more after a refill interval");
        assertFalse(LessonPackets.allow(reader, 200 + LessonPackets.TICKS_PER_PACKET), "Only one");
    }

    @Test void oneReaderNeverSpendsAnothersAllowance() {
        UUID flooder = UUID.randomUUID(), reader = UUID.randomUUID();
        for (int i = 0; i < 50; i++) LessonPackets.allow(flooder, 300);
        assertTrue(LessonPackets.allow(reader, 300), "Another reader turns pages freely");
    }
}
