package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.SakuraVillager;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Renders {@link SakuraVillager} as either a Tea Master or a Shrine Maiden. */
public class SakuraVillagerRenderer extends MobRenderer<SakuraVillager, SakuraVillagerRenderState, SakuraVillagerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("sakura_villager"), "main");

	private static final Identifier TEA = Wildercord.id("textures/entity/sakura_villager_tea.png");
	private static final Identifier SHRINE = Wildercord.id("textures/entity/sakura_villager_shrine.png");

	public SakuraVillagerRenderer(EntityRendererProvider.Context context) {
		super(context, new SakuraVillagerModel(context.bakeLayer(LAYER)), 0.45F);
	}

	@Override
	public Identifier getTextureLocation(SakuraVillagerRenderState state) {
		return state.role == SakuraVillager.SHRINE_MAIDEN ? SHRINE : TEA;
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
