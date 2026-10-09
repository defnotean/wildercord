package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fresh reconstruction checks; no historical local gate or native image is reused as proof. */
class CrimsonMoonPoseTest {
    @Test void approvedClockOriginalRigMirrorsSolesAndViewMargins() throws Exception {
        CheckCrimsonMoonPose.main(new String[0]);
    }
    @Test void publicBaselineKeepsEverySampleExceptReviewedFirstPersonHandComponents() throws Exception {
        // Independently compiled from immutable public 160ff125 under restored Java 25.
        // Its complete fingerprint remains 837c4514b76867d30ab236e916a6262055206ae011866e8c5b2180c35b51fd08.
        // Canonicalize only ID18's classic windup hand x/yaw/roll; its y/z/pitch and every body/articulated field,
        // the entry weight/clock and ID18 release/recovery remain byte-for-byte protected.
        // ID9 restores only its original yaw/roll during the cut/recoil interpolation; its
        // chamber/follow endpoints and all other components remain directly hashed.
        // Dedicated Red Rain and Void Cut composition tests check the reviewed components.
        assertEquals("9f809f05118af49e7619a1471ad3b3a6d56e2fdb1a04e16f8966ec94e2eb0cea", CheckCrimsonMoonPose.legacyFingerprint(true));
    }
}
