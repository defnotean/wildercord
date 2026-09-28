package dev.wildercord.cast.events;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What a Fallen Star holds and remembers, saved with it: the rune inside, when its beacon goes out
 * and when it fades, whether its guards have come, and every block its crater changed, so the
 * crater can be put back exactly as it was when the star goes (even after a restart).
 */
public class FallenStarBlockEntity extends BlockEntity {
	/** One block the crater changed, and what it was before. */
	public record Changed(BlockPos pos, BlockState was) {
		public static final Codec<Changed> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Changed::pos),
			BlockState.CODEC.fieldOf("was").forGetter(Changed::was)
		).apply(i, Changed::new));
	}

	String rune = "";
	long beaconUntil;
	long fadeAt;
	boolean guarded;
	final List<Changed> crater = new ArrayList<>();

	public FallenStarBlockEntity(BlockPos pos, BlockState state) {
		super(EventContent.FALLEN_STAR_ENTITY, pos, state);
	}

	/** The rune inside (a rune id). */
	public String rune() {
		return rune;
	}

	public boolean guarded() {
		return guarded;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		rune = input.getStringOr("rune", "");
		beaconUntil = input.getLongOr("beacon_until", 0L);
		fadeAt = input.getLongOr("fade_at", 0L);
		guarded = input.getBooleanOr("guarded", false);
		crater.clear();
		input.listOrEmpty("crater", Changed.CODEC).stream().forEach(crater::add);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("rune", rune);
		output.putLong("beacon_until", beaconUntil);
		output.putLong("fade_at", fadeAt);
		output.putBoolean("guarded", guarded);
		ValueOutput.TypedOutputList<Changed> list = output.list("crater", Changed.CODEC);
		crater.forEach(list::add);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FallenStarBlockEntity star) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long now = server.getGameTime();
		Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 0.75, 0);
		if (now % 5 == 0) {
			// Starlight sparkles off it.
			server.sendParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 1, 0.3, 0.25, 0.3, 0.02);
		}
		if ((now + pos.asLong()) % 20 != 0) {
			return;
		}
		if (star.fadeAt > 0 && now >= star.fadeAt) {
			FallenStars.crumble(server, pos, star, false);
			return;
		}
		FallenStars.tickStar(server, pos, star, now);
	}
}
