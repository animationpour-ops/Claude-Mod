package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineUtil;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class DivineArmorItem extends ArmorItem {
    private final String[] lines;

    public DivineArmorItem(Type type, Settings settings, String... lines) {
        super(ArmorMaterials.DIAMOND, type, settings);
        this.lines = lines;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DivineUtil.lines(tooltip, lines);
    }
}
