package dev.wildercord.client.ocean;

import dev.wildercord.ocean.OceanRules;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-side render states carrying synced animation & state flags for Deep Ocean entities. */
public final class OceanRenderStates {
	private OceanRenderStates() {}

	public static class BarnacleHulkState extends LivingEntityRenderState {
		public int slamTicks;
	}

	public static class SirenJellyState extends LivingEntityRenderState {
		public int pulseTicks;
	}

	public static class CoralMakoState extends LivingEntityRenderState {
		public int lungeTicks;
	}

	public static class DreadNautilusState extends LivingEntityRenderState {
		public int laserTicks;
	}

	public static class ThalassorState extends LivingEntityRenderState {
		public float jawOpen;
		public int attackMode;
	}

	public static class BrineMerchantState extends LivingEntityRenderState {
		public OceanRules.MerchantProfession profession = OceanRules.MerchantProfession.PEARL_JEWELER;
	}

	public static class LumiSeahorseState extends LivingEntityRenderState {
		public boolean saddled;
		public int dashTicks;
	}

	public static class PearlPupState extends LivingEntityRenderState {
		public boolean sniffing;
	}

	public static class MoonWhaleState extends LivingEntityRenderState {
		public boolean singing;
	}
}
