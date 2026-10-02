package dev.wildercord.client.fx;

import dev.wildercord.cast.SoarRules;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Articulated, double-sided flight feathers rooted to the interpolated shoulder blades. */
public final class SoarWingModel extends SingleQuadParticle implements SigilGroup.Extent {

    private final AbstractClientPlayer flier;
    private float spread, oldSpread;
    private float strength = 0, oldStrength;
    private float phase, oldPhase;
    private int closing;

    SoarWingModel(ClientLevel level, AbstractClientPlayer flier) {
        super(level, flier.getX(), flier.getY(), flier.getZ(), SpellCircleParticle.particleSprite("soar_feather"));
        this.flier = flier;
        hasPhysics = false;
    }

    boolean belongsTo(ClientLevel current) { return level == current; }

    @Override public void tick() {
        oldSpread = spread;
        oldStrength = strength;
        oldPhase = phase;
        float speed = (float)flier.position().distanceTo(new Vec3(flier.xo, flier.yo, flier.zo));
        phase += 0.19F + Math.min(0.13F, speed * 0.4F);
        var note = flier.getAttached(WildercordAttachments.SOARING);
        boolean active = note != null && !note.falling() && note.until() > level.getGameTime();
        if (flier.isRemoved() || flier.level() != level) { remove(); return; }
        closing = active ? 0 : closing + 1;
        if (closing > 18) { remove(); return; }
        float target = active && !flier.onGround() ? 1 : 0;
        spread += (target - spread) * 0.18F;
        float fade = active ? Mth.clamp((note.until() - level.getGameTime()) / (float)SoarRules.WARNING_TICKS, 0.25F, 1) : 0;
        strength += (fade - strength) * 0.18F;
        x = flier.getX(); y = flier.getY(); z = flier.getZ();
        age++;
    }

    @Override public void extract(QuadParticleRenderState state, Camera camera, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (flier.isInvisible() || flier.isSpectator() || (mc.getCameraEntity() == flier && mc.options.getCameraType().isFirstPerson())) return;
        float alpha = Mth.lerp(partial, oldStrength, strength);
        if (alpha < 0.02F) return;
        float open = Mth.lerp(partial, oldSpread, spread);
        float yaw = (flier.yBodyRotO + Mth.wrapDegrees(flier.yBodyRot - flier.yBodyRotO) * partial) * Mth.DEG_TO_RAD;
        Vec3 position = new Vec3(Mth.lerp(partial, flier.xo, flier.getX()), Mth.lerp(partial, flier.yo, flier.getY()), Mth.lerp(partial, flier.zo, flier.getZ()));
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
        Vec3 root = position.add(0, flier.getBbHeight() * 0.72, 0).add(back.scale(0.31)).subtract(camera.position());
        float time = age + partial;
        float beat = (float)Math.sin(Mth.lerp(partial, oldPhase, phase)) * 0.24F * open;
        int light = LightCoordsUtil.pack(Math.max(8, level.getBrightness(LightLayer.BLOCK, flier.blockPosition())), level.getBrightness(LightLayer.SKY, flier.blockPosition()));
        int ink = (Math.round(alpha * 255) << 24) | 0xFFFFFF;
        boolean distant = flier.position().distanceToSqr(camera.position()) > 24 * 24;
        boolean calm = MagicQuality.bodyAura != MagicQuality.BodyAura.FULL;
        int flights = distant ? 6 : calm ? 9 : 12;
        int coverts = distant ? 4 : calm ? 6 : 8;
        for (int side = -1; side <= 1; side += 2) {
            Quaternionf wing = new Quaternionf().rotationY(-yaw).rotateZ(side * beat).rotateY(side * (0.18F + (1 - open) * 0.45F));
            // Overlapping long primaries form a broad fan, with shorter feathers filling the inner wing.
            for (int i = 0; i < flights; i++) {
                float t = i / (float)(flights - 1);
                Vector3f socket = new Vector3f(side * (0.14F + t * 0.95F) * (0.30F + 0.70F * open), 0.10F + 0.32F * (float)Math.sin(t * 2.2), -0.015F * i);
                float angle = side * Mth.lerp(open, 0.08F + t * 0.14F, 0.40F + t * 1.17F + 0.04F * (float)Math.sin(time * 0.19 - t * 2));
                feather(state, root, wing, socket, angle, 0.73F + 0.40F * t, ink, light);
            }
            for (int i = 0; i < coverts; i++) {
                float t = i / (float)(coverts - 1);
                Vector3f socket = new Vector3f(side * (0.10F + t * 0.73F) * (0.30F + 0.70F * open), 0.18F + 0.33F * t, -0.20F);
                feather(state, root, wing, socket, side * Mth.lerp(open, 0.12F, 0.55F + t * 0.90F), 0.44F + 0.16F * t, ink, light);
            }
        }
    }

    private void feather(QuadParticleRenderState state, Vec3 root, Quaternionf wing, Vector3f socket, float angle, float length, int ink, int light) {
        Quaternionf q = new Quaternionf(wing).rotateZ(angle);
        Vector3f anchor = wing.transform(new Vector3f(socket));
        float tile = length / 4;
        for (int i = 0; i < 4; i++) {
            Vector3f offset = q.transform(new Vector3f(0, -(i + 0.5F) * tile, 0)).add(anchor);
            Vec3 at = root.add(offset.x, offset.y, offset.z);
            float v0 = Mth.lerp(i / 4F, sprite.getV0(), sprite.getV1()), v1 = Mth.lerp((i + 1) / 4F, sprite.getV0(), sprite.getV1());
            for (int face = 0; face < 2; face++) {
                Quaternionf faceQ = face == 0 ? q : new Quaternionf(q).rotateY(Mth.PI);
                state.add(Layer.TRANSLUCENT, (float)at.x, (float)at.y, (float)at.z, faceQ.x, faceQ.y, faceQ.z, faceQ.w, tile * 0.501F,
                    face == 0 ? sprite.getU0() : sprite.getU1(), face == 0 ? sprite.getU1() : sprite.getU0(), v0, v1, ink, light);
            }
        }
    }

    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
    @Override public ParticleRenderType getGroup() { return SigilGroup.TYPE; }
    @Override public double centreX() { return x; }
    @Override public double centreY() { return y + 1; }
    @Override public double centreZ() { return z; }
    @Override public double reach() { return 2.8; }
}
