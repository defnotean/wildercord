package dev.wildercord.content;

import dev.wildercord.Wildercord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Spell constructs have real collision, original textures, and no obtainable item or loot. */
public final class PhysicalBlocks {
	private PhysicalBlocks(){}
	public static final Block STRATA=block("raised_strata",false,false,0);
	public static final Block CINDER=block("cinder_bulwark",false,false,7);
	public static final Block ROOT=block("root_bulwark",false,false,2);
	public static final Block WATER=block("lifted_water",false,true,3);
	public static final Block RESERVATION=block("water_reservation",false,true,0);
	public static final Block WIND=block("wind_step",true,false,4);
	public static final Block RIME=block("rime_step",true,false,3);
	public static final Block THUNDER=block("thunder_step",true,false,6);
	private static Block block(String name,boolean step,boolean ghost,int light){
		var key=ResourceKey.create(Registries.BLOCK,Wildercord.id(name));
		var p=BlockBehaviour.Properties.of().setId(key).strength(.6F,2).noLootTable().noOcclusion().lightLevel(s->light)
			.sound(step?SoundType.WOOL:SoundType.STONE);
		if(ghost)p.noCollision().strength(-1,3600000);
		return Registry.register(BuiltInRegistries.BLOCK,key,step?new Step(p):new Block(p));
	}
	private static final class Step extends Block {
		Step(Properties p){super(p);}
		@Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(0,0,0,16,3,16);}
	}
	public static boolean isConstruct(BlockState state){return state.is(STRATA)||state.is(CINDER)||state.is(ROOT)||state.is(WATER)||state.is(RESERVATION)||isStep(state);}
	public static boolean isStep(BlockState state){return state.is(WIND)||state.is(RIME)||state.is(THUNDER);}
	public static void init(){}
}
