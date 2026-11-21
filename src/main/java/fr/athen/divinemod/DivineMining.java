package fr.athen.divinemod;

import fr.athen.divinemod.item.ModItems;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DivineMining {
    private DivineMining() {}

    /** Pioche Divine : les minerais sont cuits directement, x2, et vont dans l'inventaire. */
    public static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be) {
        if (!(world instanceof ServerWorld sw)) return true;
        ItemStack tool = player.getMainHandStack();
        if (tool.isOf(ModItems.DIVINE_PICKAXE) && state.isIn(ConventionalBlockTags.ORES)) {
            List<ItemStack> loot = new ArrayList<>();
            if (mineBlock(sw, pos, player, tool, loot)) {
                give(player, loot);
                sw.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
                return false;
            }
        }
        return true;
    }

    /** Hache Divine : tout l'arbre explose d'un coup. */
    public static void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be) {
        if (!(world instanceof ServerWorld sw)) return;
        ItemStack tool = player.getMainHandStack();
        if (tool.isOf(ModItems.DIVINE_AXE) && state.isIn(BlockTags.LOGS)) fellTree(sw, pos, player, tool);
    }

    public static void fellTree(ServerWorld w, BlockPos origin, PlayerEntity p, ItemStack tool) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        seen.add(origin);
        List<ItemStack> loot = new ArrayList<>();
        int count = 0;
        while (!queue.isEmpty() && count < 700) {
            BlockPos c = queue.poll();
            for (BlockPos n : BlockPos.iterate(c.add(-1, -1, -1), c.add(1, 1, 1))) {
                BlockPos im = n.toImmutable();
                if (!seen.add(im)) continue;
                BlockState s = w.getBlockState(im);
                if ((s.isIn(BlockTags.LOGS) || s.isIn(BlockTags.LEAVES)) && mineBlock(w, im, p, tool, loot)) {
                    count++;
                    queue.add(im);
                }
            }
        }
        give(p, loot);
    }

    /** Casse un bloc "proprement" et met ses drops dans out. Les minerais sont cuits et doublés. */
    public static boolean mineBlock(ServerWorld w, BlockPos pos, PlayerEntity p, ItemStack tool, List<ItemStack> out) {
        BlockState st = w.getBlockState(pos);
        if (st.isAir() || st.getHardness(w, pos) < 0 || st.getBlock() instanceof FluidBlock || w.getBlockEntity(pos) != null) {
            return false;
        }
        boolean ore = st.isIn(ConventionalBlockTags.ORES);
        for (ItemStack drop : Block.getDroppedStacks(st, w, pos, null, p, tool)) {
            if (ore) {
                ItemStack s = smelt(w, drop);
                s.setCount(Math.min(s.getMaxCount(), s.getCount() * 2));
                out.add(s);
            } else {
                out.add(drop);
            }
        }
        if (ore) st.onStacksDropped(w, pos, tool, true);
        w.syncWorldEvent(2001, pos, Block.getRawIdFromState(st));
        w.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        return true;
    }

    public static ItemStack smelt(ServerWorld w, ItemStack in) {
        Optional<RecipeEntry<SmeltingRecipe>> r = w.getRecipeManager()
                .getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(in), w);
        if (r.isEmpty()) return in;
        ItemStack res = r.get().value().getResult(w.getRegistryManager()).copy();
        if (res.isEmpty()) return in;
        res.setCount(Math.min(res.getMaxCount(), res.getCount() * in.getCount()));
        return res;
    }

    public static void give(PlayerEntity p, List<ItemStack> loot) {
        for (ItemStack s : loot) if (!s.isEmpty()) p.getInventory().offerOrDrop(s);
    }
}
