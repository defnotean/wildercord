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
		var p=BlockBehaviour.Properties.of()