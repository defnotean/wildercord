package dev.wildercord.cast;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;

/** A finite, permanent shallow pool. Validate the entire vessel before placing any water. */
public final class Basinfill {
    private Basinfill() {}
    public static final int MAX_CELLS = 16, MAX_REACH = 3;

    public static boolean fill(Cast cast, Cast.Hit hit) {
        var level = cast.level;
        if (hit.block() == null || hit.face() == null || hit.self()) return refuse(cast);
        if (level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, hit.point()) || !Casters.mayBuild(cast.caster)) return refuse(cast);
        BlockPos seed = hit.block().relative(hit.face());
        var cells = new LinkedHashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(seed);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (cells.contains(pos)) continue;
            if (Math.abs(pos.getX()-seed.getX()) > MAX_REACH || Math.abs(pos.getZ()-seed.getZ()) > MAX_REACH
                || !safe(cast,pos) || !fillable(cast,pos) || cells.size() >= MAX_CELLS) return refuse(cast);
            BlockPos floor = pos.below();
            if (!safe(cast,floor) || !level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP)
                || !Casters.mayEdit(cast.caster,level,pos) || !Casters.mayEdit(cast.caster,level,floor)) return refuse(cast);
            cells.add(pos);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(d);
                if (!safe(cast,next)) return refuse(cast);
                if (fillable(cast,next)) queue.addLast(next);
                else if (!level.getBlockState(next).isFaceSturdy(level,next,d.getOpposite())
                    || TemporaryBlocks.recorded(level,next)) return refuse(cast);
            }
        }
        var edits = cells.stream().filter(p -> !level.getBlockState(p).is(Blocks.WATER)
            || !level.getFluidState(p).isSource()).toList();
        if (edits.isEmpty() || !cast.once("basinfill")) return false;
        if (!cast.takeBlocks(edits.size())) return refuse(cast);
        // No ticks or callbacks intervene between validation and placement on the server thread.
        for (BlockPos pos : edits) {
            level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
            level.sendParticles(ParticleTypes.SPLASH,pos.getX()+.5,pos.getY()+.9,pos.getZ()+.5,5,.24,.03,.24,.02);
            level.sendParticles(ParticleTypes.FALLING_WATER,pos.getX()+.5,pos.getY()+1.3,pos.getZ()+.5,3,.2,.06,.2,0);
        }
        level.playSound(null,seed,SoundEvents.BUCKET_EMPTY,SoundSource.PLAYERS,.65F,1.15F);
        return true;
    }
    private static boolean safe(Cast cast,BlockPos pos) {
        return cast.level.isLoaded(pos) && !cast.level.isOutsideBuildHeight(pos)
            && cast.level.getWorldBorder().isWithinBounds(pos) && !TemporaryBlocks.recorded(cast.level,pos);
    }
    private static boolean fillable(Cast cast,BlockPos pos) {
        var state = cast.level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.WATER);
    }
    private static boolean refuse(Cast cast) {
        Casters.tell(cast.caster,Component.translatable("spell.wildercord.basinfill.refusal"));
        return false;
    }
}
