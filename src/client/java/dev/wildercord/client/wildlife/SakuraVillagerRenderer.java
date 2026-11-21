package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.SakuraVillager;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders {@link SakuraVillager} across all six custom Sakura Village NPC professions. */
public class SakuraVillagerRenderer extends MobRenderer<SakuraVillager, SakuraVillagerRenderState, SakuraVillagerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("sakura_villager"), "main");

	private static final ResourceLocation[] TEXTURES = {
		Wildercord.id("textures/entity/sakura_villager_tea.png"),
		Wildercord.id("textures/entity/sakura_villager_shrine.png"),
		Wildercord.id("textures/entity/sakura_villager_smith.png"),
		Wildercord.id("textures/entity/sakura_villager_lantern.png"),
		Wildercord.id("textures/entity/sakura_villager_onsen.png"),
		Wildercord.id("textures/entity/sakura_villager_fisher.png")
	};

	public SakuraVillagerRenderer(EntityRendererProvider.Context context) {
		super(context, new SakuraVillagerModel(context.bakeLayer(LAYER)), 0.45F);
	}

	@Override
	public ResourceLocation getTextureLocation(SakuraVillagerRenderState state) {
		return TEXTURES[Math.floorMod(state.role, TEXTURES.length)];
	}

	@Override
	public SakuraVillagerRenderState createRenderState() {
		return new SakuraVillagerRenderState();
	}

	@Override
	public void extractRenderState(SakuraVillager villager, SakuraVillagerRenderState state, float partial) {
		super.extractRenderState(villager, state, partial);
		state.role = villager.role();
		state.bowTick = villager.bowTick();
	}
}
