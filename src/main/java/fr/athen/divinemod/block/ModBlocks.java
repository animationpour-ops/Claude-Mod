package fr.athen.divinemod.block;

import fr.athen.divinemod.DivineMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Rarity;

public final class ModBlocks {
    public static final Block CELESTIAL_FORGE = Registry.register(Registries.BLOCK, DivineMod.id("celestial_forge"),
            new CelestialForgeBlock(AbstractBlock.Settings.create()
                    .strength(5.0F, 1200.0F)
                    .requiresTool()
                    .luminance(state -> 12)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Item CELESTIAL_FORGE_ITEM = Registry.register(Registries.ITEM, DivineMod.id("celestial_forge"),
            new BlockItem(CELESTIAL_FORGE, new Item.Settings().rarity(Rarity.EPIC)));

    private ModBlocks() {}

    public static void init() {}
}
