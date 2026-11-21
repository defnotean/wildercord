package dev.wildercord.client.auraworld;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraBeast;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class BeastRenderer<T extends AuraBeast> extends MobRenderer<T,BeastRenderState,EntityModel<BeastRenderState>> {
	private final ResourceLocation skin;
	public BeastRenderer(EntityRendererProvider.Context c,EntityModel<BeastRenderState> model,String id,float shadow) { super(c,model,shadow);skin=Wildercord.id("textures/entity/"+id+".png"); }
	@Override public BeastRenderState createRenderState() { return new BeastRenderState(); }
	@Override public ResourceLocation getTextureLocation(BeastRenderState s) { return skin; }
	@Override public void extractRenderState(T e,BeastRenderState s,float partial) { super.extractRenderState(e, s, partial);s.pose=e.pose();s.elapsed=e.elapsed(partial); }
}
