package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.spell.ClimateRules;
import dev.wildercord.wildlife.Cinderfox;
import dev.wildercord.wildlife.Glimmerwing;
import dev.wildercord.wildlife.LumenStag;
import dev.wildercord.wildlife.Rimehare;
import dev.wildercord.wildlife.Skyray;
import dev.wildercord.wildlife.WildlifeRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * The wildlife renderers: each fills the shared render state from its creature (eased poses blended between ticks,
 * how strongly it glows by night and moon) and draws its own skin and glow. The tortoise, with its garden, has a class
 * of its own ({@link MossbackTortoiseRenderer}).
 */
public final class WildlifeRenderers {
	private WildlifeRenderers() {}

	public static final ModelLayerLocation GLIMMERWING = layer("glimmerwing");
	public static final ModelLayerLocation LUMEN_STAG = layer("lumen_stag");
	public static final ModelLayerLocation MOSSBACK_TORTOISE = layer("mossback_tortoise");
	public static final ModelLayerLocation CINDERFOX = layer("cinderfox");
	public static final ModelLayerLocation SKYRAY = layer("skyray");
	public static final ModelLayerLocation RIMEHARE = layer("rimehare");
	public static final ModelLayerLocation LANTERN_NEWT = layer("lantern_newt");
	public static final class LanternNewtRenderer extends WildlifeRenderer<dev.wildercord.wildlife.LanternNewt,LanternNewtModel> {
		public LanternNewtRenderer(EntityRendererProvider.Context c) {super(c,new LanternNewtModel(c.bakeLayer(LANTERN_NEWT)),.25F,texture("lantern_newt"));var light=texture("lantern_newt_glow");glow(s -> light);}
		@Override public void extractRenderState(dev.wildercord.wildlife.LanternNewt e,WildlifeRenderState s,float partial) {
			super.extractRenderState(e,s,partial);s.air=e.isInWater()?1:0;s.graze=e.browsing()?1:0;s.bow=e.response()>0?1:0;
			s.glow=dev.wildercord.wildlife.WetlandRules.glow(dev.wildercord.wildlife.WetlandRules.night(e.level().getOverworldClockTime()),e.isInWaterOrRain(),e.response()>0);
			if(e.response()>0)s.glow*=.8F+.2F*Mth.sin(s.ageInTicks*.25F);
		}
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(Wildercord.id(name), "main");
	}

	static Identifier texture(String name) {
		return Wildercord.id("textures/entity/wildlife/" + name + ".png");
	}

	/** How dark it is where the creature is (0 noon to 1 midnight). */
	static float night(Level level) {
		return WildlifeRules.night(level.getSkyDarken());
	}

	// ------------------------------------------------------------------ the glimmerwing

	public static final class GlimmerwingRenderer extends WildlifeRenderer<Glimmerwing, GlimmerwingModel> {
		private static final Identifier[] SKINS = {texture("glimmerwing_moonlit"), texture("glimmerwing_rose"), texture("glimmerwing_amber")};
		private static final Identifier[] GLOWS = {texture("glimmerwing_moonlit_glow"), texture("glimmerwing_rose_glow"), texture("glimmerwing_amber_glow")};

		public GlimmerwingRenderer(EntityRendererProvider.Context context) {
			super(context, new GlimmerwingModel(context.bakeLayer(GLIMMERWING)), 0.12F, SKINS[0]);
			glow(state -> GLOWS[state.variant]);
		}

		@Override
		public Identifier getTextureLocation(WildlifeRenderState state) {
			return SKINS[state.variant];
		}

		@Override
		public void extractRenderState(Glimmerwing moth, WildlifeRenderState state, float partial) {
			super.extractRenderState(moth, state, partial);
			state.variant = moth.variant();
			state.flapPhase = Mth.lerp(partial, moth.flapPhaseO, moth.flapPhase);
			state.flapStrength = moth.flapStrength;
			state.glow = WildlifeRules.glimmerGlow(night(moth.level()));
		}

		/** Hand-sized: the model's metre of wing drawn at six tenths. */
		@Override
		protected void scale(WildlifeRenderState state, PoseStack poseStack) {
			poseStack.scale(0.6F, 0.6F, 0.6F);
		}

		/** Its own light keeps it from ever going quite dark. */
		@Override
		protected int getBlockLightLevel(Glimmerwing moth, BlockPos pos) {
			return Math.max(super.getBlockLightLevel(moth, pos), 9);
		}
	}

	// ------------------------------------------------------------------ the lumen stag

