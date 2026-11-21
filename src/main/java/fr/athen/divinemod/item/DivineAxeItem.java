package fr.athen.divinemod.item;

import fr.athen.divinemod.DivinePowers;
import fr.athen.divinemod.DivineUtil;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class DivineAxeItem extends AxeItem {
    public DivineAxeItem(Settings settings) {
        super(ToolMaterials.NETHERITE, settings.attributeModifiers(AxeItem.createAttributeModifiers(ToolMaterials.NETHERITE, 12.0F, -2.8F)));
    }

    // Pas d'écorçage : le clic droit sert au pouvoir, même en visant un bloc.
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return ActionResult.PASS;
    }

    /** Fracas Divin : on frappe le sol, tout vole et meurt, puis les items sont aspirés dans ton inventaire. */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw)) return TypedActionResult.success(stack);

        for (LivingEntity e : sw.getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(12, 6, 12), e -> DivineUtil.isValidTarget(e, user))) {
            Vec3d push = e.getPos().subtract(user.getPos()).normalize().multiply(1.5);
            e.addVelocity(push.x, 1.2, push.z);
            e.velocityModified = true;
            DivineUtil.smite(sw, user, e, 60f);
        }
        Vec3d c = user.getPos().add(0, 0.2, 0);
        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        for (int r = 2; r <= 12; r += 2) DivineUtil.particleRing(sw, ParticleTypes.CLOUD, c, r, r * 8);
        DivineUtil.sound(sw, user, SoundEvents.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
        DivineUtil.sound(sw, user, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.8f);

        // La "bulle" d'items : on aspire tout ce qui est tombé, une fois les mobs morts.
        DivinePowers.schedule(sw.getServer(), 25, () -> {
            if (!user.isAlive()) return;
            Box box = user.getBoundingBox().expand(16);
            for (ItemEntity ie : sw.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
                sw.spawnParticles(ParticleTypes.BUBBLE_POP, ie.getX(), ie.getY() + 0.3, ie.getZ(), 4, 0.1, 0.1, 0.1, 0.0);
                user.getInventory().offerOrDrop(ie.getStack().copy());
                ie.discard();
            }
            for (ExperienceOrbEntity orb : sw.getEntitiesByClass(ExperienceOrbEntity.class, box, ExperienceOrbEntity::isAlive)) {
                user.addExperience(orb.getExperienceAmount());
                orb.discard();
            }
            DivineUtil.sound(sw, user, SoundEvents.ENTITY_ITEM_PICKUP, 1f, 0.7f);
        });

        user.getItemCooldownManager().set(this, 40);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DivineUtil.lines(tooltip, "Abat l'arbre entier d'un coup", "Clic droit : Fracas Divin (onde de choc + aspire les items)");
    }
}
