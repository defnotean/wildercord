package dev.wildercord.gametest;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Pure identity/order predicates. Files are genuine server-hook receipts, never synthetic render input. */
public final class CrimsonMoonMultiplayerProof {
    private CrimsonMoonMultiplayerProof() {}
    public static boolean boundedTimeout(int seconds) { return seconds>=60&&seconds<=180; }
    public record Release(String sha256,long accepted,long released) {}
    public static Release readRelease(Path path,Map<String,String> expected) {
        try {
            byte[] bytes=Files.readAllBytes(path);var receipt=new Properties();receipt.load(new ByteArrayInputStream(bytes));
            for(var field:expected.entrySet())require(field.getValue().equals(receipt.getProperty(field.getKey())),"Release receipt identity: "+field.getKey());
            long accepted=Long.parseLong(receipt.getProperty("acceptedTick")),released=Long.parseLong(receipt.getProperty("releaseTick"));
            require(CrimsonMoonRenderMath.releaseAt(accepted,released),"Actual completion must be exactly acceptance+10");
            require("10".equals(receipt.getProperty("completionOffset"))&&"FULL,FULL,FULL,LOW".equals(receipt.getProperty("marks")),"Actual completed Final marks and offset");
            return new Release(ArticulatedRenderReceipt.sha256(bytes),accepted,released);
        } catch(java.io.IOException failure) { throw new AssertionError("Missing causal server release receipt",failure); }
    }
    public static boolean remoteIdentity(String actor,int actorEntity,String observer,int observerEntity,
        String local,int localEntity,String source,int sourceEntity,boolean sameLevel,boolean remoteBody,boolean ownCamera) {
        return actor!=null&&observer!=null&&!actor.equals(observer)&&actorEntity!=observerEntity
            &&observer.equals(local)&&observerEntity==localEntity&&actor.equals(source)&&actorEntity==sourceEntity
            &&sameLevel&&remoteBody&&ownCamera;
    }
    public static boolean sourceAfterRelease(long readSequence,long sourceSequence,long extractedSequence) {
        return readSequence>0&&sourceSequence>readSequence&&extractedSequence>sourceSequence;
    }
    public static boolean damageAfterBothImages(long accepted,long released,long damaged,boolean hostPersisted,boolean peerPersisted) {
        return CrimsonMoonRenderMath.releaseAt(accepted,released)&&damaged>=accepted+11&&damaged<=accepted+15&&hostPersisted&&peerPersisted;
    }
    /** Causal interval evidence cannot claim equality between the independent client and server clocks. */
    public static boolean orderClaim(long accepted,long released,long damaged,long damageAgeTicks,long damageDelayTicks,
        boolean releaseImageDamageOrderVerified,boolean serverReleaseFrameCorrespondenceVerified) {
        return releaseImageDamageOrderVerified&&!serverReleaseFrameCorrespondenceVerified
            &&CrimsonMoonRenderMath.releaseAt(accepted,released)&&damaged>=accepted+11&&damaged<=accepted+15
            &&damageAgeTicks==damaged-accepted&&damageDelayTicks==damaged-released
            &&damageAgeTicks>=11&&damageAgeTicks<=15&&damageDelayTicks>=1&&damageDelayTicks<=5;
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
