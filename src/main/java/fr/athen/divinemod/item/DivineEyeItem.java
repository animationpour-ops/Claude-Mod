package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.List;

public class DivineEyeItem extends Item {
    public DivineEyeItem(Settings settings) {
        super(settings);
    }

    /** Œil Divin : téléporte directement au-dessus du stronghold le plus proche. */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw)) return TypedActionResult.success(stack);
        if (sw.getRegistryKey() != World.OVERWORLD) {
            DivineUtil.actionBar(user, "L'Œil Divin ne fonctionne que dans l'Overworld.", Formatting.RED);
            return TypedActionResult.fail(stack);
        }
        BlockPos sh = sw.locateStructure(StructureTags.EYE_OF_ENDER_LOCATED, user.getBlockPos(), 100, false);
        if (sh == null) {
            DivineUtil.actionBar(user, "Aucun stronghold trouvé...", Formatting.RED);
            return TypedActionResult.fail(stack);
        }
        sw.spawnParticles(ParticleTypes.PORTAL, user.getX(), user.getY() + 1, user.getZ(), 80, 0.5, 1, 0.5, 0.5);
        int y = sw.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sh.getX(), sh.getZ());
        user.requestTeleport(sh.getX() + 0.5, y + 1, sh.getZ() + 0.5);
        DivineUtil.sound(sw, user, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        user.sendMessage(Text.literal("◉ Le stronghold est sous tes pieds (vers Y=" + sh.getY() + ")").formatted(Formatting.LIGHT_PURPLE), false);
        user.getItemCooldownManager().set(this, 100);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DivineUtil.lines(tooltip, "Clic droit : téléportation au stronghold le plus proche", "N'est pas consommé");
    }
}
