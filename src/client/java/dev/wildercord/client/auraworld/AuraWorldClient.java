package dev.wildercord.client.auraworld;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.Duelist;
import dev.wildercord.aura.world.FallenKnight;
import dev.wildercord.aura.world.ForgedGear;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The world of aura on the client: the duelist's and the fallen knight's models and renderers, and the lines an aura-forged
 * weapon's tooltip gains.
 */
public final class AuraWorldClient {
	private AuraWorldClient() {}

	public static final ModelLayerLocation DUELIST = new ModelLayerLocation(Wildercord.id("duelist"), "main");
	public static final ModelLayerLocation FALLEN_KNIGHT = new ModelLayerLocation(Wildercord.id("fallen_knight"), "main");

	/** Each duelist's skin, by its method: its cloak's trim, its sash and its scabbard's tassel in its element's colours. */
	private static final Map<String, Identifier> DUELIST_SKINS = new HashMap<>();

	public static void init() {
		ModelLayerRegistry.registerModelLayer(DUELIST, DuelistModel::createLayer);
		ModelLayerRegistry.registerModelLayer(FALLEN_KNIGHT, FallenKnightModel::createLayer);
		EntityRendererRegistry.register(AuraWorld.DUELIST, AuraWorldClient::duelist);
		EntityRendererRegistry.register(AuraWorld.FALLEN_KNIGHT, AuraWorldClient::knight);
		// An aura-forged weapon says what it carries, under its name.
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			List<Component> extra = ForgedGear.describe(stack);
			if (!extra.isEmpty()) {
				lines.addAll(Math.min(1, lines.size()), extra);
			}
		});
	}

	private static Identifier duelistSkin(String method) {
		return DUELIST_SKINS.computeIfAbsent(method, m -> Wildercord.id("textures/entity/duelist/" + m + ".png"));
	}

	/** Its sash's emblem glows softly, more with its blade drawn, most in its tell. */
	private static AuraFighterRenderer<Duelist, DuelistModel> duelist(EntityRendererProvider.Context context) {
		return new AuraFighterRenderer<>(context, new DuelistModel(context.bakeLayer(DUELIST)), state -> duelistSkin(state.method),
			Wildercord.id("textures/entity/duelist/glow.png"),
			state -> AuraFighterRenderer.tint((0.45F + 0.3F * state.drawn + 0.25F * state.windup + 0.05F * Mth.sin(state.ageInTicks * 0.1F))
				* AuraFighterRenderer.alive(state), state.color), 1.0F);
	}

	/** Its visor and the cracks in its plate: a dim, smoky glow that flares as it raises its blade, and gutters when it reels. */
	private static AuraFighterRenderer<FallenKnight, FallenKnightModel> knight(EntityRendererProvider.Context context) {
		return new AuraFighterRenderer<>(context, new FallenKnightModel(context.bakeLayer(FALLEN_KNIGHT)),
			state -> Wildercord.id("textures/entity/fallen_knight.png"), Wildercord.id("textures/entity/fallen_knight_glow.png"),
			state -> {
				float flicker = Mth.sin(state.ageInTicks * 0.23F) * 0.06F + Mth.sin(state.ageInTicks * 0.71F) * 0.04F;
				return AuraFighterRenderer.tint((0.55F + 0.45F * state.windup - 0.35F * state.stagger + flicker) * AuraFighterRenderer.alive(state), state.color);
			}, 0.7F);
	}
}
