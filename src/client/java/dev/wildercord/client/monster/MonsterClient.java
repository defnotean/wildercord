package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.Bramblewalker;
import dev.wildercord.monster.BogWitchFrog;
import dev.wildercord.monster.GeodeCrawler;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.monster.ThunderwingHarpy;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.util.Mth;

/**
 * The monsters of the wilds on the client: their models and renderers. Each glow is its own layer (see
 * {@link WildMonsterRenderer#glow}) whose strength follows what the creature is doing, so its tells read in any light.
 */
public final class MonsterClient {
	private MonsterClient() {}

	public static final ModelLayerLocation BRAMBLEWALKER = new ModelLayerLocation(Wildercord.id("bramblewalker"), "main");
	public static final ModelLayerLocation THUNDERWING_HARPY = new ModelLayerLocation(Wildercord.id("thunderwing_harpy"), "main");
	public static final ModelLayerLocation GEODE_CRAWLER = new ModelLayerLocation(Wildercord.id("geode_crawler"), "main");
	public static final ModelLayerLocation BOG_WITCH_FROG = new ModelLayerLocation(Wildercord.id("bog_witch_frog"), "main");

	public static void init() {
		ModelLayerRegistry.registerModelLayer(BRAMBLEWALKER, BramblewalkerModel::createLayer);
		ModelLayerRegistry.registerModelLayer(GloomstalkerRenderer.LAYER, GloomstalkerModel::createLayer);
		ModelLayerRegistry.registerModelLayer(THUNDERWING_HARPY, ThunderwingHarpyModel::createLayer);
		ModelLayerRegistry.registerModelLayer(GEODE_CRAWLER, GeodeCrawlerModel::createLayer);
		ModelLayerRegistry.registerModelLayer(BOG_WITCH_FROG, BogWitchFrogModel::createLayer);
		ModelLayerRegistry.registerModelLayer(ManaOozeRenderer.INNER, ManaOozeModel::createInner);
		ModelLayerRegistry.registerModelLayer(ManaOozeRenderer.OUTER, ManaOozeModel::createOuter);
		ModelLayerRegistry.registerModelLayer(BogBubbleRenderer.LAYER, BogBubbleRenderer::createLayer);

		EntityRendererRegistry.register(MonsterContent.BRAMBLEWALKER, MonsterClient::bramblewalker);
		EntityRendererRegistry.register(MonsterContent.GLOOMSTALKER, GloomstalkerRenderer::new);
		EntityRendererRegistry.register(MonsterContent.THUNDERWING_HARPY, MonsterClient::harpy);
		EntityRendererRegistry.register(MonsterContent.GEODE_CRAWLER, MonsterClient::crawler);
		EntityRendererRegistry.register(MonsterContent.BOG_WITCH_FROG, MonsterClient::frog);
		EntityRendererRegistry.register(MonsterContent.MANA_OOZE, ManaOozeRenderer::new);
		EntityRendererRegistry.register(MonsterContent.BOG_BUBBLE, BogBubbleRenderer::new);
		EntityRendererRegistry.register(MonsterContent.THROWN_BRAMBLE, ThrownItemRenderer::new);
	}

	/** Its eyes, and the green veins in its vines, which blaze as it rears for the lash. */
	private static WildMonsterRenderer<Bramblewalker, BramblewalkerModel> bramblewalker(EntityRendererProvider.Context context) {
		WildMonsterRenderer<Bramblewalker, BramblewalkerModel> renderer = new WildMonsterRenderer<>(context,
			new BramblewalkerModel(context.bakeLayer(BRAMBLEWALKER)), 0.7F, Wildercord.id("textures/entity/bramblewalker.png"));
		renderer.glow(Wildercord.id("textures/entity/bramblewalker_glow.png"), state -> WildMonsterRenderer.white(
			(0.35F + 0.65F * Math.max(state.windup, state.acting) + 0.08F * Mth.sin(state.ageInTicks * 0.15F)) * WildMonsterRenderer.alive(state)));
		renderer.glow(Wildercord.id("textures/entity/bramblewalker_eyes.png"), state -> WildMonsterRenderer.white(0.9F * WildMonsterRenderer.alive(state)));
		return renderer;
	}

