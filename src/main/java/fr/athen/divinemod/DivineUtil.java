package fr.athen.divinemod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class DivineUtil {
    public static final String ALLY_TAG = "divinemod_ally";
    public static final String CLONE_TAG = "divinemod_clone";
    public static final String OWNER_PREFIX = "divinemod_owner_";
    public static final String PHOENIX_TAG = "divinemod_phoenix";

    private DivineUtil() {}

    // ---------- Alliés (mobs soumis + clones) ----------
    public static String ownerTag(UUID owner) {
        return OWNER_PREFIX + owner;
    }

    public static boolean isAlly(Entity e) {
        return e != null && e.getCommandTags().contains(ALLY_TAG);
    }

    public static UUID getOwner(Entity e) {
        if (e == null) return null;
        for (String tag : e.getCommandTags()) {
            if (tag.startsWith(OWNER_PREFIX)) {
                try {
                    return UUID.fromString(tag.substring(OWNER_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    /** "Âme négative" : un monstre hostile qui n'est pas un allié. */
    public static boolean isEvil(Entity e) {
        return e instanceof Monster && e instanceof LivingEntity le && le.isAlive() && !isAlly(e);
    }

    /** Cible valable pour les gros pouvoirs de zone (on épargne joueurs, villageois, animaux apprivoisés, alliés). */
    public static boolean isValidTarget(Entity e, PlayerEntity player) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == player) return false;
        if (e instanceof PlayerEntity || e instanceof ArmorStandEntity || e instanceof MerchantEntity) return false;
        if (e instanceof TameableEntity t && t.isTamed()) return false;
        return !isAlly(e);
    }

    // ---------- Raycasts ----------
    public static BlockHitResult raycastBlocks(PlayerEntity p, double range) {
        Vec3d start = p.getEyePos();
        Vec3d end = start.add(p.getRotationVec(1.0F).multiply(range));
        return p.getWorld().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
    }

    public static EntityHitResult raycastEntity(PlayerEntity p, double range, Predicate<Entity> filter) {
        Vec3d start = p.getEyePos();
        Vec3d end = start.add(p.getRotationVec(1.0F).multiply(range));
        BlockHitResult blocks = raycastBlocks(p, range);
        if (blocks.getType() != HitResult.Type.MISS) end = blocks.getPos();
        Box box = p.getBoundingBox().stretch(end.subtract(start)).expand(1.0);
        return ProjectileUtil.raycast(p, start, end, box,
                e -> !e.isSpectator() && e.canHit() && filter.test(e), start.squaredDistanceTo(end));
    }

    // ---------- Effets ----------
    public static void smite(ServerWorld w, PlayerEntity p, Entity e, float dmg) {
        if (e instanceof LivingEntity le) le.damage(w.getDamageSources().playerAttack(p), dmg);
    }

    public static void particleLine(ServerWorld w, ParticleEffect fx, Vec3d a, Vec3d b, double step) {
        Vec3d d = b.subtract(a);
        double len = d.length();
        if (len < 0.01) return;
        Vec3d n = d.normalize();
        for (double i = 0; i < len; i += step) {
            Vec3d v = a.add(n.multiply(i));
            w.spawnParticles(fx, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    public static void particleRing(ServerWorld w, ParticleEffect fx, Vec3d c, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            w.spawnParticles(fx, c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    public static void lightning(ServerWorld w, Vec3d pos) {
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
        if (bolt != null) {
            bolt.setCosmetic(true);
            bolt.refreshPositionAfterTeleport(pos);
            w.spawnEntity(bolt);
        }
    }

    public static void sound(ServerWorld w, Entity at, SoundEvent s, float volume, float pitch) {
        w.playSound(null, at.getX(), at.getY(), at.getZ(), s, SoundCategory.PLAYERS, volume, pitch);
    }

    public static void actionBar(PlayerEntity p, String msg, Formatting color) {
        p.sendMessage(Text.literal(msg).formatted(color), true);
    }

    // ---------- Modes d'item ----------
    public static int getMode(ItemStack stack) {
        return stack.getOrDefault(ModComponents.MODE, 0);
    }

    public static int cycleMode(ItemStack stack, int count) {
        int m = (getMode(stack) + 1) % count;
        stack.set(ModComponents.MODE, m);
        return m;
    }

    public static void lines(List<Text> tooltip, String... lines) {
        for (String l : lines) tooltip.add(Text.literal(l).formatted(Formatting.GRAY));
    }
}
