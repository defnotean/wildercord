package dev.wildercord.content;

import dev.wildercord.cast.Residues;
import dev.wildercord.world.ResidueRules;
import dev.wildercord.world.ResidueRules.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One element's residue: what it looks like close up ({@link ResidueAmbience}), and what it does to what's on
 * it or near it. Ash keeps whatever stands on it warm; everfrost is slick as ice; storm-glass gives a jolt of
 * speed to whatever brushes it; a lingering eddy lifts; a wildbloom feeds bees and seeds a little; a void scar
 * draws loose items in, closes as it ages and keeps animals off; a star glyph shows the invisible; stilled
 * sand slows what walks it and keeps what's dropped there from ageing; bloodmoss feeds nether wart.
 */
public class ResidueBlock extends Block {
	/** A void scar narrowing as it closes, 0 (fresh) to 3 (almost shut). */
	public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

	/** How often (in ticks) a void scar draws items in. */
	private static final int PULL_EVERY = 10;
	private static final double PULL_RADIUS = 5.0;

	public final Kind kind;
	private final VoxelShape shape;

	public ResidueBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		this.shape = switch (kind) {
			case SMOULDERING_ASH -> Block.box(0, 0, 0, 16, 2, 16);
			case FULGURITE -> Block.box(3, 0, 3, 13, 12, 13);
			case LINGERING_EDDY -> Block.box(2, 0, 2, 14, 14, 14);
			case WILDBLOOM -> Block.box(4, 0, 4, 12, 12, 12);
			case STAR_GLYPH, BLOODMOSS -> Block.box(0, 0, 0, 16, 1, 16);
			case STILLED_SAND -> Block.box(1, 0, 1, 15, 3, 15);
			default -> Shapes.block();
		};
		if (kind == Kind.VOID_SCAR) {
			registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		// The kind isn't known yet while the state definition is built (the constructor's super call): every residue
		// block carries a stage, and only the void scar's ever changes from 0.
		builder.add(STAGE);
	}

	private boolean rests() {
		return kind.placement == ResidueRules.Placement.REST;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return kind == Kind.WILDBLOOM ? shape.move(state.getOffset(pos)) : shape;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return rests() ? Shapes.empty() : Shapes.block();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return rests();
	}

	// ------------------------------------------------------------------ lying on the ground

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (!rests() || kind == Kind.LINGERING_EDDY) {
			return true;
		}
		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
			BlockState neighbourState, RandomSource random) {
		// Whatever it lay on is gone: it goes too (as a flower would, giving what it gives).
		return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
			: super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
	}

	// ------------------------------------------------------------------ close up

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		ResidueAmbience.animate(kind, state, level, pos, random);
	}

	// ------------------------------------------------------------------ what it does

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
		switch (kind) {
			case SMOULDERING_ASH -> {
				// Still warm: nothing standing in the ash freezes.
				if (entity.getTicksFrozen() > 0) {
					entity.setTicksFrozen(0);
				}
			}
			case FULGURITE -> {
				if (!level.isClientSide() && entity instanceof LivingEntity living && !living.hasEffect(MobEffects.SPEED)) {
					living.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, true));
					level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.6F);
					ResidueAmbience.jolt(level, pos);
				}
			}
			case LINGERING_EDDY -> {
				// A gentle updraft (on both sides, as a bubble column does, so a player's own movement agrees).
				Vec3 v = entity.getDeltaMovement();
				entity.setDeltaMovement(v.x, Math.max(v.y, 0.32), v.z);
				entity.resetFallDistance();
			}
			case STAR_GLYPH -> {
				if (level instanceof ServerLevel server && entity instanceof Player && server.getGameTime() % 20 == 0) {
					// Starlight shows what hides: anything invisible near the glyph glows for a moment.
					for (LivingEntity hidden : server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(8), LivingEntity::isInvisible)) {
						hidden.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
					}
				}
			}
			case STILLED_SAND -> {
				if (level.isClientSide()) {
					return;
				}
				// Time runs thick here: what walks it slows, and what's dropped on it stops ageing.
				if (entity instanceof ItemEntity item) {
					item.setUnlimitedLifetime();
				} else if (entity instanceof LivingEntity living && !living.hasEffect(MobEffects.SLOWNESS)) {
					living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0, false, true));
				}
			}
			default -> {
			}
		}
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (kind == Kind.RIVEN_STONE && level.isClientSide() && entity.getRandom().nextInt(12) == 0) {
			ResidueAmbience.dust(level, pos);
		}
		super.stepOn(level, pos, state, entity);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (kind != Kind.LINGERING_EDDY || !stack.is(Items.GLASS_BOTTLE)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		// Bottle the gale: the eddy goes into the bottle.
		if (level instanceof ServerLevel server) {
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Reagents.item(Kind.LINGERING_EDDY))));
			server.playSound(null, pos, SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.BLOCKS, 0.8F, 1.3F);
			Residues.harvested(server, pos, state, player instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null);
			server.removeBlock(pos, false);
		}
		return InteractionResult.SUCCESS;
	}

	// ------------------------------------------------------------------ ageing

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return kind == Kind.WILDBLOOM || kind == Kind.BLOODMOSS;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (kind == Kind.WILDBLOOM) {
			Residues.seed(level, pos, random);
		} else if (kind == Kind.BLOODMOSS) {
			// Blood feeds nether wart: one growing beside the moss grows a stage.
			BlockPos near = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
			BlockState wart = level.getBlockState(near);
			if (wart.is(Blocks.NETHER_WART) && wart.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) < 3) {
				level.setBlock(near, wart.setValue(net.minecraft.world.level.block.NetherWartBlock.AGE,
					wart.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) + 1), Block.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		// However it went (broken, blown up, washed away), its record follows at the end of the tick.
		Residues.gone(level, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (kind == Kind.VOID_SCAR && !oldState.is(this)) {
			level.scheduleTick(pos, this, PULL_EVERY);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (kind != Kind.VOID_SCAR) {
			return;
		}
		level.scheduleTick(pos, this, PULL_EVERY);
		// It closes as it ages.
		int stage = Residues.stage(level, pos);
		if (stage != state.getValue(STAGE)) {
			level.setBlock(pos, state.setValue(STAGE, stage), Block.UPDATE_CLIENTS);
		}
		Vec3 heart = Vec3.atCenterOf(pos).add(0, 0.5, 0);
		// The less open it is, the weaker its pull.
		double pull = 0.06 * (1 - stage * 0.2);
		for (Entity loose : level.getEntities((Entity) null, new AABB(pos).inflate(PULL_RADIUS), e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
			Vec3 toward = heart.subtract(loose.position());
			double distance = toward.length();
			if (distance > 0.4 && distance <= PULL_RADIUS) {
				loose.setDeltaMovement(loose.getDeltaMovement().add(toward.normalize().scale(pull)));
				loose.needsSync = true;
			}
		}
		// Animals shy away from it.
		for (Animal animal : level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(3.5))) {
			Vec3 away = animal.position().subtract(heart).multiply(1, 0, 1);
			if (away.lengthSqr() > 1.0E-4 && !animal.isPassenger()) {
				Vec3 flee = animal.position().add(away.normalize().scale(6));
				animal.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.3);
				animal.setDeltaMovement(animal.getDeltaMovement().add(away.normalize().scale(0.08)));
			}
		}
	}
}
