package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fresh reconstruction checks; no historical local gate or native image is reused as proof. */
class CrimsonMoonPoseTest {
    @Test void approvedClockOriginalRigMirrorsSolesAndViewMargins() throws Exception {
        CheckCrimsonMoonPose.main(new String[0]);
    }
    @Test void publicBaselinePlayerIdsZeroThroughEighteenKeepExactSamples() throws Exception {
        // Independently compiled from immutable public 160ff125 under restored Java 25.
        assertEquals("837c4514b76867d30ab236e916a6262055206ae011866e8c5b2180c35b51fd08", CheckCrimsonMoonPose.legacyFingerprint());
    }
}
