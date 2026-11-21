package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineMining;
import fr.athen.divinemod.DivineUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DivinePickaxeItem extends PickaxeItem {
    public DivinePickaxeItem(Settings settings) {
        super(ToolMaterials.NETHERITE, settings.attributeModifiers(PickaxeItem.createAttributeModifiers(ToolMaterials.NETHERITE, 3.0F, -2.6F)));
    }

    @Override
    public float getMiningSpeed(ItemStack stack, net.minecraft.block.BlockState state) {
        return super.getMiningSpeed(stack, state) * 3.0F;
    }

    /** Lancer de pioche : creuse un tunnel 3x3 sur 24 blocs, tout arrive dans l'inventaire (minerais cuits). */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw)) return TypedActionResult.success(stack);

        Vec3d start = user.getEyePos();
        Vec3d dir = user.getRotationVec(1f);
        Set<BlockPos> done = new HashSet<>();
        List<ItemStack> loot = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            Vec3d point = start.add(dir.multiply(i));
            BlockPos c = BlockPos.ofFloored(point);
            for (BlockPos p : BlockPos.iterate(c.add(-1, -1, -1), c.add(1, 1, 1))) {
                BlockPos pos = p.toImmutable();
                if (done.add(pos)) DivineMining.mineBlock(sw, pos, user, stack, loot);
            }
            if (i % 2 == 0) sw.spawnParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 4, 0.3, 0.3, 0.3, 0.1);
        }
        DivineMining.give(user, loot);
        DivineUtil.sound(sw, user, SoundEvents.ENTITY_WITHER_SHOOT, 1f, 0.8f);
        user.getItemCooldownManager().set(this, 15);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DivineUtil.lines(tooltip, "Cuit les minerais directement (x2)", "Clic droit : lancer la pioche (tunnel 3x3)");
    }
}