	/** Its eyes, and the lightning streaks in its feathers: crackling as it shrieks or calls a bolt, brighter in a storm. */
	private static WildMonsterRenderer<ThunderwingHarpy, ThunderwingHarpyModel> harpy(EntityRendererProvider.Context context) {
		WildMonsterRenderer<ThunderwingHarpy, ThunderwingHarpyModel> renderer = new WildMonsterRenderer<>(context,
			new ThunderwingHarpyModel(context.bakeLayer(THUNDERWING_HARPY)), 0.5F, Wildercord.id("textures/entity/thunderwing_harpy.png"));
		renderer.glow(Wildercord.id("textures/entity/thunderwing_harpy_glow.png"), state -> {
			Minecraft mc = Minecraft.getInstance();
			float storm = mc.level != null && mc.level.isThundering() ? 0.25F : 0;
			float charge = Math.max(state.windup, state.alt);
			// A crackle: the streaks flicker hard while it's charged.
			float flicker = charge > 0.05F ? (Mth.sin(state.ageInTicks * 2.7F) > 0.2F ? 0.25F : -0.1F) * charge : 0;
			return WildMonsterRenderer.white((0.3F + storm + 0.6F * charge + flicker) * WildMonsterRenderer.alive(state));
		});
		renderer.glow(Wildercord.id("textures/entity/thunderwing_harpy_eyes.png"), state -> WildMonsterRenderer.white(
			(0.85F - 0.6F * state.stunned) * WildMonsterRenderer.alive(state)));
		return renderer;
	}

	/** Its crystals, shimmering slowly, flaring as it rattles; and its eyes. */
	private static WildMonsterRenderer<GeodeCrawler, GeodeCrawlerModel> crawler(EntityRendererProvider.Context context) {
		WildMonsterRenderer<GeodeCrawler, GeodeCrawlerModel> renderer = new WildMonsterRenderer<>(context,
			new GeodeCrawlerModel(context.bakeLayer(GEODE_CRAWLER)), 0.7F, Wildercord.id("textures/entity/geode_crawler.png"));
		renderer.glow(Wildercord.id("textures/entity/geode_crawler_glow.png"), state -> WildMonsterRenderer.white(
			(0.45F + 0.15F * Mth.sin(state.ageInTicks * 0.09F) + 0.4F * state.windup + 0.25F * state.acting - 0.3F * state.stunned)
				* WildMonsterRenderer.alive(state)));
		return renderer;
	}

	/** Its lamp-eyes, always lit, and its throat sac, glowing a sickly green as it swells (and after it has eaten). */
	private static WildMonsterRenderer<BogWitchFrog, BogWitchFrogModel> frog(EntityRendererProvider.Context context) {
		WildMonsterRenderer<BogWitchFrog, BogWitchFrogModel> renderer = new WildMonsterRenderer<>(context,
			new BogWitchFrogModel(context.bakeLayer(BOG_WITCH_FROG)), 0.8F, Wildercord.id("textures/entity/bog_witch_frog.png"));
		renderer.glow(Wildercord.id("textures/entity/bog_witch_frog_eyes.png"), state -> WildMonsterRenderer.white(0.85F * WildMonsterRenderer.alive(state)));
		renderer.glow(Wildercord.id("textures/entity/bog_witch_frog_glow.png"), state -> WildMonsterRenderer.white(
			(0.15F + 0.85F * state.windup + (state.engorged ? 0.35F : 0) + 0.1F * Mth.sin(state.ageInTicks * 0.3F)) * WildMonsterRenderer.alive(state)));
		return renderer;
	}
}
