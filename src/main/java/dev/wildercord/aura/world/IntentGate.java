package dev.wildercord.aura.world;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.Vec3;

/** One bounded nine-panel gate opens to a blade carrying sufficient Aura. No block ticker. */
public final class IntentGate extends Block {
	public static final IntegerProperty STAGE=IntegerProperty.create("stage",2,3);
	public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
	public IntentGate(Properties p) { super(p);registerDefaultState(defaultBlockState().setValue(STAGE,2).setValue(FACING,Direction.NORTH)); }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(STAGE,FACING);}
	@Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
	@Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
	public static void use(ServerPlayer p,BlockPos at) {
		if(p.isSpectator() || p.distanceToSqr(Vec3.atCenterOf(at))>25)return;
		var level=p.level();var s=level.getBlockState(at);if(!s.is(SwordTombs.GATE))return;
		int stage=s.getValue(STAGE);
		if(!SwordTombRules.gate(Aura.stage(p),stage,Aura.enabled(p),Aura.holdsWeapon(p))) {
			p.sendOverlayMessage(Component.translatable("message.wildercord.tomb.gate",Component.translatable("aura.wildercord.stage."+AuraRules.id(stage))));return;
		}
		Direction side=s.getValue(FACING).getClockWise();
		for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++) {
			var pos=at.relative(side,dx).offset(0,dy,0);
			if(level.hasChunkAt(pos) && level.getBlockState(pos).equals(s))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
		}
		Feels.sound(level,Vec3.atCenterOf(at),"aura_tomb_gate",.8F,1);
	}
}
