package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineMod;
import fr.athen.divinemod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.Map;

public final class ModItems {
    public static final Item DIVINE_HELMET = reg("divine_helmet", new DivineArmorItem(ArmorItem.Type.HELMET, divine(),
            "Vision nocturne + respiration aquatique", "Regard Autoritaire [R] : vise un mob, il se soumet"));
    public static final Item DIVINE_CHESTPLATE = reg("divine_chestplate", new DivineArmorItem(ArmorItem.Type.CHESTPLATE, divine(),
            "Force II, Résistance II", "Immunité au feu, régénération"));
    public static final Item DIVINE_LEGGINGS = reg("divine_leggings", new DivineArmorItem(ArmorItem.Type.LEGGINGS, divine(),
            "Vitesse x2, monte les blocs tout seul", "Multi-Clonage [B] : 4 clones pendant 30 s"));
    public static final Item DIVINE_BOOTS = reg("divine_boots", new DivineArmorItem(ArmorItem.Type.FEET, divine(),
            "Nuage magique : double saut = voler", "Flèches haut/bas : vitesse du nuage", "Aucun dégât de chute"));

    public static final Item DIVINE_SWORD = reg("divine_sword", new DivineSwordItem(divine()));
    public static final Item DIVINE_PICKAXE = reg("divine_pickaxe", new DivinePickaxeItem(divine()));
    public static final Item DIVINE_AXE = reg("divine_axe", new DivineAxeItem(divine()));
    public static final Item DIVINE_BOW = reg("divine_bow", new DivineBowItem(divine()));
    public static final Item DIVINE_LIGHTER = reg("divine_lighter", new DivineLighterItem(divine()));
    public static final Item DIVINE_EYE = reg("divine_eye", new DivineEyeItem(divine()));
    public static final Item DIVINE_APPLE = reg("divine_apple", new DivineAppleItem(new Item.Settings()
            .maxCount(16).rarity(Rarity.EPIC)
            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            .food(new FoodComponent.Builder().nutrition(10).saturationModifier(1.2F).alwaysEdible()
                    .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1), 1.0F)
                    .statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3), 1.0F)
                    .build())));

    /** Item normal -> sa version divine (ce que la Forge Céleste accepte). */
    private static final Map<Item, Item> DIVINE_OF = new HashMap<>();

    private ModItems() {}

    private static Item.Settings divine() {
        return new Item.Settings()
                .maxCount(1)
                .rarity(Rarity.EPIC)
                .fireproof()
                .component(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false))
                .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    private static Item reg(String name, Item item) {
        return Registry.register(Registries.ITEM, DivineMod.id(name), item);
    }

    public static Item divineVersionOf(Item item) {
        return DIVINE_OF.get(item);
    }

    public static void init() {
        DIVINE_OF.put(Items.DIAMOND_HELMET, DIVINE_HELMET);
        DIVINE_OF.put(Items.DIAMOND_CHESTPLATE, DIVINE_CHESTPLATE);
        DIVINE_OF.put(Items.DIAMOND_LEGGINGS, DIVINE_LEGGINGS);
        DIVINE_OF.put(Items.DIAMOND_BOOTS, DIVINE_BOOTS);
        DIVINE_OF.put(Items.DIAMOND_SWORD, DIVINE_SWORD);
        DIVINE_OF.put(Items.DIAMOND_PICKAXE, DIVINE_PICKAXE);
        DIVINE_OF.put(Items.DIAMOND_AXE, DIVINE_AXE);
        DIVINE_OF.put(Items.NETHERITE_HELMET, DIVINE_HELMET);
        DIVINE_OF.put(Items.NETHERITE_CHESTPLATE, DIVINE_CHESTPLATE);
        DIVINE_OF.put(Items.NETHERITE_LEGGINGS, DIVINE_LEGGINGS);
        DIVINE_OF.put(Items.NETHERITE_BOOTS, DIVINE_BOOTS);
        DIVINE_OF.put(Items.NETHERITE_SWORD, DIVINE_SWORD);
        DIVINE_OF.put(Items.NETHERITE_PICKAXE, DIVINE_PICKAXE);
        DIVINE_OF.put(Items.NETHERITE_AXE, DIVINE_AXE);
        DIVINE_OF.put(Items.BOW, DIVINE_BOW);
        DIVINE_OF.put(Items.FLINT_AND_STEEL, DIVINE_LIGHTER);
        DIVINE_OF.put(Items.ENDER_EYE, DIVINE_EYE);
        DIVINE_OF.put(Items.GOLDEN_APPLE, DIVINE_APPLE);
        DIVINE_OF.put(Items.ENCHANTED_GOLDEN_APPLE, DIVINE_APPLE);

        Registry.register(Registries.ITEM_GROUP, DivineMod.id("divine_items"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModBlocks.CELESTIAL_FORGE_ITEM))
                .displayName(Text.translatable("itemGroup.divinemod"))
                .entries((context, entries) -> {
                    entries.add(ModBlocks.CELESTIAL_FORGE_ITEM);
                    entries.add(DIVINE_HELMET);
                    entries.add(DIVINE_CHESTPLATE);
                    entries.add(DIVINE_LEGGINGS);
                    entries.add(DIVINE_BOOTS);
                    entries.add(DIVINE_SWORD);
                    entries.add(DIVINE_PICKAXE);
                    entries.add(DIVINE_AXE);
                    entries.add(DIVINE_BOW);
                    entries.add(DIVINE_LIGHTER);
                    entries.add(DIVINE_EYE);
                    entries.add(DIVINE_APPLE);
                })
                .build());
    }
}
