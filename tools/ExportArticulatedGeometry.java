import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.client.combat.ArticulatedRig;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Offline evidence exporter. It instantiates the ACTUAL production rig against pristine Minecraft
 * classes and exports its actual cube polygons, including sliced UVs and skin-layer cubes.
 * No Minecraft client, renderer, GPU, native fixture or Mixin attachment is run by this program.
 * Paired with preview_articulated_combat.py. All geometry is in model pixels; matrices are column-major.
 */
public final class ExportArticulatedGeometry {
    private static double maxMatrixError, maxSocketError;
    private static int matricesChecked, socketsChecked;
    public static void main(String[] args) {
        StringBuilder out = new StringBuilder(400000);
        out.append("{\"schema\":1,\"provenance\":\"actual ArticulatedRig ModelPart.Cube polygons\",\"variants\":[");
        boolean comma = false;
        for (boolean master : new boolean[] {false, true}) for (boolean slim : new boolean[] {false, true}) {
            if (master && slim) continue;
            if (comma) out.append(','); comma = true;
            ArticulatedRig rig = new ArticulatedRig(slim, master);
            rig.apply(ArticulatedCombatPose.NONE::local);
            out.append("{\"id\":\"").append(master ? "master" : slim ? "slim" : "wide").append("\",\"cubes\":[");
            boolean[] first = {true};
            rig.root.visit(new PoseStack(), (pose, path, index, cube) -> {
                if (!first[0]) out.append(','); first[0] = false;
                out.append("{\"path\":\"").append(path).append("\",\"cubeIndex\":").append(index).append(",\"bindWorld\":");
                matrix(out, pose.pose());
                out.append(",\"faces\":[");
                for (int f = 0; f < cube.polygons.length; f++) {
                    if (f > 0) out.append(',');
                    var face = cube.polygons[f];
                    out.append("{\"normal\":[").append(face.normal().x()).append(',').append(face.normal().y()).append(',').append(face.normal().z()).append("],\"vertices\":[");
                    for (int v = 0; v < face.vertices().length; v++) {
                        if (v > 0) out.append(','); var p = face.vertices()[v];
                        out.append('[').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(',').append(p.u()).append(',').append(p.v()).append(']');
                    }
                    out.append("]}");
                }
                out.append("]}");
            });
            out.append("]}");
            // Compare the pure FK matrices with the exact official ModelPart Euler implementation.
            for (boolean left : new boolean[] {false, true}) {
                int end = master ? 38 : 16;
                for (int tick = 0; tick <= end * 8; tick++) {
                    float age = tick / 8F;
                    var pose = master ? ArticulatedCombatPose.sampleMaster(1, age, 18, 1, 19, left)
                        : ArticulatedCombatPose.sampleSpellcut(0, age, 4, 12, left);
                    rig.apply(pose::local);
                    check(rig, pose::world, slim);
                    var view = ArticulatedCombatPose.view(pose, left);
                    rig.apply(view::local);
                    check(rig, view::world, slim);
                }
            }
        }
        out.append("],\"itemDisplay\":[");
        // Official vanilla handheld.json third-person displays. apply(left) is the actual engine
        // transform, including its mirroring and final (-.5,-.5,-.5) translation.
        for (boolean left : new boolean[] {false, true}) {
            if (left) out.append(',');
            PoseStack stack = new PoseStack();
            new ItemTransform(new Vector3f(0, left ? 90 : -90, left ? -55 : 55),
                new Vector3f(0, 4F / 16, .5F / 16), new Vector3f(.85F)).apply(left, stack.last());
            matrix(out, stack.last().pose());
        }
        out.append("],\"clearanceGrid\":[");
        boolean firstAngle = true;
        for (int yi = -4; yi <= 4; yi++) for (int pi = -4; pi <= 4; pi++) {
            if (!firstAngle) out.append(','); firstAngle = false;
            float yaw = yi * 6.25F, pitch = pi * 5F;
            out.append('[').append(yaw).append(',').append(pitch).append(',')
                .append(ArticulatedCombatPose.viewClearance(yaw, pitch)).append(']');
        }
        out.append("],\"verification\":{\"matricesChecked\":").append(matricesChecked)
            .append(",\"maxModelPartMatrixError\":").append(maxMatrixError)
            .append(",\"socketsChecked\":").append(socketsChecked)
            .append(",\"maxSocketMatrixError\":").append(maxSocketError).append("}}");
        System.out.println(out);
        if (maxMatrixError > .00001 || maxSocketError > .00001) throw new AssertionError("ModelPart / palette mismatch");
    }
    private static void check(ArticulatedRig rig, java.util.function.Function<Joint, ArticulatedCombatPose.Matrix> world, boolean slim) {
        for (Joint joint : Joint.values()) {
            PoseStack stack = new PoseStack(); rig.transformTo(joint, stack);
            maxMatrixError = Math.max(maxMatrixError, difference(stack.last().pose(), world.apply(joint), 0, world.apply(joint))); matricesChecked++;
        }
        for (boolean left : new boolean[] {false, true}) {
            PoseStack stack = new PoseStack(); rig.socket(left ? HumanoidArm.LEFT : HumanoidArm.RIGHT, stack);
            maxSocketError = Math.max(maxSocketError, difference(stack.last().pose(), world.apply(left ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET), (left ? 1 : -1) * (slim ? .5F : 1F), world.apply(left ? Joint.LEFT_HAND : Joint.RIGHT_HAND))); socketsChecked++;
        }
    }
    private static double difference(Matrix4f actual, ArticulatedCombatPose.Matrix expected, float offset, ArticulatedCombatPose.Matrix lateralBasis) {
        double max = 0;
        for (int c = 0; c < 4; c++) for (int r = 0; r < 4; r++) {
            float v = expected.get(r, c);
            if (c == 3 && r < 3) v = (v + lateralBasis.get(r, 0) * offset) / 16;
            max = Math.max(max, Math.abs(actual.get(c, r) - v));
        }
        return max;
    }
    private static void matrix(StringBuilder out, Matrix4f matrix) {
        out.append('['); float[] values = matrix.get(new float[16]);
        for (int i = 0; i < values.length; i++) { if (i > 0) out.append(','); out.append(values[i]); }
        out.append(']');
    }
}
