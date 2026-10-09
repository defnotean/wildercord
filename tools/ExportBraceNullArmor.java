import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.client.combat.ArticulatedArmorGeometry;
import dev.wildercord.client.combat.ArticulatedAuraShellGeometry;
import dev.wildercord.client.combat.ArticulatedRig;
import dev.wildercord.client.render.AuraShellLayer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/** Bounded source-driven OFFLINE armor/shell geometry, never game footage or native acceptance. */
public final class ExportBraceNullArmor {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] PLACEMENTS = {"canonical", "free_left", "free_right", "vertical_up", "vertical_down"};
    private static final float[] YAWS = {0, -25, 25, 0, 0};
    private static final float[] PITCHES = {0, -20, -20, -90, 90};
    private static float maxPaletteError;
    private static int matricesChecked;
    private ExportBraceNullArmor() {}

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("No arguments accepted");
        var roots = EntityModelSet.vanilla();
        StringBuilder json = new StringBuilder(8_000_000);
        json.append("{\"schema\":1,\"label\":\"OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE\",\"units\":\"model_pixels\",\"frames\":[");
        boolean comma = false;
        for (boolean slim : new boolean[] {false, true}) {
            var layers = slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR;
            Map<EquipmentSlot, ArticulatedArmorGeometry> models = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : SLOTS) models.put(slot, new ArticulatedArmorGeometry(roots.bakeLayer(layers.get(slot)), slot));
            var viewModel = new ArticulatedArmorGeometry(roots.bakeLayer(layers.chest()), EquipmentSlot.CHEST, true);
            var shellSource = (slim ? AuraShellLayer.createSlimShell() : AuraShellLayer.createShell()).bakeRoot();
            var shellWorld = new ArticulatedAuraShellGeometry(shellSource, slim, false);
            var shellView = new ArticulatedAuraShellGeometry(shellSource, slim, true);
            var rig = new ArticulatedRig(slim, false);
            if (viewModel.mesh().controlPoints().stream().anyMatch(point -> !point.region().arm())
                    || shellView.mesh().controlPoints().stream().anyMatch(point -> !point.region().arm()))
                throw new AssertionError("First-person armor/shell contains a non-arm region");
            for (boolean left : new boolean[] {false, true}) for (int move : new int[] {24, 25}) {
                var style = MastersStyleRules.animation(move);
                if (style == null || style.windup() != (move == 24 ? 6 : 4) || style.recovery() != (move == 24 ? 16 : 14))
                    throw new AssertionError("Brace/Null preview requires accepted 6/16 and 4/14 windows");
                float[] ages = move == 24 ? new float[] {3.875F, 6, 10, 20} : new float[] {2.625F, 4, 7.5F, 16};
                String[] phaseLabels = {"chamber", "active", "follow", "recovery"};
                for (int sample = 0; sample < ages.length; sample++) {
                    float age = ages[sample];
                    var pose = ArticulatedCombatPose.samplePlayer(move, age, style.windup(), style.recovery(), left);
                    var view = ArticulatedCombatPose.view(pose, left);
                    var palette = ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(pose.world(joint).values()));
                    var viewPalette = ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(view.world(joint).values()));
                    rig.apply(pose::local); comparePalette(palette, ArticulatedArmorGeometry.Palette.capture(rig));
                    rig.apply(view::local); comparePalette(viewPalette, ArticulatedArmorGeometry.Palette.capture(rig));
                    if (comma) json.append(','); comma = true;
                    json.append("{\"variant\":\"").append(slim ? "slim" : "wide").append("\",\"left\":").append(left)
                        .append(",\"move\":").append(move).append(",\"clip\":\"").append(style.art()).append("\",\"age\":").append(age)
                        .append(",\"weight\":").append(pose.weight()).append(",\"phase\":\"").append(pose.phase())
                        .append("\",\"sampleLabel\":\"").append(phaseLabels[sample]).append("\",\"bodyWorld\":[");
                    for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); matrix(json, palette.matrix(joint)); }
                    json.append("],\"viewWorld\":[");
                    for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); matrix(json, viewPalette.matrix(joint)); }
                    json.append("],\"third\":[");
                    for (int i = 0; i < SLOTS.length; i++) {
                        EquipmentSlot slot = SLOTS[i]; var model = models.get(slot); model.setupAnim(palette);
                        if (i > 0) json.append(',');
                        json.append("{\"slot\":\"").append(slot.name()).append("\",\"layer\":\"").append(layers.get(slot)).append("\",\"faces\":");
                        faces(json, model.root(), model.mesh().faces().size()); json.append('}');
                    }
                    json.append("],\"first\":{\"slot\":\"CHEST\",\"armsOnly\":true,\"layer\":\"").append(layers.chest()).append("\",\"faces\":");
                    viewModel.setupAnim(viewPalette); faces(json, viewModel.root(), viewModel.mesh().faces().size());
                    json.append("},\"shell\":{\"label\":\"GEOMETRIC WIREFRAME ONLY; NO EMISSIVE MATERIAL\",\"third\":");
                    shellWorld.setupAnim(palette); faces(json, shellWorld.root(), shellWorld.mesh().faces().size());
                    json.append(",\"first\":"); shellView.setupAnim(viewPalette); faces(json, shellView.root(), shellView.mesh().faces().size());
                    json.append("},\"viewPlacements\":[");
                    // Exact ArticulatedViewModel.submit order and clamps, including weighted clearance.
                    // +/-90 are supplied pitch deltas, not a rotation of the game camera or player body.
                    for (int i = 0; i < PLACEMENTS.length; i++) {
                        if (i > 0) json.append(',');
                        float yaw = Math.max(-25, Math.min(25, YAWS[i])) * pose.weight();
                        float pitch = Math.max(-20, Math.min(20, PITCHES[i])) * pose.weight();
                        float clearance = ArticulatedCombatPose.viewClearance(yaw, pitch);
                        Matrix4f camera = new Matrix4f().translation(view.origin().x(), view.origin().y() - .8F * clearance, view.origin().z() - clearance)
                            .rotateY((float) Math.toRadians(-yaw)).rotateX((float) Math.toRadians(-pitch)).scale(-1F / 16, -1F / 16, 1F / 16);
                        json.append("{\"name\":\"").append(PLACEMENTS[i]).append("\",\"inputYawDelta\":").append(YAWS[i])
                            .append(",\"inputPitchDelta\":").append(PITCHES[i]).append(",\"yaw\":").append(yaw).append(",\"pitch\":").append(pitch)
                            .append(",\"clearance\":").append(clearance).append(",\"camera\":");
                        matrix(json, camera); json.append('}');
                    }
                    json.append("]}");
                }
            }
        }
        json.append("],\"verification\":{\"matricesChecked\":").append(matricesChecked)
            .append(",\"maxRigVsPurePaletteError\":").append(maxPaletteError).append("}}");
        if (maxPaletteError > .00001F) throw new AssertionError("Rig / armor / shell palette mismatch");
        System.out.println(json);
    }

    private static void comparePalette(ArticulatedArmorGeometry.Palette pure, ArticulatedArmorGeometry.Palette rig) {
        for (Joint joint : Joint.values()) {
            float[] a = pure.matrix(joint).get(new float[16]), b = rig.matrix(joint).get(new float[16]);
            for (int i = 0; i < 16; i++) maxPaletteError = Math.max(maxPaletteError, Math.abs(a[i] - b[i]));
            matricesChecked++;
        }
    }
    private static void matrix(StringBuilder json, Matrix4f matrix) {
        float[] values = matrix.get(new float[16]); json.append('[');
        for (int i = 0; i < values.length; i++) { if (i > 0) json.append(','); json.append(values[i]); }
        json.append(']');
    }
    private static void faces(StringBuilder json, ModelPart root, int expected) {
        json.append('['); int[] count = {0};
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            for (ModelPart.Polygon polygon : cube.polygons) {
                if (count[0]++ > 0) json.append(','); json.append('[');
                for (int i = 0; i < polygon.vertices().length; i++) {
                    if (i > 0) json.append(','); var vertex = polygon.vertices()[i];
                    Vector3f p = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f()).mul(16);
                    if (!p.isFinite() || !Float.isFinite(vertex.u()) || !Float.isFinite(vertex.v())) throw new AssertionError("Non-finite polygon");
                    json.append('[').append(p.x).append(',').append(p.y).append(',').append(p.z).append(',').append(vertex.u()).append(',').append(vertex.v()).append(']');
                }
                json.append(']');
            }
        });
        if (count[0] != expected) throw new AssertionError("ModelPart polygon count changed");
        json.append(']');
    }
}