	public static final class LumenStagRenderer extends WildlifeRenderer<LumenStag, LumenStagModel> {
		public LumenStagRenderer(EntityRendererProvider.Context context) {
			super(context, new LumenStagModel(context.bakeLayer(LUMEN_STAG)), 0.6F, texture("lumen_stag"));
			Identifier glow = texture("lumen_stag_glow");
			glow(state -> glow);
		}

		@Override
		public void extractRenderState(LumenStag stag, WildlifeRenderState state, float partial) {
			super.extractRenderState(stag, state, partial);
			state.graze = Mth.lerp(partial, stag.grazeO, stag.graze);
			state.watch = Mth.lerp(partial, stag.watchO, stag.watch);
			state.bow = Mth.lerp(partial, stag.bowO, stag.bow);
			state.shed = stag.shedToday();
			state.glow = WildlifeRules.antlerGlow(ClimateRules.moonPhase(stag.level().getOverworldClockTime()), night(stag.level()));
		}
	}

	// ------------------------------------------------------------------ the cinderfox

	public static final class CinderfoxRenderer extends WildlifeRenderer<Cinderfox, CinderfoxModel> {
		public CinderfoxRenderer(EntityRendererProvider.Context context) {
			super(context, new CinderfoxModel(context.bakeLayer(CINDERFOX)), 0.32F, texture("cinderfox"));
			Identifier glow = texture("cinderfox_glow");
			glow(state -> glow);
		}

		@Override
		public void extractRenderState(Cinderfox fox, WildlifeRenderState state, float partial) {
			super.extractRenderState(fox, state, partial);
			state.sit = Mth.lerp(partial, fox.sitO, fox.sit);
			// Its ember never goes out; at night it burns bright.
			state.glow = 0.5F + 0.5F * night(fox.level());
		}
	}

	// ------------------------------------------------------------------ the skyray

	public static final class SkyrayRenderer extends WildlifeRenderer<Skyray, SkyrayModel> {
		public SkyrayRenderer(EntityRendererProvider.Context context) {
			super(context, new SkyrayModel(context.bakeLayer(SKYRAY)), 0.0F, texture("skyray"));
			Identifier glow = texture("skyray_glow");
			glow(state -> glow);
		}

		@Override
		public void extractRenderState(Skyray ray, WildlifeRenderState state, float partial) {
			super.extractRenderState(ray, state, partial);
			state.flapPhase = Mth.lerp(partial, ray.flapPhaseO, ray.flapPhase);
			state.flapStrength = ray.flapStrength;
			state.bank = Mth.lerp(partial, ray.bankO, ray.bank);
			state.pitch = -ray.getXRot(partial) * Mth.DEG_TO_RAD;
			// The stars on its back come out with the night.
			state.glow = 0.12F + 0.88F * night(ray.level());
		}
	}

	// ------------------------------------------------------------------ the rimehare

	public static final class RimehareRenderer extends WildlifeRenderer<Rimehare, RimehareModel> {
		public RimehareRenderer(EntityRendererProvider.Context context) {
			super(context, new RimehareModel(context.bakeLayer(RIMEHARE)), 0.22F, texture("rimehare"));
			Identifier glow = texture("rimehare_glow");
			glow(state -> glow);
		}

		@Override
		public void extractRenderState(Rimehare hare, WildlifeRenderState state, float partial) {
			super.extractRenderState(hare, state, partial);
			state.air = Mth.lerp(partial, hare.airO, hare.air);
			state.alert = Mth.lerp(partial, hare.alertO, hare.alert);
			state.rise = Mth.clamp((float) hare.getDeltaMovement().y * 6, -1, 1);
			state.glow = 0.2F + 0.55F * night(hare.level());
			prints(hare);
		}

		/** The frost prints of a landing: a pair, side by side, pointing the way it bounds. */
		private static void prints(Rimehare hare) {
			if (hare.landedTick != hare.tickCount || hare.printedTick == hare.tickCount || !(hare.level() instanceof ClientLevel level)) {
				return;
			}
			hare.printedTick = hare.tickCount;
			float yaw = hare.getYRot() * Mth.DEG_TO_RAD;
			double side = hare.isBaby() ? 0.07 : 0.11;
			float size = hare.isBaby() ? 0.07F : 0.11F;
			double y = Math.floor(hare.getY() * 16 + 0.5) / 16 + 0.012;
			for (int s = -1; s <= 1; s += 2) {
				double x = hare.getX() + Math.cos(yaw) * side * s;
				double z = hare.getZ() + Math.sin(yaw) * side * s;
				Minecraft.getInstance().particleEngine.add(new FrostPrint(level, x, y, z, yaw, size));
			}
		}
	}
}
