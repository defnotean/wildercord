package dev.wildercord.aura;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static dev.wildercord.aura.ArticulatedCombatPose.*;

/** Standalone pure-source recovery check. This is not Minecraft rendering or native gameplay evidence. */
public final class CheckCrimsonMoonPose {
    private static int checks;
    private static float nearest = Float.NEGATIVE_INFINITY;
    private static final float EPS = .001F;
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--legacy-fingerprint")) {
            System.out.println(legacyFingerprint()); return;
        }
        var rule = MastersStyleRules.of("crimson_moon");
        require(rule != null && rule.animation() == 19 && rule.windup() == 10 && rule.recovery() == 20, "Approved fixed release");
        require(rule.targets() == MastersStyleRules.TargetPolicy.ACTIVE_CONE && MastersArtRules.move(19) == null, "Cone policy without a shared input");
        require(ArtRules.art(rule.art()).slot() == 4 && ArtRules.art(rule.art()).cost() == 40 && ArtRules.art(rule.art()).cooldown() == 600, "Existing Final economy");
        require(supportsPlayer(19) && !supportsMaster(19) && MastersArtAnimation.supports(19), "Player-only presentation ID");
        int unassignedPlayerId = MastersStyleRules.STYLES.stream().mapToInt(MastersStyleRules.Style::animation).max().orElse(2) + 1;
        require(!supportsPlayer(unassignedPlayerId) && !MastersArtAnimation.supports(unassignedPlayerId), "New invalid sentinel");
        require(Joint.values().length == 20, "Original twenty-joint topology");
        for (float age = 0; age <= 30; age += .125F) {
            Pose right = sample(age, false), left = sample(age, true);
            Phase expected = age == 30 ? Phase.NONE : age < 10 ? Phase.WINDUP : age < 11 ? Phase.ACTIVE : Phase.RECOVERY;
            require(right.phase() == expected && left.phase() == expected, "One release phase at " + age);
            ViewPose rv = view(right, false), lv = view(left, true);
            require(rv.phase() == expected && lv.phase() == expected, "Independent view clock at " + age);
            for (boolean slim : new boolean[] {false,true}) {
                float centre = slim ? .5F : 1F;
                Matrix r = right.world(Joint.RIGHT_HAND).multiply(new Transform(-centre,0,0,Rotation.ZERO).matrix()).multiply(right.local(Joint.RIGHT_SOCKET).matrix());
                Matrix l = left.world(Joint.LEFT_HAND).multiply(new Transform(centre,0,0,Rotation.ZERO).matrix()).multiply(left.local(Joint.LEFT_SOCKET).matrix());
                mirrored(r.transform(0,0,0),l.transform(0,0,0));
                Matrix vr = rv.world(Joint.RIGHT_HAND).multiply(new Transform(-centre,0,0,Rotation.ZERO).matrix()).multiply(rv.local(Joint.RIGHT_SOCKET).matrix());
                Matrix vl = lv.world(Joint.LEFT_HAND).multiply(new Transform(centre,0,0,Rotation.ZERO).matrix()).multiply(lv.local(Joint.LEFT_SOCKET).matrix());
                mirrored(vr.transform(0,0,0),vl.transform(0,0,0));
            }
            for (Joint joint : Joint.values()) {
                rigid(right.world(joint)); rigid(left.world(joint)); rigid(rv.world(joint)); rigid(lv.world(joint));
                for (Vec3 point : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
                    mirrored(right.world(joint).transform(point), left.world(joint.opposite()).transform(-point.x(), point.y(), point.z()));
                    mirrored(rv.world(joint).transform(point), lv.world(joint.opposite()).transform(-point.x(), point.y(), point.z()));
                }
            }
            for (boolean handed : new boolean[] {false, true}) {
                Pose body = handed ? left : right; ViewPose camera = handed ? lv : rv;
                for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT})
                    for (float x : new float[] {-2, 2}) for (float z : new float[] {-2, 2})
                        near(body.world(foot).transform(x, 2, z).y(), 24, EPS, "Grounded sole");
                for (Joint wrist : new Joint[] {Joint.RIGHT_HAND, Joint.LEFT_HAND}) {
                    Rotation r = body.local(wrist).rotation(), v = camera.local(wrist).rotation();
                    require(Math.abs(r.x()) <= .181 && Math.abs(r.y()) <= .151 && Math.abs(r.z()) <= .181, "Body wrist bounded");
                    require(Math.abs(v.x()) <= .181 && Math.abs(v.y()) <= .151 && Math.abs(v.z()) <= .181, "View wrist bounded");
                }
                for (Joint grip : new Joint[] {handed ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET, handed ? Joint.RIGHT_HAND : Joint.LEFT_HAND}) {
                    Vec3 p = camera.cameraPoint(grip, 0, 0, 0);
                    double screenY = .5 - p.y() / -p.z() / (2 * Math.tan(Math.toRadians(35)));
                    require(screenY > .6 && screenY < .81, "Grip below aim and above HUD at " + age + ": " + screenY);
                    for (int[] viewport : new int[][] {{854,480,2},{1280,720,3},{1280,960,4},{1920,810,3}})
                        require(screenY * viewport[1] < viewport[1] - 42 * viewport[2], "Conservative HUD band");
                }
                skinDepth(camera);
            }
            var classic = MastersArtAnimation.sample(19, age, 10, 20);
            require(classic.weight() >= 0 && classic.weight() <= 1 && Float.isFinite(classic.hand().pitch()), "Classic finite clock");
            if (age >= 6.5F && age <= 14) require(classic.weight() == 1 && right.weight() == 1, "Full chamber/release/follow weight");
        }
        Vec3 chamber = sample(6.5F, false).socket(false).transform(0,0,0);
        Vec3 release = sample(10, false).socket(false).transform(0,0,0);
        Vec3 follow = sample(14, false).socket(false).transform(0,0,0);
        require(chamber.x() < 0 && release.x() > 0 && release.x() > chamber.x() + 4, "Outside hip crosses into one sweep");
        require(follow.x() > 0, "Far-side follow-through");
        require(!sample(10, false).local(Joint.RIGHT_UPPER_ARM).equals(view(sample(10, false), false).local(Joint.RIGHT_UPPER_ARM)), "Independently composed view");
        for (float boundary : new float[] {0,6.5F,10,11,14,30}) for (boolean left : new boolean[] {false,true}) {
            Pose before = sample(boundary - .0001F, left), after = sample(boundary + .0001F, left);
            for (Joint joint : Joint.values()) {
                close(before.world(joint), after.world(joint), .025F);
                close(view(before,left).world(joint), view(after,left).world(joint), .025F);
            }
        }
        for (float invalid : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 30, 80})
            require(sample(invalid,false) == NONE && MastersArtAnimation.sample(19, invalid,10,20) == MastersArtAnimation.NONE, "Invalid/expired neutral");
        System.out.println("PURE_SOURCE_PASS checks=" + checks + " nearestSkinCameraZ=" + nearest + " legacy=" + legacyFingerprint());
        System.out.println("NOT_NATIVE: item sprites, armor/shell mesh, pixels, input, owner lifecycle and gameplay require separate fresh gates.");
    }
    private static Pose sample(float age, boolean left) { return samplePlayer(19, age, 10, 20, left); }
    private static void skinDepth(ViewPose camera) {
        for (boolean slim : new boolean[] {false,true}) for (boolean left : new boolean[] {false,true}) {
            float loX = left ? -1 : slim ? -2 : -3, hiX = loX + (slim ? 3 : 4);
            Joint[] joints = left ? new Joint[] {Joint.LEFT_UPPER_ARM,Joint.LEFT_FOREARM,Joint.LEFT_HAND}
                : new Joint[] {Joint.RIGHT_UPPER_ARM,Joint.RIGHT_FOREARM,Joint.RIGHT_HAND};
            for (int i=0;i<3;i++) for(float x:new float[]{loX-.25F,hiX+.25F})
                for(float y:new float[]{i==0?-2.25F:-.25F,i==2?2.25F:4.25F}) for(float z:new float[]{-2.25F,2.25F}) {
                    Vec3 p=camera.cameraPoint(joints[i],x,y,z),o=camera.origin();
                    for(float lookY:new float[]{-180,-25,0,25,180}) for(float lookP:new float[]{-90,-20,0,20,90}) {
                        float yaw=Math.max(-25,Math.min(25,lookY))*camera.weight(), pitch=Math.max(-20,Math.min(20,lookP))*camera.weight();
                        double a=Math.toRadians(-pitch),b=Math.toRadians(-yaw),px=p.x()-o.x(),py=p.y()-o.y(),pz=p.z()-o.z();
                        double rz=Math.sin(a)*py+Math.cos(a)*pz;
                        float cameraZ=(float)(-Math.sin(b)*px+Math.cos(b)*rz+o.z()-viewClearance(yaw,pitch));
                        nearest=Math.max(nearest,cameraZ); require(cameraZ < -.05F, "Skin/sleeve clears bounded free-look near plane");
                    }
                }
        }
    }
    private static void mirrored(Vec3 a,Vec3 b) { near(-a.x(),b.x(),EPS,"Mirrored x");near(a.y(),b.y(),EPS,"Mirrored y");near(a.z(),b.z(),EPS,"Mirrored z"); }
    private static void rigid(Matrix m) {
        for(float value:m.values()) require(Float.isFinite(value),"Finite matrix");
        close(Matrix.identity(),m.multiply(m.inverseRigid()),EPS);
        float first=m.get(0,0);m.values()[0]=Float.NaN;require(m.get(0,0)==first,"Immutable matrix storage");
    }
    private static void close(Matrix a,Matrix b,float epsilon) { for(int r=0;r<4;r++) for(int c=0;c<4;c++)near(a.get(r,c),b.get(r,c),epsilon,"Matrix continuity/rigidity"); }
    private static void near(float a,float b,float e,String reason) { require(Math.abs(a-b)<=e,reason+": "+a+" vs "+b); }
    private static void require(boolean value,String reason) { checks++; if(!value)throw new AssertionError(reason); }
    static String legacyFingerprint() throws Exception { return legacyFingerprint(false); }
    /** Normalize only reviewed first-person components to their immutable baseline representation. */
    static String legacyFingerprint(boolean normalizeReviewedHands) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(int move=0;move<=18;move++) {
            var style=MastersStyleRules.animation(move); int tell=style==null?MastersArtRules.move(move).windup():style.windup();
            int recovery=style==null?MastersArtRules.move(move).recovery():style.recovery();
            for(int step=0;step<=(tell+recovery)*8;step++) {
                float age=step/8F;
                var classic=MastersArtAnimation.sample(move,age,tell,recovery);
                if(normalizeReviewedHands && move==18 && age<tell) classic=new MastersArtAnimation.Pose(classic.weight(),classic.body(),
                    classic.head(),classic.sword(),classic.guard(),classic.frontLeg(),classic.rearLeg(),classic.lower(),classic.forward(),
                    new MastersArtAnimation.Hand(0,classic.hand().y(),classic.hand().z(),classic.hand().pitch(),0,0));
                // Restore only the two reviewed Void Cut wrist components inside their changed
                // interpolation interval. Keep the existing immutable baseline hash: every other
                // value, plus both untouched hand endpoints, still enters the digest unchanged.
                if(normalizeReviewedHands && move==9 && age>tell*.65F && age<tell+Math.min(4,recovery*.25F)) {
                    float t=age<tell ? MastersArtAnimation.smooth((age-tell*.65F)/(tell-tell*.65F))
                        : MastersArtAnimation.smooth((age-tell)/Math.min(4,recovery*.25F));
                    float yaw=age<tell ? 18+(-25-18)*t : -25+(-3-(-25))*t;
                    float roll=age<tell ? -10+(14-(-10))*t : 14+(8-14)*t;
                    var h=classic.hand();
                    classic=new MastersArtAnimation.Pose(classic.weight(),classic.body(),classic.head(),classic.sword(),
                        classic.guard(),classic.frontLeg(),classic.rearLeg(),classic.lower(),classic.forward(),
                        new MastersArtAnimation.Hand(h.x(),h.y(),h.z(),h.pitch(),yaw,roll));
                }
                digest.update(classic.toString().getBytes(StandardCharsets.UTF_8));
                for(boolean left:new boolean[]{false,true}) {
                    // Styles 5-14 were frozen on the fallback; their authored forms have dedicated tests.
                    Pose pose=normalizeReviewedHands && move>=CRACKLE && move<=BLOSSOM_FALL ? NONE : samplePlayer(move,age,tell,recovery,left);ViewPose v=view(pose,left);
                    digest.update((pose.phase()+":"+Float.toHexString(pose.weight())).getBytes(StandardCharsets.UTF_8));
                    for(Joint joint:Joint.values()) {
                        digest.update(pose.local(joint).toString().getBytes(StandardCharsets.UTF_8));
                        digest.update(v.local(joint).toString().getBytes(StandardCharsets.UTF_8));
                        for(float n:pose.world(joint).values())digest.update(Float.toHexString(n).getBytes(StandardCharsets.UTF_8));
                        for(float n:v.world(joint).values())digest.update(Float.toHexString(n).getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
