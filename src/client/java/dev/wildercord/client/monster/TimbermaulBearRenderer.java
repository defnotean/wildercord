package dev.wildercord.client.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.monster.TimbermaulBear;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Timbermaul Bear, drawn half again an ordinary bear's size, its moves' poses eased in and out. */
public class TimbermaulBearRenderer extends WildMonsterRenderer<TimbermaulBear, TimbermaulBearModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("timbermaul_bear"), "main");

	public TimbermaulBearRenderer(EntityRendererProvider.Context context) {
		super(context, new TimbermaulBearModel(context.bakeLayer(LAYER)), 1.2F, Wildercord.id("textures/entity/timbermaul_bear.png"));
	}

	@Override
	public void extractRenderState(TimbermaulBear bear, MonsterRenderState state, float partial) {
		super.extractRenderState(bear, state, partial);
		state.move = bear.move();
		state.since = bear.since(partial);
		state.tell = bear.tell();
		state.roused = bear.roused();
		state.side = bear.side();
		for (int i = 0; i < state.moves.length; i++) {
			state.moves[i] = bear.blend(i, partial);
		}
	}

	@Override
	protected void scale(MonsterRenderState state, PoseStack poseStack) {
		poseStack.scale(1.5F, 1.5F, 1.5F);
	}
}
