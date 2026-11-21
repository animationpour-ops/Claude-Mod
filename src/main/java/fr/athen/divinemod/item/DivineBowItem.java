package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DivineBowItem extends Item {
    private static final String[] MODES = {"Décapitation Jumelle", "Pluie Céleste", "Constellation Meurtrière"};

    public DivineBowItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw)) return TypedActionResult.success(stack);

        if (user.isSneaking()) {
            int m = DivineUtil.cycleMode(stack, MODES.length);
            DivineUtil.actionBar(user, "✦ Pouvoir : " + MODES[m], Formatting.GOLD);
            DivineUtil.sound(sw, user, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
            return TypedActionResult.success(stack);
        }
        switch (DivineUtil.getMode(stack)) {
            case 0 -> twinDecapitation(sw, user);
            case 1 -> heavenRain(sw, user);
            default -> constellation(sw, user);
        }
        DivineUtil.sound(sw, user, SoundEvents.ENTITY_ARROW_SHOOT, 1f, 1.2f);
        user.getItemCooldownManager().set(this, 12);
        return TypedActionResult.success(stack);
    }

    /** Décapitation Jumelle : la cible ET le mob le plus proche de la même espèce perdent la tête. */
    private void twinDecapitation(ServerWorld w, PlayerEntity p) {
        EntityHitResult hit = DivineUtil.raycastEntity(p, 96, e -> DivineUtil.isValidTarget(e, p));
        if (hit == null) {
            DivineUtil.actionBar(p, "Aucune cible en vue.", Formatting.YELLOW);
            return;
        }
        Entity first = hit.getEntity();
        DivineUtil.particleLine(w, ParticleTypes.CRIT, p.getEyePos(), first.getEyePos(), 0.5);
        Entity twin = w.getEntitiesByClass(LivingEntity.class, first.getBoundingBox().expand(32),
                        e -> e != first && e.getType() == first.getType() && DivineUtil.isValidTarget(e, p))
                .stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(first))).orElse(null);
        decapitate(w, p, first);
        if (twin != null) {
            DivineUtil.particleLine(w, ParticleTypes.CRIT, first.getEyePos(), twin.getEyePos(), 0.5);
            decapitate(w, p, twin);
        }
    }

    private static void decapitate(ServerWorld w, PlayerEntity p, Entity e) {
        Item head = headOf(e);
        if (head != null) e.dropStack(new ItemStack(head));
        w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getEyeY(), e.getZ(), 12, 0.2, 0.2, 0.2, 0.2);
        DivineUtil.smite(w, p, e, 1000f);
    }

    private static Item headOf(Entity e) {
        if (e instanceof WitherSkeletonEntity) return Items.WITHER_SKELETON_SKULL;
        if (e instanceof SkeletonEntity) return Items.SKELETON_SKULL;
        if (e instanceof ZombifiedPiglinEntity) return null;
        if (e instanceof ZombieEntity) return Items.ZOMBIE_HEAD;
        if (e instanceof CreeperEntity) return Items.CREEPER_HEAD;
        if (e instanceof PiglinEntity) return Items.PIGLIN_HEAD;
        if (e instanceof EnderDragonEntity) return Items.DRAGON_HEAD;
        return null;
    }

    /** Pluie Céleste : une averse de flèches tombe du ciel là où tu vises. */
    private void heavenRain(ServerWorld w, PlayerEntity p) {
        Vec3d target;
        EntityHitResult eh = DivineUtil.raycastEntity(p, 96, e -> DivineUtil.isValidTarget(e, p));
        if (eh != null) {
            target = eh.getEntity().getPos();
        } else {
            BlockHitResult bh = DivineUtil.raycastBlocks(p, 96);
            target = bh.getType() == HitResult.Type.MISS ? p.getEyePos().add(p.getRotationVec(1f).multiply(30)) : bh.getPos();
        }
        for (int i = 0; i < 40; i++) {
            ArrowEntity arrow = new ArrowEntity(w, p, new ItemStack(Items.ARROW), null);
            arrow.setPosition(target.x + w.random.nextGaussian() * 3, target.y + 18 + w.random.nextDouble() * 6, target.z + w.random.nextGaussian() * 3);
            arrow.setVelocity(w.random.nextGaussian() * 0.05, -3.0, w.random.nextGaussian() * 0.05);
            arrow.setDamage(12.0);
            arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
            w.spawnEntity(arrow);
        }
        w.spawnParticles(ParticleTypes.END_ROD, target.x, target.y + 20, target.z, 60, 3, 1, 3, 0.02);
        DivineUtil.actionBar(p, "☔ Pluie Céleste !", Formatting.AQUA);
    }

    /** Constellation Meurtrière : une flèche qui ricoche sur TOUS les monstres autour. */
    private void constellation(ServerWorld w, PlayerEntity p) {
        List<LivingEntity> pool = new ArrayList<>(w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(48), DivineUtil::isEvil));
        if (pool.isEmpty()) {
            DivineUtil.actionBar(p, "Aucun monstre à portée.", Formatting.YELLOW);
            return;
        }
        Vec3d from = p.getEyePos();
        while (!pool.isEmpty()) {
            final Vec3d origin = from;
            LivingEntity next = pool.stream().min(Comparator.comparingDouble(e -> e.getEyePos().squaredDistanceTo(origin))).get();
            pool.remove(next);
            DivineUtil.particleLine(w, ParticleTypes.END_ROD, from, next.getEyePos(), 0.6);
            w.spawnParticles(ParticleTypes.FIREWORK, next.getX(), next.getEyeY(), next.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
            from = next.getEyePos();
            DivineUtil.smite(w, p, next, 1000f);
        }
        DivineUtil.sound(w, p, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.5f, 1f);
        DivineUtil.actionBar(p, "✦ Constellation Meurtrière !", Formatting.LIGHT_PURPLE);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Pouvoir actif : " + MODES[DivineUtil.getMode(stack) % MODES.length]).formatted(Formatting.GOLD));
        DivineUtil.lines(tooltip, "Pas besoin de flèches", "Clic droit : tirer", "Sneak + clic droit : changer de pouvoir");
    }
}
