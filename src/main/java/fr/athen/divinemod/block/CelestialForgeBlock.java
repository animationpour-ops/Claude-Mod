package fr.athen.divinemod.block;

import com.mojang.serialization.MapCodec;
import fr.athen.divinemod.DivineUtil;
import fr.athen.divinemod.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class CelestialForgeBlock extends Block {
    public static final MapCodec<CelestialForgeBlock> CODEC = createCodec(CelestialForgeBlock::new);

    public CelestialForgeBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    /** Clic droit avec un item compatible : il devient divin. */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        Item divine = ModItems.divineVersionOf(stack.getItem());
        if (divine == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (world instanceof ServerWorld sw) {
            ItemStack result = new ItemStack(divine);
            ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (ench != null && !ench.isEmpty()) result.set(DataComponentTypes.ENCHANTMENTS, ench);
            if (divine instanceof ArmorItem) {
                sw.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.PROTECTION)
                        .ifPresent(e -> result.addEnchantment(e, 10));
            }

            if (stack.getCount() > 1) {
                stack.decrement(1);
                player.getInventory().offerOrDrop(result);
            } else {
                player.setStackInHand(hand, result);
            }

            Vec3d c = Vec3d.ofCenter(pos).add(0, 0.6, 0);
            DivineUtil.lightning(sw, Vec3d.ofBottomCenter(pos.up()));
            sw.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y, c.z, 60, 0.4, 0.6, 0.4, 0.4);
            sw.spawnParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.2, 1.5, 0.2, 0.05);
            sw.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1f, 1.4f);
            sw.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1f, 0.8f);
            player.sendMessage(Text.literal("✦ ").append(result.getName()).append(" !").formatted(Formatting.GOLD), true);
        }
        return ItemActionResult.success(world.isClient);
    }

    /** Clic droit main vide : rappel de ce qui peut être divinisé. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.literal("Forge Céleste : pose un item en diamant (ou arc, briquet, pomme dorée, œil de l'Ender) pour le rendre divin.")
                    .formatted(Formatting.LIGHT_PURPLE), false);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 3; i++) {
            world.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.1 + random.nextDouble() * 0.5, pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.5, 0.3, (random.nextDouble() - 0.5) * 0.5);
        }
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.04, 0);
        }
    }
}
