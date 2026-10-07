package net.minecraft.client.model.player;
import net.minecraft.client.model.geom.ModelPart;
public class PlayerModel extends net.minecraft.client.model.Model<Object> implements dev.wildercord.gametest.BraceNullPlayerWidth {
 public ModelPart body = new ModelPart(), head = new ModelPart(), rightArm = new ModelPart(),
  leftArm = new ModelPart(), rightLeg = new ModelPart(), leftLeg = new ModelPart(), root = new ModelPart();
 public boolean slim;
 public ModelPart root() { return root; }
 public boolean wildercord$braceNullSlim() { return slim; }
}
