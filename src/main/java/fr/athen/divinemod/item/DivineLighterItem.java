package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class DivineLighterItem extends Item {
    public DivineLighterItem(Settings settings) {
        super(settings);
    }

    /** Flamme Purificatrice : brûle l'âme de tous les monstres dans un rayon de 64 blocs (même dans les grottes). */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw)) return TypedActionResult.success(stack);

        List<LivingEntity> evil = sw.getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(64), DivineUtil::isEvil);
        for (LivingEntity e : evil) {
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getBodyY(0.5), e.getZ(), 25, 0.3, 0.6, 0.3, 0.02);
            sw.spawnParticles(ParticleTypes.SOUL, e.getX(), e.getEyeY(), e.getZ(), 10, 0.2, 0.8, 0.2, 0.05);
            DivineUtil.smite(sw, user, e, 1000f);
        }
        sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, user.getX(), user.getY() + 1, user.getZ(), 120, 2, 1, 2, 0.15);
        DivineUtil.sound(sw, user, SoundEvents.ITEM_FLINTANDSTEEL_USE, 1f, 0.6f);
        DivineUtil.sound(sw, user, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 0.5f);
        DivineUtil.actionBar(user, "✹ " + evil.size() + " âme(s) négative(s) purifiée(s)", Formatting.AQUA);
        user.getItemCooldownManager().set(this, 60);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DivineUtil.lines(tooltip, "Clic droit : Flamme Purificatrice", "Tue tous les monstres dans 64 blocs");
    }
}
