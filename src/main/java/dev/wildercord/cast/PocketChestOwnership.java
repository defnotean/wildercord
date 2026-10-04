package dev.wildercord.cast;
import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
/** Explicit persisted placement provenance. Build permission alone never proves a chest belongs to a player. */
public final class PocketChestOwnership {
 private PocketChestOwnership(){}
 public static final AttachmentType<String> OWNER=AttachmentRegistry.create(Wildercord.id("pocket_chest_owner"),b -> b.persistent(Codec.STRING));
 public static void placed(ServerPlayer p,ServerLevel level,BlockPos at){
  if(level.isLoaded(at) && level.getBlockEntity(at) instanceof ChestBlockEntity chest){chest.setAttached(OWNER,p.getUUID().toString());chest.setChanged();}
 }
 static boolean owns(ServerPlayer p,ChestBlockEntity chest){return p.getUUID().toString().equals(chest.getAttached(OWNER));}
}
