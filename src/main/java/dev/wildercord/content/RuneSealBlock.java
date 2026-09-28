package dev.wildercord.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

/**
 * A Rune Seal: one block of an Archive door, carved with an element's glyph. Strike it with a
 * spell of that element and every block of that element in the door lights up. Light them all
 * (every element in the door) within ten seconds and the door dissolves. It can't be broken.
 */
public class RuneSealBlock extends Block {
	public enum Element implements StringRepresentable {
		FIRE, FROST, STORM, WIND, EARTH, LIFE, VOID, ARCANE, TIME, BLOOD;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		public static Element of(String element) {
			for (Element e : values()) {
				if (e.getSerializedName().equals(element)) {
					return e;
				}
			}
			return null;
		}
	}

	public static final EnumProperty<Element> ELEMENT = EnumProperty.create("element", Element.class);
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	/** How long a lit element stays lit while the rest are found. */
	public static final int LIT_TICKS = 200;

	public RuneSealBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ELEMENT, Element.ARCANE).setValue(LIT, false));
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// Not all lit in time: this one goes dark again.
		if (state.getValue(LIT)) {
			level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ELEMENT, LIT);
	}
}
