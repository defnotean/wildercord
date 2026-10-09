package net.minecraft.client;
public class Minecraft {
	public static Minecraft current = new Minecraft();
	public static Minecraft getInstance() { return current; }
	public Player player = new Player(); public Level level = new Level(); public Options options = new Options();
	public static class Player {
		public int tickCount = 100; public net.minecraft.world.entity.HumanoidArm hand = net.minecraft.world.entity.HumanoidArm.RIGHT;
		public int getId() { return 7; }
		public java.util.UUID getUUID() { return new java.util.UUID(0, 7); }
		public net.minecraft.world.entity.HumanoidArm getMainArm() { return hand; }
	}
	public static class Level { public long tick = 106; public long getGameTime() { return tick; } }
	public static class Options { public Camera camera = new Camera(); public Camera getCameraType() { return camera; } }
	public static class Camera { public boolean first; public boolean isFirstPerson() { return first; } }
}
