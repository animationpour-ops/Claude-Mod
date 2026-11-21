package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class DivineAppleItem extends Item {
    public DivineAppleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof PlayerEntity p) {
            p.addCommandTag(DivineUtil.PHOENIX_TAG);
            p.sendMessage(Text.literal("✹ Bénédiction du Phénix : tu renaîtras de tes cendres.").formatted(Formatting.GOLD), false);
        }
        return super.finishUsing(stack, world, user);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DivineUtil.lines(tooltip, "Bénédiction du Phénix :", "à ta prochaine mort, tu renais", "avec deux barres de cœurs");
    }
}
