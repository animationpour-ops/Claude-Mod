package fr.athen.divinemod.item;

import fr.athen.divinemod.DivineUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DivineSwordItem extends SwordItem {
    private static final String[] MODES = {"Lame Céleste", "Jugement Dernier", "Exécution Divine"};
    private static final Map<UUID, UUID> MARKS = new HashMap<>();

    public DivineSwordItem(Settings settings) {
        super(ToolMaterials.NETHERITE, settings.attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 12, -2.0F)));
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
            case 0 -> heavenlyEdge(sw, user);
            case 1 -> lastJudgment(sw, user);
            default -> divineExecution(sw, user);
        }
        user.getItemCooldownManager().set(this, 10);
        return TypedActionResult.success(stack);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity p && DivineUtil.getMode(stack) == 1 && !p.getWorld().isClient) mark(p, target);
        return super.postHit(stack, target, attacker);
    }

    /** Lame Céleste : un rayon de lumière qui pulvérise tout sur 48 blocs. */
    private void heavenlyEdge(ServerWorld w, PlayerEntity p) {
        Vec3d start = p.getEyePos();
        Vec3d end = start.add(p.getRotationVec(1f).multiply(48));
        Box area = p.getBoundingBox().stretch(end.subtract(start)).expand(2);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, area, e -> DivineUtil.isValidTarget(e, p))) {
            if (e.getBoundingBox().expand(0.8).raycast(start, end).isPresent()) {
                w.spawnParticles(ParticleTypes.FLASH, e.getX(), e.getBodyY(0.5), e.getZ(), 1, 0, 0, 0, 0);
                DivineUtil.smite(w, p, e, 1000f);
            }
        }
        DivineUtil.particleLine(w, ParticleTypes.END_ROD, start.add(0, -0.3, 0), end, 0.5);
        DivineUtil.particleLine(w, ParticleTypes.SWEEP_ATTACK, start.add(0, -0.3, 0), end, 2.0);
        DivineUtil.sound(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
        DivineUtil.sound(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 2f);
    }

    /** Jugement Dernier : on marque une cible (clic droit ou coup), puis clic droit = foudre divine. */
    private void lastJudgment(ServerWorld w, PlayerEntity p) {
        EntityHitResult hit = DivineUtil.raycastEntity(p, 64, e -> DivineUtil.isValidTarget(e, p));
        UUID markedId = MARKS.get(p.getUuid());
        Entity marked = markedId == null ? null : w.getEntity(markedId);

        if (hit != null && hit.getEntity() != marked) {
            mark(p, hit.getEntity());
            return;
        }
        if (marked instanceof LivingEntity le && le.isAlive()) {
            Vec3d pos = le.getPos();
            for (int i = 0; i < 3; i++) DivineUtil.lightning(w, pos.add(w.random.nextGaussian() * 0.5, 0, w.random.nextGaussian() * 0.5));
            w.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 1, pos.z, 60, 0.4, 1.2, 0.4, 0.08);
            w.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 80, 0.2, 3.0, 0.2, 0.15);
            DivineUtil.smite(w, p, le, 2000f);
            MARKS.remove(p.getUuid());
            DivineUtil.actionBar(p, "⚡ Jugement Dernier !", Formatting.GOLD);
        } else {
            MARKS.remove(p.getUuid());
            DivineUtil.actionBar(p, "Vise un mob pour le marquer d'abord.", Formatting.YELLOW);
        }
    }

    private static void mark(PlayerEntity p, Entity target) {
        if (!(target instanceof LivingEntity le) || !(p.getWorld() instanceof ServerWorld w)) return;
        MARKS.put(p.getUuid(), target.getUuid());
        le.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 30, 0, false, false));
        w.spawnParticles(ParticleTypes.ENCHANTED_HIT, le.getX(), le.getBodyY(0.5), le.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
        p.sendMessage(Text.literal("✦ Cible marquée : ").append(le.getDisplayName()).formatted(Formatting.YELLOW), true);
    }

    /** Exécution Divine : coup à 360° qui fauche tout dans un rayon de 10 blocs. */
    private void divineExecution(ServerWorld w, PlayerEntity p) {
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(10), e -> DivineUtil.isValidTarget(e, p))) {
            DivineUtil.smite(w, p, e, 1000f);
        }
        Vec3d c = p.getPos().add(0, 1, 0);
        for (int r = 2; r <= 10; r += 2) DivineUtil.particleRing(w, ParticleTypes.SWEEP_ATTACK, c, r, r * 6);
        DivineUtil.particleRing(w, ParticleTypes.END_ROD, c, 10, 90);
        DivineUtil.sound(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.5f);
        DivineUtil.sound(w, p, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Pouvoir actif : " + MODES[DivineUtil.getMode(stack) % MODES.length]).formatted(Formatting.GOLD));
        DivineUtil.lines(tooltip, "Clic droit : utiliser le pouvoir", "Sneak + clic droit : changer de pouvoir");
    }
}
