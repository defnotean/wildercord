package dev.wildercord.client.fx;

import dev.wildercord.aura.AuraFx;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Non-emissive impact scars projected onto nearby solid ground, with fragments of the actual floor. */
public final class AuraGroundScar extends SingleQuadParticle implements SigilGroup.Extent {
	private record Key(Vec3 at, int kind, int radius) {}
	private static final class Tile {
		private final BlockPos floor;
		private final double height;
		private final BlockState block;
		private int lighting;
		private boolean visible = true;
		Tile(BlockPos floor, double height, BlockState block, ClientLevel level) {
			this.floor = floor; this.height = height; this.block = block;
			refresh(level);
		}
		BlockPos floor() { return floor; }
		double height() { return height; }
		BlockState block() { return block; }
		void refresh(ClientLevel level) {
			visible = level.getBlockState(floor).equals(block);
			BlockPos above = floor.above();
			lighting = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, above), level.getBrightness(LightLayer.SKY, above));
		}
	}
	private static final Map<Key, AuraGroundScar> LIVE = new HashMap<>();
	private final Key key;
	private final float radius;
	private final List<Tile> tiles = new ArrayList<>();

	public static int showing() { return LIVE.size(); }
	public static void clear() { LIVE.values().forEach(AuraGroundScar::remove); LIVE.clear(); }
	public static void receive(AuraFx.GroundScar cue) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !Float.isFinite(cue.radius())) return;
		LIVE.values().removeIf(p -> !p.isAlive() || p.level != mc.level);
		Key key = new Key(cue.at(), Mth.clamp(cue.kind(), 0, 2), Math.round(cue.radius() * 10));
		AuraGroundScar old = LIVE.get(key);
		if (old != null) { old.age = 0; old.lifetime = Mth.clamp(cue.ticks(), 20, 600); return; }
		if (LIVE.size() >= 24) return;
		AuraGroundScar scar = new AuraGroundScar(mc.level, cue, key);
		if (scar.tiles.isEmpty()) return;
		LIVE.put(key, scar);
		mc.particleEngine.add(scar);
		scar.debris();
	}

	private AuraGroundScar(ClientLevel level, AuraFx.GroundScar cue, Key key) {
		super(level, cue.at().x, cue.at().y, cue.at().z, SpellCircleParticle.particleSprite("aura_scar_" + key.kind()));
		this.key = key;
		this.radius = Mth.clamp(cue.radius(), 0.3F, 8);
		this.lifetime = Mth.clamp(cue.ticks(), 20, 600);
		this.hasPhysics = false;
		int extent = Mth.ceil(radius);
		BlockPos centre = BlockPos.containing(cue.at());
		for (int dx = -extent; dx <= extent; dx++) for (int dz = -extent; dz <= extent; dz++) {
			BlockPos probe = centre.offset(dx, 0, dz);
			double px = probe.getX() + 0.5 - x, pz = probe.getZ() + 0.5 - z;
			if (Math.abs(px) > radius + 0.5 || Math.abs(pz) > radius + 0.5) continue;
			for (int down = 0; down < 4; down++) {
				BlockPos floor = probe.below(down);
				BlockState block = level.getBlockState(floor);
				var shape = block.getCollisionShape(level, floor);
				if (shape.isEmpty()) continue;
				double height = floor.getY() + shape.max(Direction.Axis.Y);
				if (height > y + 0.3 || height < y - 2.5) continue;
				// Thin poles and fences are not surfaces for a floor scar.
				if (shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X) < 0.9
					|| shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z) < 0.9) break;
				tiles.add(new Tile(floor, height + 0.012, block, level));
				break;
			}
		}
	}

	private void debris() {
		int count = MagicQuality.bodyAura == MagicQuality.BodyAura.OFF ? 0 : MagicQuality.bodyAura == MagicQuality.BodyAura.CALM ? 6 : 14;
		for (int i = 0; i < count; i++) {
			Tile tile = tiles.get(random.nextInt(tiles.size()));
			double px = tile.floor().getX() + random.nextDouble(), pz = tile.floor().getZ() + random.nextDouble();
			level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, tile.block()), px, tile.height() + 0.05, pz,
				(px - x) * 0.03, 0.06 + random.nextDouble() * 0.08, (pz - z) * 0.03);
		}
	}

	@Override public void tick() {
		if (++age >= lifetime) { remove(); LIVE.remove(key, this); return; }
		// Sample world changes on ticks, never once per floor tile on every rendered frame.
		if (age % 5 == 0) tiles.forEach(tile -> tile.refresh(level));
	}
	@Override public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		float fade = Mth.clamp((lifetime - age - partial) / 20F, 0, 1);
		int ink = (Math.round(fade * 235) << 24) | 0xFFFFFF;
		Quaternionf q = new Quaternionf().rotationX(-Mth.HALF_PI);
		for (Tile tile : tiles) {
			// If a player changes the floor, the old mark must not float above the replacement or open air.
			if (!tile.visible) continue;
			double x0 = Math.max(tile.floor().getX(), x - radius), x1 = Math.min(tile.floor().getX() + 1, x + radius);
			double z0 = Math.max(tile.floor().getZ(), z - radius), z1 = Math.min(tile.floor().getZ() + 1, z + radius);
			double width = Math.min(x1 - x0, z1 - z0);
			if (width <= 0.001) continue;
			double px = (x0 + x1) / 2, pz = (z0 + z1) / 2;
			float u0 = (float) ((px - width / 2 - x + radius) / (2 * radius));
			float v0 = (float) ((pz - width / 2 - z + radius) / (2 * radius));
			float span = (float) (width / (2 * radius));
			state.add(Layer.TRANSLUCENT, (float)(px - camera.position().x), (float)(tile.height() - camera.position().y),
				(float)(pz - camera.position().z), q.x, q.y, q.z, q.w, (float)(width * 0.501),
				Mth.lerp(u0, sprite.getU0(), sprite.getU1()), Mth.lerp(u0 + span, sprite.getU0(), sprite.getU1()),
				Mth.lerp(v0, sprite.getV0(), sprite.getV1()), Mth.lerp(v0 + span, sprite.getV0(), sprite.getV1()), ink, tile.lighting);
		}
	}
	@Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
	@Override public ParticleRenderType getGroup() { return SigilGroup.TYPE; }
	@Override public double centreX() { return x; }
	@Override public double centreY() { return y - 1; }
	@Override public double centreZ() { return z; }
	@Override public double reach() { return radius + 2; }
}
